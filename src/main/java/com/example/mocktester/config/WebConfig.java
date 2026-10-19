package com.example.mocktester.config;

import com.example.mocktester.model.AppUser;
import com.example.mocktester.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Optional;

/** Forces users flagged "must change password" (the default admin) to the account page. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UserRepository users;

    public WebConfig(UserRepository users) {
        this.users = users;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws Exception {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                    Optional<AppUser> u = users.findByUsername(auth.getName());
                    if (u.isPresent() && u.get().isMustChangePassword()) {
                        response.sendRedirect(request.getContextPath() + "/account?forced");
                        return false;
                    }
                }
                return true;
            }
        }).addPathPatterns("/**")
          .excludePathPatterns("/mock/**", "/login", "/logout", "/css/**", "/error", "/account/**", "/account");
    }
}
