package com.daku.diary.repository;

import com.daku.diary.entity.Neighbor;
import com.daku.diary.entity.NeighborStatus;
import com.daku.diary.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NeighborRepository extends JpaRepository<Neighbor, Long> {

    boolean existsByRequesterAndReceiverAndStatus(User requester, User receiver, NeighborStatus status);

    Optional<Neighbor> findByRequesterAndReceiver(User requester, User receiver);

    List<Neighbor> findByReceiverAndStatus(User receiver, NeighborStatus status);

    List<Neighbor> findByRequesterAndStatus(User requester, NeighborStatus status);
}