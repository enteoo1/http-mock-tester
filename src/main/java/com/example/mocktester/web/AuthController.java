package com.example.mocktester.web;

import com.example.mocktester.repo.UserRepository;
import com.example.mocktester.service.TimeUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.regex.Pattern;

@Controller
public class AuthController {

    static final Pattern SLUG = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/contexts";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password,
                           @RequestParam String confirm, Model model) {
        String error = null;
        if (!USERNAME.matcher(username).matches()) {
            error = "Username must be 3-32 characters: letters, digits, '-' or '_' (it is part of your mock URLs).";
        } else if (password.length() < 8) {
            error = "Password must have at least 8 characters.";
        } else if (!password.equals(confirm)) {
            error = "Passwords do not match.";
        } else if (users.findByUsername(username).isPresent()) {
            error = "That username is already taken.";
        }
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("username", username);
            return "register";
        }
        users.insert(username, encoder.encode(password), "USER", "UTC", false, TimeUtil.nowDb());
        return "redirect:/login?registered";
    }
}
