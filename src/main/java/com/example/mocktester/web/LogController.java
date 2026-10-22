package com.example.mocktester.web;

import com.example.mocktester.model.AppUser;
import com.example.mocktester.model.RequestLog;
import com.example.mocktester.repo.LogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.List;

@Controller
public class LogController {

    private static final int PAGE_SIZE = 50;
    private static final List<String> OUTCOMES = List.of("MATCHED", "UNAUTHORIZED", "BAD_REQUEST",
            "METHOD_NOT_ALLOWED", "NOT_FOUND", "DISABLED");

    private final CurrentUser current;
    private final LogRepository logs;

    public LogController(CurrentUser current, LogRepository logs) {
        this.current = current;
        this.logs = logs;
    }

    @GetMapping("/logs")
    public String list(Principal principal, Model model,
                       @RequestParam(defaultValue = "") String context,
                       @RequestParam(defaultValue = "") String scenario,
                       @RequestParam(defaultValue = "") String outcome,
                       @RequestParam(defaultValue = "0") int page) {
        AppUser u = current.get(principal);
        int p = Math.max(page, 0);
        List<RequestLog> rows = logs.search(u.getId(), context, scenario, outcome, PAGE_SIZE + 1, p * PAGE_SIZE);
        boolean hasNext = rows.size() > PAGE_SIZE;
        model.addAttribute("rows", hasNext ? rows.subList(0, PAGE_SIZE) : rows);
        model.addAttribute("hasNext", hasNext);
        model.addAttribute("page", p);
        model.addAttribute("context", context);
        model.addAttribute("scenario", scenario);
        model.addAttribute("outcome", outcome);
        model.addAttribute("outcomes", OUTCOMES);
        return "logs";
    }

    @GetMapping("/logs/{id}")
    public String detail(@PathVariable long id, Principal principal, Model model) {
        AppUser u = current.get(principal);
        RequestLog l = logs.findByIdAndUser(id, u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("log", l);
        return "log-detail";
    }

    @PostMapping("/logs/{id}/delete")
    public String delete(@PathVariable long id, Principal principal,
                         @RequestParam(defaultValue = "") String context,
                         @RequestParam(defaultValue = "") String scenario,
                         @RequestParam(defaultValue = "") String outcome,
                         @RequestParam(defaultValue = "0") int page) {
        logs.deleteByIdAndUser(id, current.get(principal).getId());
        return backToList(context, scenario, outcome, page);
    }

    @PostMapping("/logs/delete-all")
    public String deleteAll(Principal principal) {
        logs.deleteAllByUser(current.get(principal).getId());
        return "redirect:/logs";
    }

    private static String backToList(String context, String scenario, String outcome, int page) {
        return "redirect:" + UriComponentsBuilder.fromPath("/logs")
                .queryParam("context", context)
                .queryParam("scenario", scenario)
                .queryParam("outcome", outcome)
                .queryParam("page", Math.max(page, 0))
                .build().encode().toUriString();
    }
}
