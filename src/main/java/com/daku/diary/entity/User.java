package com.daku.diary.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE) // id 는 DB 가 자동 생성하므로 외부에서 못 바꾸게
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(unique = true)
    private String nickname;

    @Column
    private String profileImage;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column
    private String diaryTitle;

    @Column
    private String todayMood; // 현재 선택한 "TODAY IS" 무드

    @Column(columnDefinition = "TEXT")
    private String moodOptions; // 무드 선택지 목록 ('|' 구분)

    @Column
    private String fontFamily; // 다이어리 UI 폰트 (CSS font-family 값)
}
