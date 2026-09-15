package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.PwaSessionToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PwaSessionTokenRepository extends JpaRepository<PwaSessionToken, Long> {
    Optional<PwaSessionToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PwaSessionToken token set token.usedAt = :now
             where token.tokenHash = :tokenHash and token.usedAt is null and token.expiresAt > :now
            """)
    int consumeIfValid(@Param("tokenHash") String tokenHash, @Param("now") Instant now);
}
