package com.gestion;

import com.gestion.adhesion.models.ERole;
import com.gestion.adhesion.models.Role;
import com.gestion.adhesion.repository.RoleRepository;
import com.gestion.adhesion.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class SpringBootAssoPostgresqlApplication {

	@Autowired
	RoleRepository roleRepository;
	@Autowired
	UserServices userServices;
	@Autowired
	ActiviteServices activiteServices;

	@Autowired
	ParamBooleanServices paramBooleanServices;

	@Autowired
	ParamTextServices paramTextServices;

	@Autowired
	ParamNumberServices paramNumberServices;

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
		if(!roleRepository.findByName(ERole.ROLE_PROF).isPresent()){
			roleRepository.save(new Role(ERole.ROLE_PROF));
		}
		if(!roleRepository.findByName(ERole.ROLE_COMPTABLE).isPresent()){
			roleRepository.save(new Role(ERole.ROLE_COMPTABLE));
		}
		if(!roleRepository.findByName(ERole.ROLE_BUREAU).isPresent()){
			roleRepository.save(new Role(ERole.ROLE_BUREAU));
		}
		if(!roleRepository.findByName(ERole.ROLE_ADMINISTRATEUR).isPresent()){
			roleRepository.save(new Role(ERole.ROLE_ADMINISTRATEUR));
		}
		if(!roleRepository.findByName(ERole.ROLE_SECRETAIRE).isPresent()){
			roleRepository.save(new Role(ERole.ROLE_SECRETAIRE));
		}

		//activiteServices.fillActivites();
		//userServices.fillUsers();

		paramBooleanServices.fillParamBoolean();
		paramTextServices.fillParamText();
		paramNumberServices.fillParamNumber();
	}
}

