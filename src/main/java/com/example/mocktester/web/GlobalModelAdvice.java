package com.example.mocktester.web;

import com.example.mocktester.model.AppUser;
import com.example.mocktester.repo.UserRepository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.Optional;

/** Adds the logged in user's name, role and timezone to every UI page. */
@ControllerAdvice(basePackages = "com.example.mocktester.web")
public class GlobalModelAdvice {

    private final UserRepository users;

    public GlobalModelAdvice(UserRepository users) {
        this.users = users;
    }

    @ModelAttribute
    public void common(Principal principal, Model model) {
        String tz = "UTC";
        String me = null;
        boolean admin = false;
        if (principal != null) {
            Optional<AppUser> u = users.findByUsername(principal.getName());
            if (u.isPresent()) {
                tz = u.get().getTimezone();
                me = u.get().getUsername();
                admin = "ADMIN".equals(u.get().getRole());
            }
        }
        model.addAttribute("tz", tz);
        model.addAttribute("me", me);
        model.addAttribute("isAdmin", admin);
    }
}
