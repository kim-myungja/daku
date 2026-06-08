package com.daku.diary.repository;

import com.daku.diary.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String username);

    List<User> findByNicknameContainingIgnoreCase(String nickname);

    boolean existsByNickname(String nickname);

    boolean existsByUsername(String username);
}