package com.belongus.repository;

import com.belongus.domain.CoupleSpace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoupleSpaceRepository extends JpaRepository<CoupleSpace, Long> {
    Optional<CoupleSpace> findByInviteCode(String inviteCode);
}
