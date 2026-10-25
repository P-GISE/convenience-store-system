package com.example.ordersystem2.service;

import com.example.ordersystem2.model.User;
import com.example.ordersystem2.model.UserRole;
import com.example.ordersystem2.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class AccessControlService {
    private final UserRepository users;

    public AccessControlService(UserRepository users) {
        this.users = users;
    }

    public User currentUser(HttpSession session) {
        if (session == null || !(session.getAttribute("userId") instanceof Long id)) {
            return null;
        }
        return users.findById(id).orElse(null);
    }

    public boolean isLoggedIn(HttpSession session) {
        return currentUser(session) != null;
    }

    public boolean hasRole(HttpSession session, UserRole role) {
        User user = currentUser(session);
        return user != null && user.getRole() == role;
    }

    public boolean isHeadquarters(HttpSession session) {
        return hasRole(session, UserRole.HEADQUARTERS);
    }

    public boolean isStoreOwner(HttpSession session) {
        return hasRole(session, UserRole.STORE_OWNER);
    }

    public String redirectFor(HttpSession session) {
        User user = currentUser(session);
        return user == null ? "redirect:/login"
                : user.getRole() == UserRole.CUSTOMER ? "redirect:/shop" : "redirect:/home2";
    }
}
