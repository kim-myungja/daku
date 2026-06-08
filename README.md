# ☁️ 서울 남성의 몰래 숨겨둔 한 다이어리 

> 레트로 감성으로 만든 다이어리입니다. 사진과 텍스트를 자유롭게 배치할 수 있게 하여 실제 다이어리처럼 만든 것이 특징입니다.

**4차 해커톤 — Java Spring Boot & Thymeleaf 기반 웹 서비스 구축 및 클라우드 배포 챌린지** 제출작.

---

## 1. 프로젝트 개요

- **수행 주제**: Thymeleaf 기반 회원 맞춤형 캔버스 다이어리 & 소셜(이웃) 서비스
- **컨셉**: 사진을 자유롭게 배치하는 캔버스형 다이어리를 "노트(다이어리)" 메타포로 꾸미고, 계층형 카테고리·달력·서로이웃/이웃(팔로우) 기능으로 미니홈피처럼 운영
- **사용 기술**

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.14, Spring Security, Spring Data JPA |
| View | Thymeleaf, HTML/CSS, Vanilla JS (Pointer Events 캔버스 에디터) |
| DB | MariaDB |
| Build | Gradle |
| Infra | GCP VM (Compute Engine), Cloudflare (도메인 + SSL) |
| 기타 | Lombok, BCrypt, 둥근모/구글 웹폰트 |

### 주요 기능
- 🔐 **회원가입 / 로그인** — Spring Security 폼 로그인, 아이디·닉네임 중복 검증
- 📓 **캔버스 다이어리(CRUD)** — 텍스트·사진을 드래그/리사이즈로 배치, **멀티 페이지**(노트 넘기기), 사진 원본 비율 유지
- 🗂️ **계층형 카테고리** — 부모-자식 트리, 글 개수(하위 누적), 펼침/접기 토글, **순서 변경(EDIT 모드)**, **비공개(자물쇠)**
- 📅 **달력** — 월 이동, 글 쓴 날 강조, 날짜별 필터 (옛날 탁상달력 스타일)
- 🎭 **TODAY IS 무드** — 싸이월드식 무드 선택 + 노션식 비동기(AJAX) 속성 편집
- 👤 **프로필** — 사진/닉네임/한줄소개 수정, **다이어리 폰트 5종 선택**
- 👥 **소셜** — 양방향 **서로이웃**(신청/수락/거절) + 단방향 **이웃(팔로우)**, 이웃 다이어리 **방문 모드**(공개 글만)
- 🔒 **공개 범위** — 글별 전체공개/서로이웃공개/비공개 + 카테고리 잠금 상속

---

## 2. 스프링 아키텍처 및 서비스 구조

### 표준 레이어드 아키텍처 (UI → Controller → Service → Repository → DB)

```
com.daku.diary
├── controller   ← 요청 매핑 / 화면 반환 (얇게 유지)
│   ├── AuthController        (로그인·회원가입 화면/처리)
│   ├── DiaryController       (다이어리 CRUD, 메인 화면)
│   ├── CategoryController    (카테고리 생성/수정/삭제/이동)
│   ├── NeighborController    (서로이웃·이웃·방문)
│   ├── UserController        (프로필 수정)
│   ├── MoodController        (TODAY IS 무드 AJAX)
│   └── FileUploadController  (이미지 업로드)
├── service      ← 비즈니스 로직 / 트랜잭션 / 권한 판단
│   ├── UserService, DiaryService, CategoryService, NeighborService
│   └── CustomUserDetailsService (Spring Security 연동)
├── repository   ← Spring Data JPA (DB 접근)
│   ├── UserRepository, DiaryRepository, CategoryRepository
│   └── NeighborRepository, FollowRepository
├── entity       ← JPA 엔티티 (User, Diary, Category, Neighbor, Follow)
│   └── Visibility, NeighborStatus (enum)
├── dto          ← CalendarDay (화면 전용 데이터)
├── config       ← SecurityConfig, WebConfig, CurrentUserArgumentResolver
└── annotation   ← @CurrentUser (커스텀 파라미터 어노테이션)
```

**계층 분리 원칙**
- **Controller**: 파라미터 바인딩 + Service 호출 + 뷰 반환만 담당. 비즈니스 판단 없음.
- **Service**: 권한 확인(소유자/방문자), 트리 구성, 공개범위 필터 등 핵심 로직. 컨트롤러에서 반복되던 인증 사용자 조회는 `@CurrentUser` ArgumentResolver로 일원화.
- **Repository**: `JpaRepository` 상속, 쿼리 메서드(파생 쿼리)로 CRUD.

### 주요 URL 및 권한 라우팅

| URL | 권한 | 설명 |
|-----|------|------|
| `/login`, `/register` | 누구나 | 로그인 / 회원가입 |
| `/css/**`, `/js/**`, `/images/**`, `/fonts/**`, `/uploads/**` | 누구나 | 정적 리소스 |
| `/diaries` | 인증 사용자 | 내 다이어리 메인(목록 조회) |
| `/diaries/new`, `/{id}/edit`, `/{id}/delete` | 인증 사용자(소유자) | 글 생성·수정·삭제 |
| `/users/{id}` | 인증 사용자 | 이웃 다이어리 방문(공개 글만) |
| `/categories/**`, `/mood/**`, `/profile` | 인증 사용자 | 카테고리·무드·프로필 |
| `/neighbors/**`, `/follow`, `/unfollow`, `/explore` | 인증 사용자 | 서로이웃·이웃 |
| `/admin/**` | `ROLE_ADMIN` | 관리자 전용(권한 라우팅 정의) |
| 그 외 모든 요청 | 인증 사용자 | `anyRequest().authenticated()` |

---

## 3. Spring Security 인증/인가 설정

- **폼 로그인**: `SecurityFilterChain`에서 `formLogin`으로 `/login` 커스텀 페이지 지정, 성공 시 `/diaries`로 이동
- **사용자 인증**: `CustomUserDetailsService`가 `UserRepository`로 사용자를 조회해 `UserDetails`로 변환, **BCrypt**로 비밀번호 검증
- **인가(권한)**: `users.role` 컬럼(`USER`/`ADMIN`) 기반. `/admin/**`은 `hasRole("ADMIN")`, 정적 리소스·로그인·회원가입은 `permitAll()`, 나머지는 `authenticated()`
- **세션 관리**: 스프링 시큐리티 기본 세션(JSESSIONID) 사용, 로그아웃 시 세션 무효화 후 `/login`
- **CSRF**: 기본 활성화. 폼은 Thymeleaf가 자동으로 `_csrf` 히든 필드 삽입, **AJAX(무드 추가/삭제)는 `X-CSRF-TOKEN` 헤더**로 토큰 전달
- **인증 사용자 주입**: `@CurrentUser User user` — `HandlerMethodArgumentResolver`가 `SecurityContext`에서 현재 사용자를 꺼내 컨트롤러에 바로 주입(중복 코드 제거)

---

## 4. 데이터베이스 및 SQL 활용

### 사용 테이블

| 테이블 | 설명 | 주요 컬럼 |
|--------|------|-----------|
| `users` | 회원 | id, username(unique), password, role, nickname, profile_image, bio, diary_title, today_mood, mood_options, font_family |
| `diaries` | 다이어리 글 | id, title, content, category, visibility(PUBLIC/NEIGHBOR/PRIVATE), elements_json(LONGTEXT), created_at, user_id(FK) |
| `categories` | 카테고리(계층) | id, name, user_id(FK), parent_id(FK self), locked, sort_order |
| `neighbors` | 서로이웃(양방향) | id, requester_id, receiver_id, status(PENDING/ACCEPTED), created_at |
| `follows` | 이웃(단방향 팔로우) | id, follower_id, following_id |

- ORM은 **Spring Data JPA**(Hibernate, `ddl-auto: update`)를 사용하며, 아래는 JPA 파생 쿼리가 실행하는 대표 SQL입니다.

### 주요 SQL (CRUD)
```sql
-- [C] 글 작성
INSERT INTO diaries(title, content, category, visibility, elements_json, created_at, user_id)
VALUES (?, ?, ?, ?, ?, ?, ?);

-- [R] 내 다이어리 목록
SELECT * FROM diaries WHERE user_id = ?;

-- [R] 방문 시 공개 글만 (비공개 카테고리 글 제외는 서비스 계층에서 필터)
SELECT * FROM diaries WHERE user_id = ? AND visibility <> 'PRIVATE';

-- [U] 글 수정
UPDATE diaries SET title=?, content=?, category=?, visibility=?, elements_json=? WHERE id=?;

-- [D] 글 삭제
DELETE FROM diaries WHERE id = ?;

-- 계층 카테고리 조회 / 순서
SELECT * FROM categories WHERE user_id = ? ORDER BY sort_order, id;

-- 서로이웃 수락 상태 확인
SELECT EXISTS(SELECT 1 FROM neighbors WHERE requester_id=? AND receiver_id=? AND status='ACCEPTED');
```

---

## 5. 트러블슈팅 (문제 해결 기록)

1. **Thymeleaf 재귀 프래그먼트에서 파라미터가 `null`**
   계층 카테고리를 재귀 `th:replace`로 렌더하니 `cat` 파라미터가 null로 바인딩돼 `EL1007E` 발생. → 서비스 계층에서 트리를 **평면화(flatSubtree + depth)** 하여 단순 `th:each` 순회로 렌더하도록 변경(재귀 제거).

2. **AJAX 요청이 CSRF로 403**
   무드 추가/삭제 비동기 요청이 차단됨. → `<meta name="_csrf">`/`_csrf_header`를 읽어 `fetch` 시 **`X-CSRF-TOKEN` 헤더**로 전달하여 해결.

3. **Spring Security 정적 리소스(이미지·폰트) 차단**
   배경 이미지/웹폰트가 401. → `SecurityConfig`에 `/images/**`, `/fonts/**`, `/uploads/**`를 `permitAll()`로 추가.

4. **계층 간 순환 참조 우려**
   `DiaryService`가 카테고리 비공개 필터를 위해 `CategoryService`를 참조. 양방향이 되지 않도록 의존 방향을 **단방향**(Diary→Category)으로 정리해 `BeanCurrentlyInCreationException` 방지.

5. **회원가입 중복 시 DB 예외(500)**
   unique 제약으로 500이 노출됨. → 저장 전 `existsByUsername/Nickname`으로 **선검증** 후 사용자 친화 메시지 반환.

6. **부모 카테고리 글 수 미반영 / 비공개 상속**
   부모 폴더 글 수가 0으로 표시되고, 비공개 카테고리가 방문자에게 노출됨. → 글 개수를 **하위 누적 합산**으로, 비공개는 **자신+모든 하위 카테고리**를 방문자에게서 숨기도록 처리.

7. **캔버스 텍스트 편집 중 커서 이동 불가**
   편집 모드에서 클릭하면 선택 로직이 편집을 해제. → 편집 중에는 클릭을 커서 이동 전용으로 흘려보내도록 가드 추가.

---

## 6. 로컬 실행 방법

```bash
# 1) MariaDB 준비 (예: 포트 3307, DB명 daku)
#    CREATE DATABASE daku CHARACTER SET utf8mb4;

# 2) 환경변수(선택) — 미설정 시 기본값(localhost:3307 / root / 1234) 사용
#    DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, UPLOAD_DIR

# 3) 실행
./gradlew bootRun        # http://localhost:8080
```

## 7. GCP 배포 개요

1. **GCP VM(Compute Engine, Ubuntu)** 인스턴스 생성, 방화벽에서 80/443(및 임시 8080) 허용
2. VM에 **JDK 17 + MariaDB** 설치, DB/계정 생성
3. GitHub에서 소스 clone → `./gradlew bootJar` 로 빌드
4. 환경변수(`DB_PASSWORD` 등) 주입 후 `java -jar build/libs/*.jar` 실행 (systemd 서비스로 등록 권장)
5. **Cloudflare**에 도메인 연결, A 레코드를 VM 외부 IP로, SSL/TLS(Flexible/Full)로 **HTTPS** 적용

---

### 디렉터리
- 소스: `src/main/java/com/daku/diary`
- 템플릿: `src/main/resources/templates`
- 정적 리소스: `src/main/resources/static` (images, fonts)
