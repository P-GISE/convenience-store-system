package com.example.ordersystem2.controller;

import com.example.ordersystem2.model.User;
import com.example.ordersystem2.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    @Autowired
    private UserService userService;

    @GetMapping("/home2")
    public String showHomePage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        User user = userService.findById(userId);
        if (user == null) {
            // 사용자 정보가 없을 경우 처리
            return "redirect:/login";
        }

        model.addAttribute("username", user.getUsername());
        return "home2"; // home2.html 템플릿
    }
}
