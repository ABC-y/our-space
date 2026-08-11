package com.belongus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "couple_spaces")
public class CoupleSpace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 40)
    private String memberOneName;

    @Column(nullable = false, length = 40)
    private String memberTwoName;

    @Column(nullable = false, unique = true, length = 16)
    private String inviteCode;

    @Column(nullable = false)
    private LocalDate relationshipStartedOn;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected CoupleSpace() {
    }

    public CoupleSpace(String name, String memberOneName, LocalDate relationshipStartedOn, String inviteCode) {
        this.name = name;
        this.memberOneName = memberOneName;
        this.memberTwoName = "等待加入";
        this.relationshipStartedOn = relationshipStartedOn;
        this.inviteCode = inviteCode;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMemberOneName() {
        return memberOneName;
    }

    public String getMemberTwoName() {
        return memberTwoName;
    }

    public void setMemberTwoName(String memberTwoName) {
        this.memberTwoName = memberTwoName;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDate getRelationshipStartedOn() {
        return relationshipStartedOn;
    }

    public void setRelationshipStartedOn(LocalDate relationshipStartedOn) {
        this.relationshipStartedOn = relationshipStartedOn;
    }
}
