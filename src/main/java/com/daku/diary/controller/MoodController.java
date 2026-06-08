package com.daku.diary.controller;

import com.daku.diary.annotation.CurrentUser;
import com.daku.diary.entity.User;
import com.daku.diary.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * "TODAY IS" 무드 기능 — 비동기(AJAX) 처리.
 */
@Controller
@RequiredArgsConstructor
public class MoodController {

    private final UserService userService;

    // 현재 무드 선택
    @PostMapping("/mood/set")
    @ResponseBody
    public Map<String, Object> set(@CurrentUser User user, @RequestParam String mood) {
        userService.setTodayMood(user, mood);
        Map<String, Object> res = new HashMap<>();
        res.put("mood", mood);
        return res;
    }

    // 무드 선택지 추가
    @PostMapping("/mood/add")
    @ResponseBody
    public Map<String, Object> add(@CurrentUser User user, @RequestParam String mood) {
        Map<String, Object> res = new HashMap<>();
        res.put("options", userService.addMood(user, mood));
        return res;
    }

    // 무드 선택지 삭제
    @PostMapping("/mood/delete")
    @ResponseBody
    public Map<String, Object> delete(@CurrentUser User user, @RequestParam String mood) {
        Map<String, Object> res = new HashMap<>();
        res.put("options", userService.deleteMood(user, mood));
        return res;
    }
}
