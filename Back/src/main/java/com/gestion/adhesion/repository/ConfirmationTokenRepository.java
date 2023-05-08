package com.gestion.adhesion.repository;



import com.gestion.adhesion.models.ConfirmationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, Long> {


    ConfirmationToken findByconfirmationToken(UUID token);
}