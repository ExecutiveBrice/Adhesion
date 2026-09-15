package com.wild.corp;

import com.wild.corp.adhesion.services.ParamBooleanServices;
import com.wild.corp.adhesion.services.ParamNumberServices;
import com.wild.corp.adhesion.services.ParamTextServices;
import com.wild.corp.adhesion.services.UserServices;
import com.wild.corp.adhesion.services.RoleTableMigration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication
@EnableFeignClients(basePackages = "com.wild.corp.adhesion.config")
public class AdhesionWebApp {

	@Autowired
	UserServices userServices;
	@Autowired
	RoleTableMigration roleTableMigration;

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
		roleTableMigration.migrate();

//		if(!userServices.existsByEmail("admin")){
//			userServices.initAdmin();
//		}

		paramBooleanServices.fillParamBoolean();
		paramTextServices.fillParamText();
		paramNumberServices.fillParamNumber();
	}
}
