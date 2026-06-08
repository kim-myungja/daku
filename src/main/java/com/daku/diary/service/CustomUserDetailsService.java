package com.daku.diary.service;

import com.daku.diary.entity.User;
import com.daku.diary.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // DB에서 유저 찾기
        User user = userRepository.findByUsername(username);

        // 유저가 없으면 예외 던지기
        if (user == null) {
            throw new UsernameNotFoundException("유저를 찾을 수 없습니다: " + username);
        }

        // Spring Security가 이해하는 형태로 변환해서 반환
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();
    }
}