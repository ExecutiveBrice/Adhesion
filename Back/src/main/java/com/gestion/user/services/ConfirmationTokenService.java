package com.gestion.user.services;

import com.gestion.user.models.ConfirmationToken;
import com.gestion.user.repository.ConfirmationTokenRepository;
import com.gestion.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ConfirmationTokenService {

@Autowired
    private ConfirmationTokenRepository confirmationTokenRepository;

    @Autowired
    private UserRepository userRepository;

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