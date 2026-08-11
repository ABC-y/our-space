package com.belongus.repository;

import com.belongus.domain.Letter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LetterRepository extends JpaRepository<Letter, Long> {
    List<Letter> findBySpaceIdOrderByCreatedAtDesc(Long spaceId);
}
