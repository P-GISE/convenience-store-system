package com.example.ordersystem2.service;

import com.example.ordersystem2.model.UserRole;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class AccessControlService {

    public boolean isLoggedIn(HttpSession session) {
        return session != null && session.getAttribute("userId") != null;
    }

    public boolean hasRole(HttpSession session, UserRole role) {
        if (!isLoggedIn(session)) {
            return false;
        }
        return readRole(session) == role;
    }

    public boolean isHeadquarters(HttpSession session) {
        return hasRole(session, UserRole.HEADQUARTERS);
    }

    public boolean isStoreOwner(HttpSession session) {
        return hasRole(session, UserRole.STORE_OWNER);
    }

    public String redirectFor(HttpSession session) {
        return isLoggedIn(session) ? "redirect:/home2" : "redirect:/login";
    }

    private UserRole readRole(HttpSession session) {
        Object role = session.getAttribute("role");
        if (role instanceof UserRole userRole) {
            return userRole;
        }
        if (role instanceof String roleName) {
            try {
                return UserRole.valueOf(roleName);
            } catch (IllegalArgumentException ignored) {
                return UserRole.STORE_OWNER;
            }
        }
        return UserRole.STORE_OWNER;
    }
}
