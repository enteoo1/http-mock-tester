package com.example.mocktester.web;

import com.example.mocktester.model.AppUser;
import com.example.mocktester.model.MockContext;
import com.example.mocktester.repo.ContextRepository;
import com.example.mocktester.repo.ScenarioRepository;
import com.example.mocktester.service.TimeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
public class ContextController {

    private final CurrentUser current;
    private final ContextRepository contexts;
    private final ScenarioRepository scenarios;

    public ContextController(CurrentUser current, ContextRepository contexts, ScenarioRepository scenarios) {
        this.current = current;
        this.contexts = contexts;
        this.scenarios = scenarios;
    }

    @GetMapping("/contexts")
    public String list(Principal principal, Model model) {
        AppUser u = current.get(principal);
        model.addAttribute("contexts", contexts.findByUser(u.getId()));
        return "contexts";
    }

    @PostMapping("/contexts")
    public String create(Principal principal, @RequestParam String name,
                         @RequestParam(defaultValue = "") String description, Model model) {
        AppUser u = current.get(principal);
        String error = null;
        if (!AuthController.SLUG.matcher(name).matches()) {
            error = "Context name may only contain letters, digits, '-' and '_' (max 64 characters).";
        } else if (contexts.findByUserAndName(u.getId(), name).isPresent()) {
            error = "You already have a context with that name.";
        }
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("contexts", contexts.findByUser(u.getId()));
            return "contexts";
        }
        long id = contexts.insert(u.getId(), name, description.trim(), TimeUtil.nowDb());
        return "redirect:/contexts/" + id;
    }

    @GetMapping("/contexts/{id}")
    public String detail(@PathVariable long id, Principal principal, Model model, HttpServletRequest request) {
        AppUser u = current.get(principal);
        MockContext c = contexts.findByIdAndUser(id, u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("context", c);
        model.addAttribute("scenarios", scenarios.findByContext(c.getId()));
        String host = request.getHeader("Host");
        if (host == null) {
            host = request.getServerName() + ":" + request.getServerPort();
        }
        model.addAttribute("baseUrl", request.getScheme() + "://" + host + "/mock/" + u.getUsername()
                + "/" + c.getName());
        return "context";
    }

    @PostMapping("/contexts/{id}/delete")
    public String delete(@PathVariable long id, Principal principal) {
        AppUser u = current.get(principal);
        contexts.delete(id, u.getId());
        return "redirect:/contexts";
    }
}
