package com.daku.diary.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false)
    private String name; // 카테고리 이름 (예: 일상, 여행)

    @ManyToOne // 여러 카테고리가 한 명의 유저에 속함
    @JoinColumn(name = "user_id")
    private User user; // 이 카테고리의 주인

    @ManyToOne // 부모 카테고리 (null 이면 최상위)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false)
    private boolean locked = false; // 비공개(자물쇠) 여부

    @Column(nullable = false)
    private int sortOrder = 0; // 같은 레벨에서의 정렬 순서

    // ----- 화면 렌더링용 임시 필드 (DB 저장 안 함) -----
    @Transient
    private List<Category> children = new ArrayList<>(); // 하위 카테고리들

    @Transient
    private int depth = 0; // 계층 깊이 (0=최상위)

    @Transient
    private String displayName; // 드롭다운용: 깊이만큼 들여쓴 이름

    @Transient
    private long diaryCount = 0; // 이 카테고리에 속한 글 개수

    @Transient
    private List<Category> flatSubtree = new ArrayList<>(); // 자신 + 모든 자손을 평면화(depth 포함)
}
