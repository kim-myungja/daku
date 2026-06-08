package com.daku.diary.repository;

import com.daku.diary.entity.Diary;
import com.daku.diary.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// <Diary, Long> = Diary 테이블을, id타입은 Long으로 쓰겠다
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    // 특정 유저의 다이어리 목록 전부 가져오기
    List<Diary> findByUser(User user);
}