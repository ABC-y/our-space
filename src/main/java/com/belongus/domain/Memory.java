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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "memories", indexes = {
        @Index(name = "idx_memories_space_occurred", columnList = "space_id,occurred_on,created_at"),
        @Index(name = "idx_memories_image_url", columnList = "image_url")
})
public class Memory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id")
    private CoupleSpace space;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1200)
    private String content;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false, length = 40)
    private String authorName;

    @Column(nullable = false)
    private LocalDate occurredOn;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Memory() {
    }

    public Memory(CoupleSpace space, String title, String content, String imageUrl, String authorName, LocalDate occurredOn) {
        this.space = space;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.authorName = authorName;
        this.occurredOn = occurredOn;
    }

    public Long getId() {
        return id;
    }

    public CoupleSpace getSpace() {
        return space;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getAuthorName() {
        return authorName;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void update(String title, String content, String imageUrl, LocalDate occurredOn) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.occurredOn = occurredOn;
    }
}
