package com.belongus.repository;

import com.belongus.domain.Memory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemoryRepository extends JpaRepository<Memory, Long> {
    List<Memory> findBySpaceIdOrderByOccurredOnDescCreatedAtDesc(Long spaceId);

    Optional<Memory> findFirstByImageUrl(String imageUrl);

    boolean existsByImageUrl(String imageUrl);
}
