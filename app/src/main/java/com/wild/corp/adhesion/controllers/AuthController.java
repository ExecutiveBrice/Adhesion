package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.models.UserDetails;
import com.wild.corp.adhesion.services.UserDetailsService;
import com.wild.corp.adhesion.services.UserServices;
import com.wild.corp.adhesion.security.jwt.JwtUtils;
import com.wild.corp.adhesion.security.payload.request.LoginRequest;
import com.wild.corp.adhesion.security.payload.request.SignupRequest;
import com.wild.corp.adhesion.security.payload.response.JwtResponse;
import com.wild.corp.adhesion.security.payload.response.MessageResponse;
import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth")
public class AuthController {
	@Autowired
	AuthenticationManager authenticationManager;
	@Autowired
	UserServices userServices;
	@Autowired
	JwtUtils jwtUtils;
	@Autowired
	PasswordEncoder encoder;

	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername().toLowerCase(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);

		UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		List<String> roles = userDetails.getAuthorities().stream()
				.map(item -> item.getAuthority())
				.collect(Collectors.toList());

		return ResponseEntity.ok(new JwtResponse(jwt,
				userDetails.getId(),
				userDetails.getUsername().toLowerCase(),
				userDetails.isEnabled(),
				roles));
	}

	@PostMapping("/reinitPassword")
	public ResponseEntity<?> reinitPassword(@RequestBody SignupRequest signUpRequest) {
		signUpRequest.setUsername(signUpRequest.getUsername().toLowerCase());
		return ResponseEntity.ok(userServices.reinitPassword(signUpRequest.getUsername()));
	}

	@PostMapping("/userExist")
	public ResponseEntity<?> userExist(@RequestBody SignupRequest signUpRequest) {
		signUpRequest.setUsername(signUpRequest.getUsername().toLowerCase());
		userServices.isUserExist(signUpRequest.getUsername());
		return ResponseEntity.ok("ok");
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		signUpRequest.setUsername(signUpRequest.getUsername().toLowerCase());
		if (userServices.existsByEmail(signUpRequest.getUsername())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Erreur: cet e-mail est déjà utilisé"));
		}
		userServices.createNewUser(signUpRequest.getUsername(),
				encoder.encode(signUpRequest.getPassword()));
		return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
	}

	@PostMapping("/signupAnonymous")
	public ResponseEntity<?> signupAnonymous(@PathParam("email") String email) {
		if (userServices.existsByEmail(email.toLowerCase())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Erreur: cet e-mail est déjà utilisé"));
		}
		Adherent uuid = userServices.createUserAnonymous(email.toLowerCase());
		return ResponseEntity.ok(uuid);
	}


	@GetMapping("/confirmEmail/{confirmationToken}")
	public ResponseEntity<?> confirmEmail(@PathVariable String confirmationToken) throws Exception {
		userServices.confirmEmailAnswer(confirmationToken);
		return ResponseEntity.ok("Votre e-mail à bien été confirmée");
	}



	@PostMapping("/changePassword")
	public ResponseEntity<?> changePassword(@PathParam("token") String token, @RequestBody SignupRequest signUpRequest) {


		userServices.changePassword(token, signUpRequest.getPassword());
		return ResponseEntity.ok("Réinitialisation du mot de passe réussie");


	}
}
