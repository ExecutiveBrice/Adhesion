package com.gestion;

import com.gestion.user.models.ERole;
import com.gestion.user.models.Role;
import com.gestion.user.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class SpringBootAssoPostgresqlApplication {

	@Autowired
	RoleRepository roleRepository;

	public static void main(String[] args) {
		SpringApplication.run(SpringBootAssoPostgresqlApplication.class, args);
	}


	@PostConstruct
	private void init() {


		if(!roleRepository.findByName(ERole.ROLE_USER).isPresent()){
			roleRepository.save(new Role(ERole.ROLE_USER));
		}
		if(!roleRepository.findByName(ERole.ROLE_ADMIN).isPresent()){
			roleRepository.save(new Role(ERole.ROLE_ADMIN));
		}
		if(!roleRepository.findByName(ERole.ROLE_MODERATOR).isPresent()){
			roleRepository.save(new Role(ERole.ROLE_MODERATOR));
		}

	}
}
