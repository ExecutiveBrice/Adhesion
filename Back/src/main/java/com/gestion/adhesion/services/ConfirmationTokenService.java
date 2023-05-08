package com.gestion.adhesion.services;

import com.gestion.adhesion.models.ConfirmationToken;
import com.gestion.adhesion.repository.ConfirmationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ConfirmationTokenService {

    @Autowired
    private ConfirmationTokenRepository confirmationTokenRepository;

    public void saveConfirmationToken(ConfirmationToken confirmationToken) {
        confirmationTokenRepository.save(confirmationToken);
    }


    public void deleteConfirmationToken(Long id){
        confirmationTokenRepository.deleteById(id);
    }

    public ConfirmationToken findByToken(String token){
        return confirmationTokenRepository.findByconfirmationToken(UUID.fromString(token));
    }


}