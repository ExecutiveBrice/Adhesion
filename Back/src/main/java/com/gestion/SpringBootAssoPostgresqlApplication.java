package com.gestion;

import com.gestion.user.models.ERole;
import com.gestion.user.models.Param;
import com.gestion.user.models.Role;
import com.gestion.user.repository.RoleRepository;
import com.gestion.user.services.ActiviteServices;
import com.gestion.user.services.ParamServices;
import com.gestion.user.services.UserServices;
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
	ParamServices paramServices;

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
		if(!roleRepository.findByName(ERole.ROLE_BUREAU).isPresent()){
			roleRepository.save(new Role(ERole.ROLE_BUREAU));
		}
		if(!roleRepository.findByName(ERole.ROLE_ADMINISTRATEUR).isPresent()){
			roleRepository.save(new Role(ERole.ROLE_ADMINISTRATEUR));
		}
		if(!roleRepository.findByName(ERole.ROLE_SECRETAIRE).isPresent()){
			roleRepository.save(new Role(ERole.ROLE_SECRETAIRE));
		}
		paramServices.fillParam();
		Param fill_Activites=paramServices.findByParamName("Fill_Activites");
		if(Boolean.parseBoolean(fill_Activites.getParamValue())){
			activiteServices.fillActivites();
			fill_Activites.setParamValue("false");
			paramServices.save(fill_Activites);
		}

		Param fill_Adherents=paramServices.findByParamName("Fill_Adherents");
		if(Boolean.parseBoolean(fill_Adherents.getParamValue())){
			userServices.fillUsers();
			fill_Adherents.setParamValue("false");
			paramServices.save(fill_Adherents);
		}



	}
}


/**
 *
 if(!userServices.existsByEmail("adherent@mail.com")){
 User user = userServices.createNewUser("adherent@mail.com", encoder.encode("adherent"));
 Adherent adherent = user.getTribu().getAdherents().stream().findFirst().get();
 adherent.setNom("AMIDALA");
 adherent.setPrenom("Leya");
 adherent.setNaissance(LocalDate.of(1960,10,17));
 adherent.setLieuNaissance("Naboo");
 adherent.setTelephone("0695768168");
 adherent.setAdresse("Lieu dit région des lacs");
 adherentRepository.save(adherent);
 }

 if(!userServices.existsByEmail("secretaire@mail.com")){
 User user = userServices.createNewUser("secretaire@mail.com", encoder.encode("secretaire"));
 Adherent adherent = user.getTribu().getAdherents().stream().findFirst().get();
 adherent.setNom("SKYWALKER");
 adherent.setPrenom("Luke");
 adherent.setNaissance(LocalDate.of(1972,2,28));
 adherent.setLieuNaissance("Inconnu");
 adherent.setTelephone("0695768168");
 adherent.setAdresse("bordure exterieure");
 adherentRepository.save(adherent);
 userServices.grantUser(ERole.ROLE_SECRETAIRE, user);
 }

 if(!userServices.existsByEmail("administrateur@mail.com")){
 User user = userServices.createNewUser("administrateur@mail.com", encoder.encode("administrateur"));
 Adherent adherent = user.getTribu().getAdherents().stream().findFirst().get();
 adherent.setNom("VADOR");
 adherent.setPrenom("Dark");
 adherent.setNaissance(LocalDate.of(1981,5,12));
 adherent.setLieuNaissance("Tatouine");
 adherent.setTelephone("2");
 adherent.setAdresse("Etoile noire");
 adherentRepository.save(adherent);
 userServices.grantUser(ERole.ROLE_ADMINISTRATEUR, user);
 }

 if(!userServices.existsByEmail("bureau@mail.com")){
 User user = userServices.createNewUser("bureau@mail.com", encoder.encode("bureau"));
 Adherent adherent = user.getTribu().getAdherents().stream().findFirst().get();
 adherent.setNom("PALPATINE");
 adherent.setPrenom("Empereur");
 adherent.setNaissance(LocalDate.of(1910,12,24));
 adherent.setLieuNaissance("Coruscant");
 adherent.setTelephone("6666666666");
 adherent.setAdresse("BP1 Sénat impérial");
 adherentRepository.save(adherent);
 userServices.grantUser(ERole.ROLE_BUREAU, user);
 }

 if(!userServices.existsByEmail("siteadmin@mail.com")){
 User user = userServices.createNewUser("siteadmin@mail.com", encoder.encode("siteadmin"));
 Adherent adherent = user.getTribu().getAdherents().stream().findFirst().get();
 adherent.setNom("YODA");
 adherent.setPrenom("Maitre");
 adherent.setNaissance(LocalDate.of(999,6,14));
 adherent.setLieuNaissance("Dagobah");
 adherent.setTelephone("");
 adherent.setAdresse("Secteur Sluis Systeme Prime");
 adherentRepository.save(adherent);
 userServices.grantUser(ERole.ROLE_ADMIN, user);
 }

 */