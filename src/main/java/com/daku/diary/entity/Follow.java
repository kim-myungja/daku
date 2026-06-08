package com.daku.diary.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * 단방향 "이웃"(팔로우) 관계. follower 가 following 을 이웃으로 추가한 것.
 * (서로이웃 Neighbor 와 달리 한쪽 방향만 성립한다 — 인스타 팔로우 느낌)
 */
@Entity
@Table(name = "follows")
@Getter
@Setter
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "follower_id")
    private User follower;   // 이웃 추가한 사람

    @ManyToOne
    @JoinColumn(name = "following_id")
    private User following;  // 이웃으로 추가된 사람
}
