package com.gestion.auth.services;

import com.gestion.user.services.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {
  @Autowired
  UserServices userServices;

  @Autowired
  ConfirmationTokenService confirmationTokenService;
  @Override
  @Transactional
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return UserDetails.build(userServices.findByEmail(username));
  }


}
