package com.daku.diary.repository;

import com.daku.diary.entity.Category;
import com.daku.diary.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 특정 유저의 카테고리 전부 가져오기
    List<Category> findByUser(User user);

    // 특정 카테고리의 바로 아래 하위 카테고리들
    List<Category> findByParent(Category parent);
}
