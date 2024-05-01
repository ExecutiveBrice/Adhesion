package com.wild.corp;

import com.wild.corp.adhesion.models.ERole;
import com.wild.corp.adhesion.models.Role;
import com.wild.corp.adhesion.repository.RoleRepository;

import com.wild.corp.adhesion.services.ParamBooleanServices;
import com.wild.corp.adhesion.services.ParamNumberServices;
import com.wild.corp.adhesion.services.ParamTextServices;
import com.wild.corp.adhesion.services.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;


@SpringBootApplication
public class AdhesionWebApp {

	@Autowired
	UserServices userServices;
	@Autowired
	RoleRepository roleRepository;

	@Autowired
	ParamBooleanServices paramBooleanServices;

	@Autowired
	ParamTextServices paramTextServices;

	@Autowired
	ParamNumberServices paramNumberServices;

	public static void main(String[] args) {
		SpringApplication.run(AdhesionWebApp.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
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

//		if(!userServices.existsByEmail("admin")){
//			userServices.initAdmin();
//		}

		paramBooleanServices.fillParamBoolean();
		paramTextServices.fillParamText();
		paramNumberServices.fillParamNumber();
	}
}

