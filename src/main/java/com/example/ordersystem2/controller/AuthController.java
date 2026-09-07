// src/main/java/com/example/ordersystem2/controller/AuthController.java
package com.example.ordersystem2.controller;

import com.example.ordersystem2.dto.UserLoginRequestDTO;
import com.example.ordersystem2.dto.UserRegistrationRequestDTO;
import com.example.ordersystem2.model.User;
import com.example.ordersystem2.model.UserRole;
import com.example.ordersystem2.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    // 루트 URL을 공용 홈 페이지로 매핑
    @GetMapping("/")
    public String showPublicHomePage(Model model) {
        return "home"; // 로그인 전 사용자에게 보여지는 홈 페이지 템플릿
    }

    // 회원가입 페이지 표시
    @GetMapping("/signup")
    public String showSignupPage(Model model) {
        model.addAttribute("userRegistrationRequest", new UserRegistrationRequestDTO());
        return "signup";
    }

    // 회원가입 처리
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("userRegistrationRequest") UserRegistrationRequestDTO userRegistrationRequestDTO,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "signup";
        }

        if (!userRegistrationRequestDTO.getPassword().equals(userRegistrationRequestDTO.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.userRegistrationRequest", "비밀번호가 일치하지 않습니다.");
            return "signup";
        }

        // 사용자 중복 확인
        if (userService.findByUsername(userRegistrationRequestDTO.getUsername()) != null) {
            bindingResult.rejectValue("username", "error.userRegistrationRequest", "사용자명이 이미 사용 중입니다.");
            return "signup";
        }

        // 사용자 등록 (비밀번호 암호화 포함)
        userService.registerUser(
                userRegistrationRequestDTO.getUsername(),
                userRegistrationRequestDTO.getPassword(),
                UserRole.STORE_OWNER
        );

        logger.debug("User {} registered successfully.", userRegistrationRequestDTO.getUsername());

        // 로그인 페이지로 리다이렉트
        return "redirect:/login?signup_success";
    }

    // 로그인 페이지 표시
    @GetMapping("/login")
    public String showLoginPage(Model model) {
        model.addAttribute("userLoginRequest", new UserLoginRequestDTO());
        return "login";
    }

    // 로그인 처리
    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("userLoginRequest") UserLoginRequestDTO userLoginRequestDTO,
                        BindingResult bindingResult,
                        HttpServletRequest request,
                        Model model) {
        if (bindingResult.hasErrors()) {
            return "login";
        }

        // 사용자 인증 로직
        User user = userService.authenticate(userLoginRequestDTO.getUsername(), userLoginRequestDTO.getPassword());
        if (user != null) {
            // 세션 고정 공격 방어: 기존 세션 무효화 후 새로운 세션 생성
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession newSession = request.getSession(true); // 새로운 세션 생성
            newSession.setAttribute("userId", user.getId()); // 사용자 ID 저장
            newSession.setAttribute("username", user.getUsername()); // 사용자명 저장 (선택 사항)
            newSession.setAttribute("role", user.getRole().name());

            logger.debug("User {} logged in. Session ID: {}", user.getUsername(), newSession.getId());

            return "redirect:/home2"; // 로그인 후 대시보드로 리다이렉트
        } else {
            bindingResult.reject("invalidCredentials", "사용자명 또는 비밀번호가 잘못되었습니다.");
            return "login";
        }
    }

    // 로그아웃 처리
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        logger.debug("User logged out. Session ID: {}", session.getId());
        session.invalidate();
        return "redirect:/login?logout_success";
    }
}
