package com.daku.diary.controller;

import com.daku.diary.annotation.CurrentUser;
import com.daku.diary.entity.User;
import com.daku.diary.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 프로필 수정 페이지
    @GetMapping("/profile")
    public String profileForm(@CurrentUser User user, Model model) {
        model.addAttribute("user", user);
        return "profile";
    }

    // 프로필 저장
    @PostMapping("/profile")
    public String updateProfile(@CurrentUser User user,
                                @RequestParam String nickname,
                                @RequestParam(required = false) String diaryTitle,
                                @RequestParam(required = false) String bio,
                                @RequestParam(required = false) String profileImage,
                                @RequestParam(required = false) String fontFamily,
                                Model model) {
        boolean ok = userService.updateProfile(user, nickname, diaryTitle, bio, profileImage, fontFamily);
        if (!ok) {
            // 닉네임 중복: 입력값을 그대로 보여주며 에러 표시
            model.addAttribute("user", user);
            model.addAttribute("error", "이미 사용 중인 닉네임이에요");
            model.addAttribute("inputNickname", nickname);
            model.addAttribute("inputDiaryTitle", diaryTitle);
            model.addAttribute("inputBio", bio);
            return "profile";
        }
        return "redirect:/diaries";
    }
}
