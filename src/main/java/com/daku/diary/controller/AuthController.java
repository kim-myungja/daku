package com.daku.diary.controller;

import com.daku.diary.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller // 이 클래스가 Controller라고 선언
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // 로그인 페이지 보여주기
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // templates/login.html 보여줌
    }

    // 회원가입 페이지 보여주기
    @GetMapping("/register")
    public String registerPage() {
        return "register"; // templates/register.html 보여줌
    }

    // 회원가입 처리
    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String nickname,
                           @RequestParam String diaryTitle,
                           Model model) {
        try {
            userService.register(username, password, nickname, diaryTitle); // Service한테 전달
            return "redirect:/login"; // 가입 후 로그인 페이지로 이동
        } catch (IllegalArgumentException e) {
            // 중복 등 가입 실패 시 에러 메시지와 입력값을 다시 화면으로 전달 (비밀번호 제외)
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("nickname", nickname);
            model.addAttribute("diaryTitle", diaryTitle);
            return "register";
        }
    }
}