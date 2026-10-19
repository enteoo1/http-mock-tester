package com.example.mocktester.web;

import com.example.mocktester.model.AppUser;
import com.example.mocktester.model.MockContext;
import com.example.mocktester.model.Scenario;
import com.example.mocktester.repo.ContextRepository;
import com.example.mocktester.repo.ScenarioRepository;
import com.example.mocktester.service.TimeUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@Controller
public class ScenarioController {

    private static final List<String> ALL_METHODS =
            List.of("ANY", "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");
    private static final List<String> BODY_MODES = List.of("OPTIONAL", "REQUIRED", "FORBIDDEN");
    private static final List<String> AUTH_TYPES = List.of("NONE", "BASIC", "BEARER", "API_KEY", "OAUTH2");

    private final CurrentUser current;
    private final ContextRepository contexts;
    private final ScenarioRepository scenarios;

    public ScenarioController(CurrentUser current, ContextRepository contexts, ScenarioRepository scenarios) {
        this.current = current;
        this.contexts = contexts;
        this.scenarios = scenarios;
    }

    private MockContext ownedContext(long id, AppUser u) {
        return contexts.findByIdAndUser(id, u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Scenario ownedScenario(long id, AppUser u) {
        return scenarios.findByIdAndUser(id, u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String render(Model model, MockContext c, ScenarioForm form, String action, String title,
                          String error) {
        model.addAttribute("context", c);
        model.addAttribute("form", form);
        model.addAttribute("action", action);
        model.addAttribute("title", title);
        model.addAttribute("allMethods", ALL_METHODS);
        model.addAttribute("bodyModes", BODY_MODES);
        model.addAttribute("authTypes", AUTH_TYPES);
        model.addAttribute("error", error);
        return "scenario-form";
    }

    @GetMapping("/contexts/{cid}/scenarios/new")
    public String newForm(@PathVariable long cid, Principal principal, Model model) {
        MockContext c = ownedContext(cid, current.get(principal));
        return render(model, c, new ScenarioForm(), "/contexts/" + cid + "/scenarios", "New scenario", null);
    }

    @PostMapping("/contexts/{cid}/scenarios")
    public String create(@PathVariable long cid, @ModelAttribute("form") ScenarioForm form,
                         Principal principal, Model model) {
        MockContext c = ownedContext(cid, current.get(principal));
        String error = validate(form);
        if (error == null && scenarios.findByContextAndName(c.getId(), form.getName().trim()).isPresent()) {
            error = "A scenario with that name already exists in this context.";
        }
        if (error != null) {
            return render(model, c, form, "/contexts/" + cid + "/scenarios", "New scenario", error);
        }
        Scenario s = new Scenario();
        form.applyTo(s);
        s.setContextId(c.getId());
        s.setCreatedAt(TimeUtil.nowDb());
        scenarios.insert(s);
        return "redirect:/contexts/" + cid;
    }

    @GetMapping("/scenarios/{id}/edit")
    public String editForm(@PathVariable long id, Principal principal, Model model) {
        AppUser u = current.get(principal);
        Scenario s = ownedScenario(id, u);
        MockContext c = ownedContext(s.getContextId(), u);
        return render(model, c, ScenarioForm.from(s), "/scenarios/" + id, "Edit scenario " + s.getName(), null);
    }

    @PostMapping("/scenarios/{id}")
    public String update(@PathVariable long id, @ModelAttribute("form") ScenarioForm form,
                         Principal principal, Model model) {
        AppUser u = current.get(principal);
        Scenario s = ownedScenario(id, u);
        MockContext c = ownedContext(s.getContextId(), u);
        String error = validate(form);
        if (error == null) {
            var clash = scenarios.findByContextAndName(c.getId(), form.getName().trim());
            if (clash.isPresent() && !clash.get().getId().equals(s.getId())) {
                error = "A scenario with that name already exists in this context.";
            }
        }
        if (error != null) {
            return render(model, c, form, "/scenarios/" + id, "Edit scenario " + s.getName(), error);
        }
        form.applyTo(s);
        scenarios.update(s);
        return "redirect:/contexts/" + c.getId();
    }

    @PostMapping("/scenarios/{id}/toggle")
    public String toggle(@PathVariable long id, Principal principal) {
        Scenario s = ownedScenario(id, current.get(principal));
        scenarios.setEnabled(s.getId(), !s.isEnabled());
        return "redirect:/contexts/" + s.getContextId();
    }

    @PostMapping("/scenarios/{id}/delete")
    public String delete(@PathVariable long id, Principal principal) {
        Scenario s = ownedScenario(id, current.get(principal));
        scenarios.delete(s.getId());
        return "redirect:/contexts/" + s.getContextId();
    }

    private String validate(ScenarioForm f) {
        if (f.getName() == null || !AuthController.SLUG.matcher(f.getName().trim()).matches()) {
            return "Scenario name may only contain letters, digits, '-' and '_' (max 64 characters).";
        }
        if (f.getMethods() == null || f.getMethods().isEmpty()) {
            return "Select at least one HTTP method.";
        }
        for (String m : f.getMethods()) {
            if (!ALL_METHODS.contains(m)) {
                return "Unknown HTTP method: " + m;
            }
        }
        if (!BODY_MODES.contains(f.getBodyMode())) {
            return "Invalid request body option.";
        }
        if (!AUTH_TYPES.contains(f.getAuthType())) {
            return "Invalid authentication type.";
        }
        if ("API_KEY".equals(f.getAuthType())
                && (f.getApiKeyHeader() == null || f.getApiKeyHeader().isBlank())) {
            return "Enter the header name used for the API key.";
        }
        if (f.getResponseStatus() == null || f.getResponseStatus() < 100 || f.getResponseStatus() > 599) {
            return "Response status must be between 100 and 599.";
        }
        if (f.getDelayMs() == null || f.getDelayMs() < 0 || f.getDelayMs() > 60000) {
            return "Delay must be between 0 and 60000 ms.";
        }
        return null;
    }
}
