package com.daku.diary.service;

import com.daku.diary.entity.Category;
import com.daku.diary.entity.Diary;
import com.daku.diary.entity.User;
import com.daku.diary.repository.CategoryRepository;
import com.daku.diary.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final DiaryRepository diaryRepository;

    // 내 카테고리 평면 목록 (계층 상관없이 전부)
    public List<Category> getMyCategories(User user) {
        return categoryRepository.findByUser(user);
    }

    // 내 카테고리를 트리로 (최상위 목록, 각 노드에 children/depth/displayName 채워짐)
    public List<Category> getMyCategoryTree(User user) {
        List<Category> all = categoryRepository.findByUser(user);
        Map<Long, Category> byId = new HashMap<>();
        for (Category c : all) {
            c.getChildren().clear();
            byId.put(c.getId(), c);
        }
        List<Category> roots = new ArrayList<>();
        for (Category c : all) {
            Category parent = c.getParent();
            if (parent != null && byId.containsKey(parent.getId())) {
                byId.get(parent.getId()).getChildren().add(c);
            } else {
                roots.add(c);
            }
        }
        setDepthAndDisplay(roots, 0);
        // 각 최상위 카테고리에 대해 자신+자손을 평면화 (화면에서 재귀 없이 순회하려고)
        for (Category root : roots) {
            List<Category> flat = new ArrayList<>();
            collectSubtree(root, flat);
            root.setFlatSubtree(flat);
        }
        return roots;
    }

    private void collectSubtree(Category c, List<Category> out) {
        out.add(c);
        for (Category child : c.getChildren()) {
            collectSubtree(child, out);
        }
    }

    // 카테고리 이름 + 그 모든 하위 카테고리 이름 집합 (상위 폴더 클릭 시 하위 글까지 보려고)
    public Set<String> getCategoryWithDescendantNames(User user, String name) {
        List<Category> all = categoryRepository.findByUser(user);
        Set<String> result = new HashSet<>();
        Category start = all.stream().filter(c -> name.equals(c.getName())).findFirst().orElse(null);
        if (start == null) {
            result.add(name);
            return result;
        }
        collectDescendantNames(start, all, result);
        return result;
    }

    private void collectDescendantNames(Category c, List<Category> all, Set<String> out) {
        out.add(c.getName());
        for (Category child : all) {
            if (child.getParent() != null && child.getParent().getId().equals(c.getId())) {
                collectDescendantNames(child, all, out);
            }
        }
    }

    // 비공개(locked) 카테고리 + 그 모든 하위 카테고리 이름 (방문자에게 숨길 글의 카테고리)
    public Set<String> getPrivateCategoryNames(User user) {
        List<Category> all = categoryRepository.findByUser(user);
        Set<String> result = new HashSet<>();
        for (Category c : all) {
            if (c.isLocked()) {
                collectDescendantNames(c, all, result);
            }
        }
        return result;
    }

    // 트리를 평면 리스트로 (드롭다운 선택용, displayName 에 들여쓰기 포함)
    public List<Category> flatten(List<Category> roots) {
        List<Category> out = new ArrayList<>();
        flattenInto(roots, out);
        return out;
    }

    // 트리 노드에 글 개수 채우기 — 부모는 자신 + 모든 하위 글 수의 합
    public void fillCounts(List<Category> nodes, Map<String, Long> counts) {
        for (Category c : nodes) {
            fillCounts(c.getChildren(), counts); // 자식 먼저 계산
            long own = counts.getOrDefault(c.getName(), 0L);
            long childSum = 0;
            for (Category child : c.getChildren()) {
                childSum += child.getDiaryCount();
            }
            c.setDiaryCount(own + childSum);
        }
    }

    // 카테고리 생성 (parentId 가 있으면 하위 카테고리로)
    public void create(String name, User user, Long parentId, boolean locked) {
        Category category = new Category();
        category.setName(name);
        category.setUser(user);
        category.setLocked(locked);
        Long pid = null;
        if (parentId != null) {
            Category parent = categoryRepository.findById(parentId)
                    .filter(p -> p.getUser().getId().equals(user.getId()))
                    .orElse(null);
            if (parent != null) {
                category.setParent(parent);
                pid = parent.getId();
            }
        }
        // 같은 레벨의 맨 뒤에 오도록 sortOrder = (형제 최대값 + 1)
        category.setSortOrder(siblingsOf(user, pid).stream()
                .mapToInt(Category::getSortOrder).max().orElse(-1) + 1);
        categoryRepository.save(category);
    }

    // 같은 부모를 가진 형제 카테고리들 (parentId 가 null 이면 최상위)
    private List<Category> siblingsOf(User user, Long parentId) {
        return categoryRepository.findByUser(user).stream()
                .filter(c -> {
                    Long pid = c.getParent() != null ? c.getParent().getId() : null;
                    return Objects.equals(pid, parentId);
                })
                .sorted(Comparator.comparingInt(Category::getSortOrder).thenComparing(Category::getId))
                .collect(Collectors.toList());
    }

    // 카테고리 순서 이동 (같은 레벨에서 위/아래로) — 주인만
    public void move(Long id, User user, String direction) {
        Category cat = categoryRepository.findById(id).orElse(null);
        if (cat == null || !cat.getUser().getId().equals(user.getId())) return;
        Long parentId = cat.getParent() != null ? cat.getParent().getId() : null;
        List<Category> siblings = siblingsOf(user, parentId);

        int idx = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getId().equals(id)) { idx = i; break; }
        }
        if (idx < 0) return;
        int swap = "up".equals(direction) ? idx - 1 : idx + 1;
        if (swap < 0 || swap >= siblings.size()) return;

        Collections.swap(siblings, idx, swap);
        // 0,1,2... 로 정규화해서 저장
        for (int i = 0; i < siblings.size(); i++) siblings.get(i).setSortOrder(i);
        categoryRepository.saveAll(siblings);
    }

    // 카테고리 수정 — 이름/잠금 변경 (주인만)
    public void update(Long id, User user, String name, boolean locked) {
        Category c = categoryRepository.findById(id).orElse(null);
        if (c == null || !c.getUser().getId().equals(user.getId())) return;
        if (name != null && !name.isBlank()) c.setName(name);
        c.setLocked(locked);
        categoryRepository.save(c);
    }

    // 카테고리 삭제 — 주인만, 하위 카테고리까지 함께 삭제
    public void delete(Long id, User user) {
        Category c = categoryRepository.findById(id).orElse(null);
        if (c == null || !c.getUser().getId().equals(user.getId())) return;
        deleteRecursive(c);
    }

    private void deleteRecursive(Category c) {
        for (Category child : categoryRepository.findByParent(c)) {
            deleteRecursive(child);
        }
        // 이 카테고리 이름의 글들은 미분류("")로 옮긴다 (글 자체는 보존)
        for (Diary d : diaryRepository.findByUser(c.getUser())) {
            if (c.getName().equals(d.getCategory())) {
                d.setCategory("");
                diaryRepository.save(d);
            }
        }
        categoryRepository.delete(c);
    }

    private void setDepthAndDisplay(List<Category> nodes, int depth) {
        // 같은 레벨은 sortOrder(같으면 id) 순으로 정렬
        nodes.sort(Comparator.comparingInt(Category::getSortOrder).thenComparing(Category::getId));
        for (Category c : nodes) {
            c.setDepth(depth);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < depth; i++) sb.append("— ");
            c.setDisplayName(sb + c.getName());
            setDepthAndDisplay(c.getChildren(), depth + 1);
        }
    }

    private void flattenInto(List<Category> nodes, List<Category> out) {
        for (Category c : nodes) {
            out.add(c);
            flattenInto(c.getChildren(), out);
        }
    }
}
