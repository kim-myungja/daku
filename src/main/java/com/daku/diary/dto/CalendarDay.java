package com.daku.diary.dto;

/**
 * 달력의 한 칸을 표현하는 DTO.
 * day 가 0 이면 그 달 시작 전 빈 칸(앞쪽 여백)을 의미한다.
 *
 * @param day      날짜 (1~31), 빈 칸이면 0
 * @param hasDiary 그 날 작성한 다이어리가 있는지
 * @param today    오늘 날짜인지
 * @param dateStr  yyyy-MM-dd 형식 문자열 (날짜 클릭 링크용), 빈 칸이면 null
 */
public record CalendarDay(int day, boolean hasDiary, boolean today, String dateStr) {

    // 그 달 1일 앞쪽의 빈 칸
    public static CalendarDay blank() {
        return new CalendarDay(0, false, false, null);
    }

    // Thymeleaf 에서 ${cell.blank} 로 접근
    public boolean isBlank() {
        return day == 0;
    }
}
