package com.daku.diary.controller;

import com.daku.diary.annotation.CurrentUser;
import com.daku.diary.entity.Category;
import com.daku.diary.entity.Diary;
import com.daku.diary.entity.User;
import com.daku.diary.service.CategoryService;
import com.daku.diary.service.DiaryService;
import com.daku.diary.service.NeighborService;
import com.daku.diary.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class NeighborController {

    private final NeighborService neighborService;
    private final UserService userService;
    private final DiaryService diaryService;
    private final CategoryService categoryService;

    // 탐색 — 닉네임 검색했을 때만 결과 표시
    @GetMapping("/explore")
    public String explore(@CurrentUser User me,
                          @RequestParam(required = false) String keyword,
                          Model model) {
        List<User> others = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            others = userService.searchByNickname(keyword.trim(), me);
        }
        Map<Long, String> relations = new HashMap<>();
        Map<Long, Boolean> following = new HashMap<>();
        for (User u : others) {
            relations.put(u.getId(), neighborService.relationStatus(me, u));
            following.put(u.getId(), neighborService.isFollowing(me, u));
        }
        model.addAttribute("others", others);
        model.addAttribute("relations", relations);
        model.addAttribute("following", following);
        model.addAttribute("keyword", keyword);
        return "neighbor/explore";
    }

    @PostMapping("/neighbors/request")
    public String request(@CurrentUser User me,
                          @RequestParam Long toUserId) {
        neighborService.sendRequest(me, toUserId);
        return "redirect:/explore";
    }

    // 이웃 추가 (팔로우)
    @PostMapping("/follow")
    public String follow(@CurrentUser User me, @RequestParam Long toUserId) {
        neighborService.follow(me, toUserId);
        return "redirect:/explore";
    }

    // 이웃 취소 (언팔로우)
    @PostMapping("/unfollow")
    public String unfollow(@CurrentUser User me, @RequestParam Long toUserId,
                           @RequestParam(required = false, defaultValue = "/neighbors") String from) {
        neighborService.unfollow(me, toUserId);
        return "redirect:" + from;
    }

    @GetMapping("/neighbors")
    public String neighbors(@CurrentUser User me, Model model) {
        model.addAttribute("pending", neighborService.getReceivedPending(me));
        model.addAttribute("myNeighbors", neighborService.getMyNeighbors(me));
        model.addAttribute("following", neighborService.getFollowing(me));
        return "neighbor/neighbors";
    }

    @PostMapping("/neighbors/{id}/accept")
    public String accept(@CurrentUser User me,
                         @PathVariable Long id) {
        neighborService.acceptRequest(id, me);
        return "redirect:/neighbors";
    }

    @PostMapping("/neighbors/{id}/reject")
    public String reject(@CurrentUser User me,
                         @PathVariable Long id) {
        neighborService.rejectRequest(id, me);
        return "redirect:/neighbors";
    }

    // 이웃의 다이어리 방문 — 메인 화면(list.html)을 방문자 모드로 재사용. 공개 글만 보임.
    @GetMapping("/users/{id}")
    public String visit(@CurrentUser User me,
                        @PathVariable Long id,
                        @RequestParam(required = false) Integer year,
                        @RequestParam(required = false) Integer month,
                        @RequestParam(required = false) String date,
                        @RequestParam(required = false) String category,
                        Model model) {
        User target = userService.findById(id);
        if (target.getId().equals(me.getId())) {
            return "redirect:/diaries"; // 내 다이어리는 그냥 내 메인으로
        }
        // 내가 볼 수 있는 글만 (비공개 제외 — 공개 + 서로이웃공개)
        List<Diary> all = diaryService.getViewableDiariesOf(target, me);

        // 달력
        LocalDate today = LocalDate.now();
        YearMonth ym = (year != null && month != null) ? YearMonth.of(year, month) : YearMonth.from(today);
        model.addAttribute("calendar", diaryService.buildCalendar(all, ym, today));
        model.addAttribute("calYear", ym.getYear());
        model.addAttribute("calMonth", ym.getMonthValue());
        YearMonth prev = ym.minusMonths(1), next = ym.plusMonths(1);
        model.addAttribute("prevYear", prev.getYear());
        model.addAttribute("prevMonth", prev.getMonthValue());
        model.addAttribute("nextYear", next.getYear());
        model.addAttribute("nextMonth", next.getMonthValue());

        // 목록 필터 (카테고리/날짜)
        List<Diary> diaries;
        if (category != null && !category.isBlank()) {
            diaries = diaryService.filterByCategoryNames(all,
                    categoryService.getCategoryWithDescendantNames(target, category));
        } else if (date != null && !date.isBlank()) {
            diaries = diaryService.filterByDate(all, LocalDate.parse(date));
        } else {
            diaries = all;
        }

        // 카테고리 트리 (방문 대상의) + 공개 글 개수
        List<Category> categoryTree = categoryService.getMyCategoryTree(target);
        categoryService.fillCounts(categoryTree, diaryService.countByCategory(all));

        boolean hasFilter = (category != null && !category.isBlank())
                || (date != null && !date.isBlank());

        model.addAttribute("diaries", diaries);
        model.addAttribute("diariesByCategory", diaryService.groupByCategory(all));
        model.addAttribute("categories", categoryTree);
        model.addAttribute("totalCount", all.size());
        model.addAttribute("user", target);          // 표시 대상 = 방문한 사람
        model.addAttribute("todayMood", target.getTodayMood());
        model.addAttribute("filterDate", date);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("hasFilter", hasFilter);
        model.addAttribute("owner", false);          // 방문자 모드 = 편집 불가
        model.addAttribute("baseUrl", "/users/" + target.getId());
        // 방문자-대상 관계 (버튼 표시용)
        model.addAttribute("targetId", target.getId());
        model.addAttribute("isNeighbor", neighborService.isNeighbor(me, target));
        model.addAttribute("isFollowing", neighborService.isFollowing(me, target));
        model.addAttribute("relationStatus", neighborService.relationStatus(me, target));
        return "diary/list";
    }
}
