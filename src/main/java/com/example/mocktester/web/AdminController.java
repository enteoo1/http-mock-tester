package com.example.mocktester.web;

import com.example.mocktester.repo.UserRepository;
import com.example.mocktester.service.RetentionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminController {

    private final UserRepository users;
    private final RetentionService retention;

    public AdminController(UserRepository users, RetentionService retention) {
        this.users = users;
        this.retention = retention;
    }

    private String page(Model model) {
        model.addAttribute("users", users.findAll());
        model.addAttribute("retentionDays", retention.getRetentionDays());
        return "admin";
    }

    @GetMapping("/admin")
    public String show(Model model) {
        return page(model);
    }

    @PostMapping("/admin/retention")
    public String saveRetention(@RequestParam int days, Model model) {
        if (days < 1 || days > 3650) {
            model.addAttribute("error", "Retention must be between 1 and 3650 days.");
            return page(model);
        }
        retention.setRetentionDays(days);
        return "redirect:/admin?saved";
    }

    @PostMapping("/admin/purge")
    public String purgeNow() {
        retention.purge();
        return "redirect:/admin?purged";
    }
}
