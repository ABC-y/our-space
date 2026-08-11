package com.belongus.repository;

import com.belongus.domain.SpaceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceMemberRepository extends JpaRepository<SpaceMember, Long> {
    boolean existsBySpaceIdAndUserId(Long spaceId, Long userId);

    long countBySpaceId(Long spaceId);

    List<SpaceMember> findBySpaceIdOrderByJoinedAtAsc(Long spaceId);

    List<SpaceMember> findByUserIdOrderByJoinedAtDesc(Long userId);
}
