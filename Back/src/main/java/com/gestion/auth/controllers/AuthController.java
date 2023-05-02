package com.gestion.auth.controllers;

import com.gestion.auth.payload.request.LoginRequest;
import com.gestion.auth.payload.request.SignupRequest;
import com.gestion.auth.payload.response.JwtResponse;
import com.gestion.auth.payload.response.MessageResponse;
import com.gestion.auth.security.jwt.JwtUtils;
import com.gestion.auth.services.UserDetails;
import com.gestion.auth.services.UserDetailsService;
import com.gestion.user.models.User;
import com.gestion.user.services.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.websocket.server.PathParam;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api_adhesion/auth")
public class AuthController {
	@Autowired
	AuthenticationManager authenticationManager;
	@Autowired
	UserServices userServices;


	@Autowired
	JwtUtils jwtUtils;

	@Autowired
	UserDetailsService userDetailsService;
	@Autowired
	PasswordEncoder encoder;
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
		if (userServices.existsByEmail(signUpRequest.getUsername())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Erreur: cet e-mail est déjà utilisé"));
		}
		User user = userServices.createNewUser(signUpRequest.getUsername(),
				encoder.encode(signUpRequest.getPassword()));
		return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
	}

	@GetMapping("/confirmEmail/{confirmationToken}")
	public ResponseEntity<?> confirmEmail(@PathVariable String confirmationToken) throws Exception {
		userServices.confirmEmailAnswer(confirmationToken);
		return ResponseEntity.ok("Votre e-mail à bien été confirmée");
	}

	@PostMapping("/reinitPassword")
	public ResponseEntity<?> reinitPassword(@RequestBody SignupRequest signUpRequest) {

		return ResponseEntity.ok(userServices.reinitPassword(signUpRequest.getUsername()));
	}

	@PostMapping("/changePassword")
	public ResponseEntity<?> changePassword(@PathParam("token") String token, @RequestBody SignupRequest signUpRequest) {


			userServices.changePassword(token, signUpRequest.getPassword());
			return ResponseEntity.ok("Réinitialisation du mot de passe réussie");


	}
}
