package com.daku.diary.controller;

import com.daku.diary.annotation.CurrentUser;
import com.daku.diary.entity.Category;
import com.daku.diary.entity.Diary;
import com.daku.diary.entity.User;
import com.daku.diary.entity.Visibility;
import com.daku.diary.service.CategoryService;
import com.daku.diary.service.DiaryService;
import com.daku.diary.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/diaries")
public class DiaryController {

    private final DiaryService diaryService;
    private final CategoryService categoryService;
    private final UserService userService;

    @GetMapping
    public String list(@CurrentUser User user,
                       @RequestParam(required = false) Integer year,
                       @RequestParam(required = false) Integer month,
                       @RequestParam(required = false) String date,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) String category,
                       Model model) {
        List<Diary> all = diaryService.getMyDiaries(user);

        // --- 달력: 연/월 파라미터가 없으면 오늘 기준 ---
        LocalDate today = LocalDate.now();
        YearMonth ym = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.from(today);

        model.addAttribute("calendar", diaryService.buildCalendar(all, ym, today));
        model.addAttribute("calYear", ym.getYear());
        model.addAttribute("calMonth", ym.getMonthValue());
        YearMonth prev = ym.minusMonths(1);
        YearMonth next = ym.plusMonths(1);
        model.addAttribute("prevYear", prev.getYear());
        model.addAttribute("prevMonth", prev.getMonthValue());
        model.addAttribute("nextYear", next.getYear());
        model.addAttribute("nextMonth", next.getMonthValue());

        // --- 목록 필터: 검색(q) > 카테고리(category) > 날짜(date) > 전체 ---
        List<Diary> diaries;
        if (q != null && !q.isBlank()) {
            diaries = diaryService.searchByTitle(all, q);
        } else if (category != null && !category.isBlank()) {
            diaries = diaryService.filterByCategoryNames(all,
                    categoryService.getCategoryWithDescendantNames(user, category));
        } else if (date != null && !date.isBlank()) {
            diaries = diaryService.filterByDate(all, LocalDate.parse(date));
        } else {
            diaries = all;
        }

        // 카테고리 트리 구성 + 각 카테고리 글 개수 채우기
        List<Category> categoryTree = categoryService.getMyCategoryTree(user);
        categoryService.fillCounts(categoryTree, diaryService.countByCategory(all));

        boolean hasFilter = (q != null && !q.isBlank())
                || (category != null && !category.isBlank())
                || (date != null && !date.isBlank());

        model.addAttribute("diaries", diaries);
        model.addAttribute("diariesByCategory", diaryService.groupByCategory(all));
        model.addAttribute("categories", categoryTree);
        model.addAttribute("categoriesFlat", categoryService.flatten(categoryTree));
        model.addAttribute("totalCount", all.size());
        model.addAttribute("user", user);
        model.addAttribute("todayMood", user.getTodayMood());
        model.addAttribute("moodOptions", userService.getMoodOptions(user));
        model.addAttribute("q", q);
        model.addAttribute("filterDate", date);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("hasFilter", hasFilter);
        model.addAttribute("owner", true); // 내 다이어리 = 편집 가능
        model.addAttribute("baseUrl", "/diaries");
        return "diary/list";
    }

    // 사용자의 다이어리 폰트 (없으면 둥근모)
    private String fontOf(User u) {
        return (u.getFontFamily() != null && !u.getFontFamily().isBlank()) ? u.getFontFamily() : "DungGeunMo";
    }

    @GetMapping("/new")
    public String newForm(@CurrentUser User user, Model model) {
        List<Category> categories = categoryService.flatten(categoryService.getMyCategoryTree(user));
        model.addAttribute("categories", categories);
        model.addAttribute("userFont", fontOf(user));
        return "diary/form";
    }

    @PostMapping("/new")
    public String create(@CurrentUser User user,
                         @RequestParam String title,
                         @RequestParam String content,
                         @RequestParam String category,
                         @RequestParam(defaultValue = "PRIVATE") Visibility visibility,
                         @RequestParam(required = false) String elementsJson) {
        diaryService.create(title, content, category, visibility, elementsJson, user);
        return "redirect:/diaries";
    }

    // 상세보기 — 볼 권한 있는지 확인
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @CurrentUser User me,
                         Model model) {
        Diary diary = diaryService.getById(id);
        if (!diaryService.canView(diary, me)) {
            return "redirect:/diaries"; // 권한 없으면 내 목록으로 돌려보냄
        }
        model.addAttribute("diary", diary);
        model.addAttribute("isOwner", diaryService.isOwner(diary, me));
        model.addAttribute("userFont", fontOf(diary.getUser()));
        return "diary/detail";
    }

    // 수정 페이지 — 주인만
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @CurrentUser User me,
                           Model model) {
        Diary diary = diaryService.getById(id);
        if (!diaryService.isOwner(diary, me)) {
            return "redirect:/diaries";
        }
        List<Category> categories = categoryService.flatten(categoryService.getMyCategoryTree(me));
        model.addAttribute("diary", diary);
        model.addAttribute("categories", categories);
        model.addAttribute("userFont", fontOf(me));
        return "diary/edit";
    }

    // 수정 저장 — 주인만
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @CurrentUser User me,
                         @RequestParam String title,
                         @RequestParam String content,
                         @RequestParam(defaultValue = "") String category,
                         @RequestParam(defaultValue = "PRIVATE") Visibility visibility,
                         @RequestParam(required = false) String elementsJson) {
        Diary diary = diaryService.getById(id);
        if (!diaryService.isOwner(diary, me)) {
            return "redirect:/diaries";
        }
        diaryService.update(id, title, content, category, visibility, elementsJson);
        return "redirect:/diaries";
    }

    // 삭제 — 주인만
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @CurrentUser User me) {
        Diary diary = diaryService.getById(id);
        if (!diaryService.isOwner(diary, me)) {
            return "redirect:/diaries";
        }
        diaryService.delete(id);
        return "redirect:/diaries";
    }
}
