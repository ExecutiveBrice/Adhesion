package com.gestion.auth.controllers;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import com.gestion.auth.payload.request.SignupRequest;
import com.gestion.auth.payload.response.JwtResponse;
import com.gestion.auth.payload.response.MessageResponse;
import com.gestion.auth.security.jwt.JwtUtils;
import com.gestion.emails.services.EmailService;
import com.gestion.user.models.ConfirmationToken;
import com.gestion.user.services.ConfirmationTokenService;
import com.gestion.user.services.UserDetails;
import com.gestion.user.models.ERole;
import com.gestion.user.models.Role;
import com.gestion.user.repository.RoleRepository;
import com.gestion.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.gestion.user.models.User;
import com.gestion.auth.payload.request.LoginRequest;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
	@Autowired
	AuthenticationManager authenticationManager;
	@Autowired
	UserRepository userRepository;
	@Autowired
	EmailService emailService;

	@Autowired
	ConfirmationTokenService confirmationTokenService;

	@Autowired
	RoleRepository roleRepository;
	@Autowired
	PasswordEncoder encoder;

	@Autowired
	JwtUtils jwtUtils;

	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);
		
		UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		List<String> roles = userDetails.getAuthorities().stream()
				.map(item -> item.getAuthority())
				.collect(Collectors.toList());

		return ResponseEntity.ok(new JwtResponse(jwt,
												 userDetails.getId(),
												 userDetails.getUsername(),
												 userDetails.isEnabled(),
												 roles));
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Error: Email is already taken!"));
		}


		// Create new user's account
		User user = new User(signUpRequest.getUsername(),
				encoder.encode(signUpRequest.getPassword()));
		user.setEnabled(false);

		Set<Role> roles = new HashSet<>();
		user.setRoles(roles);
		Role userRole = roleRepository.findByName(ERole.ROLE_USER)
				.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
		roles.add(userRole);
		user.getRoles().add(userRole);

		userRepository.save(user);

		ConfirmationToken cft = new ConfirmationToken();
		cft.setUser(user);
		cft.setCreatedDate(LocalDate.now());
		UUID uuid = UUID.randomUUID();
		cft.setConfirmationToken(uuid);

		confirmationTokenService.saveConfirmationToken(cft);

		SimpleMailMessage mess = new SimpleMailMessage();
		mess.setTo(user.getUsername());
		mess.setSubject("Confirmation");
		mess.setText("http://localhost:8002/api/test/confirmEmail/"+uuid);
		emailService.sendEmail(mess);

		return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
	}



}
