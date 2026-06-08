package com.daku.diary.service;

import com.daku.diary.entity.User;
import com.daku.diary.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 무드 기본 선택지 (사용자가 아직 직접 만든 게 없을 때)
    private static final List<String> DEFAULT_MOODS =
            List.of("😊 행복", "😢 슬픔", "😡 화남", "🥱 피곤", "🥰 사랑", "😎 신남");

    public void register(String username, String password, String nickname, String diaryTitle) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디예요");
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임이에요");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setNickname(nickname);
        user.setDiaryTitle(diaryTitle);
        userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    public List<User> findAllExcept(User me) {
        return userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(me.getId()))
                .toList();
    }

    // 닉네임으로 검색 (나 자신 제외)
    public List<User> searchByNickname(String keyword, User me) {
        return userRepository.findByNicknameContainingIgnoreCase(keyword).stream()
                .filter(u -> !u.getId().equals(me.getId()))
                .toList();
    }

    // 닉네임 중복 여부
    public boolean isNicknameTaken(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    /**
     * 프로필 수정. 닉네임을 다른 사람이 이미 쓰고 있으면 false 를 반환하고 아무것도 바꾸지 않는다.
     * profileImage 가 비어 있으면 기존 사진을 그대로 유지한다.
     */
    public boolean updateProfile(User user, String nickname, String diaryTitle, String bio,
                                 String profileImage, String fontFamily) {
        // 닉네임이 실제로 바뀌었고, 그 닉네임을 이미 다른 사람이 쓰고 있으면 실패
        if (nickname != null && !nickname.equals(user.getNickname())
                && userRepository.existsByNickname(nickname)) {
            return false;
        }
        user.setNickname(nickname);
        user.setDiaryTitle(diaryTitle);
        user.setBio(bio);
        if (profileImage != null && !profileImage.isBlank()) {
            user.setProfileImage(profileImage);
        }
        if (fontFamily != null && !fontFamily.isBlank()) {
            user.setFontFamily(fontFamily);
        }
        userRepository.save(user);
        return true;
    }

    // ----- TODAY IS 무드 -----

    // 무드 선택지 목록 (없으면 기본값)
    public List<String> getMoodOptions(User user) {
        String raw = user.getMoodOptions();
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>(DEFAULT_MOODS);
        }
        List<String> list = new ArrayList<>();
        for (String s : raw.split("\\|")) {
            if (!s.isBlank()) list.add(s);
        }
        return list.isEmpty() ? new ArrayList<>(DEFAULT_MOODS) : list;
    }

    // 현재 무드 설정
    public void setTodayMood(User user, String mood) {
        user.setTodayMood(mood);
        userRepository.save(user);
    }

    // 무드 선택지 추가
    public List<String> addMood(User user, String mood) {
        if (mood == null || mood.isBlank()) return getMoodOptions(user);
        List<String> list = getMoodOptions(user);
        String trimmed = mood.trim();
        if (!list.contains(trimmed)) list.add(trimmed);
        user.setMoodOptions(String.join("|", list));
        userRepository.save(user);
        return list;
    }

    // 무드 선택지 삭제
    public List<String> deleteMood(User user, String mood) {
        List<String> list = getMoodOptions(user);
        list.remove(mood);
        user.setMoodOptions(String.join("|", list));
        // 삭제한 게 현재 무드면 현재 무드도 비움
        if (mood != null && mood.equals(user.getTodayMood())) {
            user.setTodayMood(null);
        }
        userRepository.save(user);
        return list;
    }
}