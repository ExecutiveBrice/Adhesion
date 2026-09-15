package com.wild.corp.adhesion.repository;



import com.wild.corp.adhesion.models.ConfirmationToken;
import com.wild.corp.adhesion.models.ConfirmationTokenType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;


@Repository
public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, Long> {

    Optional<ConfirmationToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ConfirmationToken token where token.tokenHash is null")
    int deleteLegacyTokensWithoutHash();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ConfirmationToken token
               set token.usedAt = :usedAt
             where token.tokenHash = :tokenHash
               and token.type = :type
               and token.usedAt is null
               and token.expiresAt > :now
            """)
    int consumeIfValid(@Param("tokenHash") String tokenHash,
                       @Param("type") ConfirmationTokenType type,
                       @Param("now") Instant now,
                       @Param("usedAt") Instant usedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ConfirmationToken token
               set token.usedAt = :usedAt
             where token.user.id = :userId
               and token.type = :type
               and token.usedAt is null
            """)
    int invalidateActiveForUser(@Param("userId") Long userId,
                                @Param("type") ConfirmationTokenType type,
                                @Param("usedAt") Instant usedAt);
}
