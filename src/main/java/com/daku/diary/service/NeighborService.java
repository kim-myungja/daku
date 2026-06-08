package com.daku.diary.service;

import com.daku.diary.entity.Follow;
import com.daku.diary.entity.Neighbor;
import com.daku.diary.entity.NeighborStatus;
import com.daku.diary.entity.User;
import com.daku.diary.repository.FollowRepository;
import com.daku.diary.repository.NeighborRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NeighborService {

    private final NeighborRepository neighborRepository;
    private final FollowRepository followRepository;
    private final UserService userService;

    // 서로이웃 신청 보내기
    public void sendRequest(User from, Long toUserId) {
        User to = userService.findById(toUserId);
        if (from.getId().equals(to.getId())) return; // 자기 자신에겐 신청 불가
        // 이미 관계(어느 방향이든)가 있으면 중복 신청 방지
        if (neighborRepository.findByRequesterAndReceiver(from, to).isPresent()) return;
        if (neighborRepository.findByRequesterAndReceiver(to, from).isPresent()) return;

        Neighbor n = new Neighbor();
        n.setRequester(from);
        n.setReceiver(to);
        n.setStatus(NeighborStatus.PENDING);
        neighborRepository.save(n);
    }

    // 받은 신청 수락 (받은 사람만 가능)
    public void acceptRequest(Long neighborId, User currentUser) {
        Neighbor n = neighborRepository.findById(neighborId).orElseThrow();
        if (!n.getReceiver().getId().equals(currentUser.getId())) return;
        n.setStatus(NeighborStatus.ACCEPTED);
        neighborRepository.save(n);
    }

    // 받은 신청 거절 (받은 사람만 가능) — 신청 삭제
    public void rejectRequest(Long neighborId, User currentUser) {
        Neighbor n = neighborRepository.findById(neighborId).orElse(null);
        if (n == null || !n.getReceiver().getId().equals(currentUser.getId())) return;
        neighborRepository.delete(n);
    }

    // 내가 받은 대기중 신청 목록
    public List<Neighbor> getReceivedPending(User user) {
        return neighborRepository.findByReceiverAndStatus(user, NeighborStatus.PENDING);
    }

    // 내 서로이웃 목록 (수락된 관계의 상대방들)
    public List<User> getMyNeighbors(User user) {
        List<User> result = new ArrayList<>();
        for (Neighbor n : neighborRepository.findByRequesterAndStatus(user, NeighborStatus.ACCEPTED)) {
            result.add(n.getReceiver());
        }
        for (Neighbor n : neighborRepository.findByReceiverAndStatus(user, NeighborStatus.ACCEPTED)) {
            result.add(n.getRequester());
        }
        return result;
    }

    // 두 사람이 서로이웃인지 판단 (양방향 확인)
    public boolean isNeighbor(User a, User b) {
        return neighborRepository.existsByRequesterAndReceiverAndStatus(a, b, NeighborStatus.ACCEPTED)
                || neighborRepository.existsByRequesterAndReceiverAndStatus(b, a, NeighborStatus.ACCEPTED);
    }

    // 화면 버튼 표시용: NONE(관계없음) / PENDING(신청중) / ACCEPTED(서로이웃)
    public String relationStatus(User me, User other) {
        Optional<Neighbor> a = neighborRepository.findByRequesterAndReceiver(me, other);
        if (a.isPresent()) return a.get().getStatus().name();
        Optional<Neighbor> b = neighborRepository.findByRequesterAndReceiver(other, me);
        if (b.isPresent()) return b.get().getStatus().name();
        return "NONE";
    }

    // ----- 단방향 이웃(팔로우) -----

    // 이웃 추가 (팔로우)
    public void follow(User me, Long targetId) {
        User target = userService.findById(targetId);
        if (me.getId().equals(target.getId())) return; // 자기 자신은 불가
        if (!followRepository.existsByFollowerAndFollowing(me, target)) {
            Follow f = new Follow();
            f.setFollower(me);
            f.setFollowing(target);
            followRepository.save(f);
        }
    }

    // 이웃 취소 (언팔로우)
    public void unfollow(User me, Long targetId) {
        User target = userService.findById(targetId);
        followRepository.findByFollowerAndFollowing(me, target)
                .ifPresent(followRepository::delete);
    }

    // 내가 이웃으로 추가했는지
    public boolean isFollowing(User me, User other) {
        return followRepository.existsByFollowerAndFollowing(me, other);
    }

    // 내 이웃 목록 (내가 추가한 사람들)
    public List<User> getFollowing(User user) {
        List<User> result = new ArrayList<>();
        for (Follow f : followRepository.findByFollower(user)) {
            result.add(f.getFollowing());
        }
        return result;
    }
}