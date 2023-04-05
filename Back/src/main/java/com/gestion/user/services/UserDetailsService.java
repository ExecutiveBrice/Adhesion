package com.gestion.user.services;

import com.gestion.user.models.ConfirmationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestion.user.models.User;
import com.gestion.user.repository.UserRepository;

@Service
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {
  @Autowired
  UserRepository userRepository;

  @Autowired
  ConfirmationTokenService confirmationTokenService;
  @Override
  @Transactional
  public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

    return UserDetails.build(user);
  }


  public void confirmUser(String token) {
    ConfirmationToken confirmationToken= confirmationTokenService.findByToken(token);
    final User user = confirmationToken.getUser();
    user.setEnabled(true);
    userRepository.save(user);
    confirmationTokenService.deleteConfirmationToken(confirmationToken.getId());

  }
}
