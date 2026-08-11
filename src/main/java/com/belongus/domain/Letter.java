package com.belongus.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "letters")
public class Letter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id")
    private CoupleSpace space;

    @Column(nullable = false, length = 40)
    private String senderName;

    @Column(nullable = false, length = 40)
    private String recipientName;

    @Column(nullable = false, length = 1600)
    private String content;

    @Column(nullable = false, length = 20)
    private String status = "SENT";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "letter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LetterReply> replies = new ArrayList<>();

    protected Letter() {
    }

    public Letter(CoupleSpace space, String senderName, String recipientName, String content) {
        this.space = space;
        this.senderName = senderName;
        this.recipientName = recipientName;
        this.content = content;
    }

    public void addReply(LetterReply reply) {
        replies.add(reply);
        status = "REPLIED";
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public LetterReply findReply(Long replyId) {
        return replies.stream()
                .filter(reply -> reply.getId().equals(replyId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("这条回应已经不在这里了"));
    }

    public void removeReply(LetterReply reply) {
        replies.remove(reply);
        if (replies.isEmpty()) {
            status = "SENT";
        }
    }

    public Long getId() {
        return id;
    }

    public CoupleSpace getSpace() {
        return space;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getContent() {
        return content;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<LetterReply> getReplies() {
        return replies;
    }
}
