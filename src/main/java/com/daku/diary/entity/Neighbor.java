package com.daku.diary.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "neighbors")
public class Neighbor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "requester_id")
    private User requester;   // 신청한 사람

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;    // 신청 받은 사람

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NeighborStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }
    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }
    public NeighborStatus getStatus() { return status; }
    public void setStatus(NeighborStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}