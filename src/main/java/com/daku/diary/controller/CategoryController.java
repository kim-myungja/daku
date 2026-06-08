package com.daku.diary.controller;

import com.daku.diary.annotation.CurrentUser;
import com.daku.diary.entity.User;
import com.daku.diary.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    // 카테고리 생성 (parentId 가 있으면 하위 카테고리, locked=true 면 비공개)
    @PostMapping("/new")
    public String create(@CurrentUser User user,
                         @RequestParam String name,
                         @RequestParam(required = false) Long parentId,
                         @RequestParam(required = false, defaultValue = "false") boolean locked) {
        categoryService.create(name, user, parentId, locked);
        return "redirect:/diaries";
    }

    // 카테고리 수정 — 이름/잠금 변경 (주인만)
    @PostMapping("/{id}/edit")
    public String update(@CurrentUser User user,
                         @PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam(required = false, defaultValue = "false") boolean locked) {
        categoryService.update(id, user, name, locked);
        return "redirect:/diaries";
    }

    // 카테고리 순서 이동 (같은 레벨 위/아래) — 주인만
    @PostMapping("/{id}/move")
    public String move(@CurrentUser User user,
                       @PathVariable Long id,
                       @RequestParam String direction) {
        categoryService.move(id, user, direction);
        return "redirect:/diaries";
    }

    // 카테고리 삭제 — 주인만, 하위 카테고리까지 함께 삭제
    @PostMapping("/{id}/delete")
    public String delete(@CurrentUser User user,
                         @PathVariable Long id) {
        categoryService.delete(id, user);
        return "redirect:/diaries";
    }
}
