package com.belongus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "space_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_space_member", columnNames = {"space_id", "user_id"}),
        indexes = {
                @Index(name = "idx_space_members_user_joined", columnList = "user_id,joined_at"),
                @Index(name = "idx_space_members_space_joined", columnList = "space_id,joined_at")
        }
)
public class SpaceMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id")
    private CoupleSpace space;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    protected SpaceMember() {
    }

    public SpaceMember(CoupleSpace space, AppUser user) {
        this.space = space;
        this.user = user;
    }

    public CoupleSpace getSpace() {
        return space;
    }

    public AppUser getUser() {
        return user;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}
