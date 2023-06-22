package com.gestion.adhesion.controllers;

import com.gestion.adhesion.models.UserDetails;
import com.gestion.adhesion.services.UserDetailsService;
import com.gestion.adhesion.services.UserServices;
import com.gestion.security.jwt.JwtUtils;
import com.gestion.security.payload.request.LoginRequest;
import com.gestion.security.payload.request.SignupRequest;
import com.gestion.security.payload.response.JwtResponse;
import com.gestion.security.payload.response.MessageResponse;
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
		loginRequest.setUsername(loginRequest.getUsername().toLowerCase());
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
				userDetails.getUsername().toLowerCase(),
				userDetails.isEnabled(),
				roles));
	}


	@PostMapping("/signupAnonymous")
	public ResponseEntity<?> signupAnonymous(@RequestBody SignupRequest signUpRequest) {
		signUpRequest.setUsername(signUpRequest.getUsername().toLowerCase());
		if (userServices.existsByEmail(signUpRequest.getUsername())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Erreur: cet e-mail est déjà utilisé"));
		}
		userServices.createNewUserAnonymous(signUpRequest.getUsername());
		return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
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
