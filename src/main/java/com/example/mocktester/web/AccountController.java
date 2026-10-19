package com.example.mocktester.web;

import com.example.mocktester.model.AppUser;
import com.example.mocktester.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class AccountController {

    private final CurrentUser current;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AccountController(CurrentUser current, UserRepository users, PasswordEncoder encoder) {
        this.current = current;
        this.users = users;
        this.encoder = encoder;
    }

    private String page(Model model, AppUser u) {
        List<String> zones = new ArrayList<>(ZoneId.getAvailableZoneIds());
        Collections.sort(zones);
        model.addAttribute("zones", zones);
        model.addAttribute("user", u);
        return "account";
    }

    @GetMapping("/account")
    public String show(Principal principal, Model model) {
        AppUser u = current.get(principal);
        return page(model, u);
    }

    @PostMapping("/account/timezone")
    public String timezone(Principal principal, @RequestParam String timezone, Model model) {
        AppUser u = current.get(principal);
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            model.addAttribute("error", "Unknown timezone.");
            return page(model, u);
        }
        users.updateTimezone(u.getId(), timezone);
        return "redirect:/account?saved";
    }

    @PostMapping("/account/password")
    public String password(Principal principal, @RequestParam String currentPassword,
                           @RequestParam String newPassword, @RequestParam String confirm, Model model) {
        AppUser u = current.get(principal);
        String error = null;
        if (!encoder.matches(currentPassword, u.getPasswordHash())) {
            error = "Current password is incorrect.";
        } else if (newPassword.length() < 8) {
            error = "New password must have at least 8 characters.";
        } else if (!newPassword.equals(confirm)) {
            error = "New passwords do not match.";
        } else if (newPassword.equals(currentPassword)) {
            error = "New password must be different from the current one.";
        }
        if (error != null) {
            model.addAttribute("error", error);
            return page(model, u);
        }
        users.updatePassword(u.getId(), encoder.encode(newPassword));
        return "redirect:/contexts";
    }
}
