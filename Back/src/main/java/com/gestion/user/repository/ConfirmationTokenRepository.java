package com.gestion.user.repository;



import com.gestion.user.models.ConfirmationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, Long> {


    ConfirmationToken findByconfirmationToken(UUID token);
}