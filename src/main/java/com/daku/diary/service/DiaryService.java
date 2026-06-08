package com.daku.diary.service;

import com.daku.diary.dto.CalendarDay;
import com.daku.diary.entity.Diary;
import com.daku.diary.entity.User;
import com.daku.diary.entity.Visibility;
import com.daku.diary.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final NeighborService neighborService;
    private final CategoryService categoryService;

    public void create(String title, String content, String category, Visibility visibility, String elementsJson, User user) {
        Diary diary = new Diary();
        diary.setTitle(title);
        diary.setContent(content);
        diary.setCategory(category);
        diary.setVisibility(visibility);
        diary.setElementsJson(elementsJson);
        diary.setUser(user);
        diaryRepository.save(diary);
    }

    public List<Diary> getMyDiaries(User user) {
        return diaryRepository.findByUser(user);
    }

    public Diary getById(Long id) {
        return diaryRepository.findById(id).orElseThrow();
    }

    public void update(Long id, String title, String content, String category, Visibility visibility, String elementsJson) {
        Diary diary = getById(id);
        diary.setTitle(title);
        diary.setContent(content);
        diary.setCategory(category);
        diary.setVisibility(visibility);
        diary.setElementsJson(elementsJson);
        diaryRepository.save(diary);
    }

    public void delete(Long id) {
        diaryRepository.deleteById(id);
    }

    // 주인 여부
    public boolean isOwner(Diary diary, User user) {
        return diary.getUser().getId().equals(user.getId());
    }

    // 이 사람이 이 다이어리를 볼 수 있는가? (핵심 인가 로직)
    public boolean canView(Diary diary, User viewer) {
        if (isOwner(diary, viewer)) return true; // 주인은 항상 OK

        Visibility v = diary.getVisibility();
        if (v == null) return false; // 공개범위 값이 없으면 안전하게 비공개 취급

        switch (v) {
            case PUBLIC:   return true;                                  // 전체공개
            case NEIGHBOR: return neighborService.isNeighbor(diary.getUser(), viewer); // 서로이웃만
            default:       return false;                                 // 비공개
        }
    }

    // 특정 유저의 다이어리 중 내가 볼 수 있는 것만 (비공개 카테고리 글은 본인만)
    public List<Diary> getViewableDiariesOf(User target, User viewer) {
        boolean isSelf = target.getId().equals(viewer.getId());
        Set<String> privateCats = isSelf ? Set.of() : categoryService.getPrivateCategoryNames(target);
        List<Diary> result = new ArrayList<>();
        for (Diary d : diaryRepository.findByUser(target)) {
            if (!canView(d, viewer)) continue;
            // 비공개(자물쇠) 카테고리 + 그 하위 글은 방문자에게 숨김
            if (!isSelf && d.getCategory() != null && privateCats.contains(d.getCategory())) continue;
            result.add(d);
        }
        return result;
    }

    // 제목으로 내 다이어리 검색 (대소문자 무시)
    public List<Diary> searchByTitle(List<Diary> diaries, String keyword) {
        String kw = keyword.trim().toLowerCase();
        return diaries.stream()
                .filter(d -> d.getTitle() != null && d.getTitle().toLowerCase().contains(kw))
                .toList();
    }

    // 특정 날짜에 작성한 다이어리만
    public List<Diary> filterByDate(List<Diary> diaries, LocalDate date) {
        return diaries.stream()
                .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().toLocalDate().equals(date))
                .toList();
    }

    // 특정 카테고리에 속한 다이어리만
    public List<Diary> filterByCategory(List<Diary> diaries, String category) {
        return diaries.stream()
                .filter(d -> category.equals(d.getCategory()))
                .toList();
    }

    // 여러 카테고리 이름(자신 + 하위) 중 하나에 속한 다이어리
    public List<Diary> filterByCategoryNames(List<Diary> diaries, Set<String> names) {
        return diaries.stream()
                .filter(d -> d.getCategory() != null && names.contains(d.getCategory()))
                .toList();
    }

    // 카테고리 이름별 글 개수 (사이드바 뱃지용)
    public Map<String, Long> countByCategory(List<Diary> diaries) {
        return diaries.stream()
                .filter(d -> d.getCategory() != null && !d.getCategory().isBlank())
                .collect(Collectors.groupingBy(Diary::getCategory, Collectors.counting()));
    }

    // 카테고리 이름별 글 목록 (카테고리 빈 값은 "" 키 = 미분류)
    public Map<String, List<Diary>> groupByCategory(List<Diary> diaries) {
        Map<String, List<Diary>> map = new HashMap<>();
        for (Diary d : diaries) {
            String key = (d.getCategory() == null || d.getCategory().isBlank()) ? "" : d.getCategory();
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
        }
        return map;
    }

    // 달력 한 달치 칸 목록 만들기 (일요일 시작, 일기 있는 날 표시)
    public List<CalendarDay> buildCalendar(List<Diary> diaries, YearMonth ym, LocalDate today) {
        // 이 달에 일기가 있는 '일(day)' 집합
        Set<Integer> daysWithDiary = diaries.stream()
                .map(d -> d.getCreatedAt() != null ? d.getCreatedAt().toLocalDate() : null)
                .filter(ld -> ld != null
                        && ld.getYear() == ym.getYear()
                        && ld.getMonthValue() == ym.getMonthValue())
                .map(LocalDate::getDayOfMonth)
                .collect(Collectors.toSet());

        List<CalendarDay> cells = new ArrayList<>();

        // 1일이 무슨 요일인지 → 앞쪽 빈 칸 개수 (일요일=0 시작)
        int lead = ym.atDay(1).getDayOfWeek().getValue() % 7;
        for (int i = 0; i < lead; i++) {
            cells.add(CalendarDay.blank());
        }

        // 실제 날짜 칸
        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate cur = ym.atDay(day);
            cells.add(new CalendarDay(
                    day,
                    daysWithDiary.contains(day),
                    cur.equals(today),
                    cur.toString()
            ));
        }
        return cells;
    }
}