package com.wild.corp.adhesion.repository;



import com.wild.corp.adhesion.models.ConfirmationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, Long> {


    ConfirmationToken findByconfirmationToken(UUID token);
}