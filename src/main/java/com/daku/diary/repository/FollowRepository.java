package com.daku.diary.repository;

import com.daku.diary.entity.Follow;
import com.daku.diary.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerAndFollowing(User follower, User following);

    Optional<Follow> findByFollowerAndFollowing(User follower, User following);

    List<Follow> findByFollower(User follower);   // 내가 이웃 추가한 사람들

    List<Follow> findByFollowing(User following); // 나를 이웃 추가한 사람들
}
