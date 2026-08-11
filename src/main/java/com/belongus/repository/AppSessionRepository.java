package com.belongus.repository;

import com.belongus.domain.AppSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppSessionRepository extends JpaRepository<AppSession, Long> {
    Optional<AppSession> findByTokenHash(String tokenHash);

    void deleteByTokenHash(String tokenHash);
}
