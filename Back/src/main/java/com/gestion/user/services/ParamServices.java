package com.gestion.user.services;

import com.gestion.user.models.Param;
import com.gestion.user.repository.ParamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParamServices {

    @Autowired
    ParamRepository paramRepository;

    public List<Param> getAll (){
        List<Param> params = paramRepository.findAll();
        return params;
    }

    public Param findByParamName(String paramName){
        return paramRepository.findByParamName(paramName).get();
    }

    public Param save(Param param){

        if(paramRepository.existsByParamName(param.getParamName())){
            Param dbparam = findByParamName(param.getParamName());
            dbparam.setParamValue(param.getParamValue());
            return paramRepository.save(dbparam);
        }
        return paramRepository.save(param);
    }

    public void fillParam(){
        if(!paramRepository.existsByParamName("Maintenance")) {
            Param param = new Param();
            param.setParamName("Maintenance");
            param.setParamValue("False");
            paramRepository.save(param);
        }
        if(!paramRepository.existsByParamName("Ouvert")){
            Param param2 = new Param();
            param2.setParamName("Ouvert");
            param2.setParamValue("True");
            paramRepository.save(param2);
        }
        if(!paramRepository.existsByParamName("Inscription")){
            Param param2 = new Param();
            param2.setParamName("Inscription");
            param2.setParamValue("False");
            paramRepository.save(param2);
        }
        if(!paramRepository.existsByParamName("Fill_Activites")){
            Param param3 = new Param();
            param3.setParamName("Fill_Activites");
            param3.setParamValue("True");
            paramRepository.save(param3);
        }
        if(!paramRepository.existsByParamName("Fill_Adherents")) {
            Param param4 = new Param();
            param4.setParamName("Fill_Adherents");
            param4.setParamValue("True");
            paramRepository.save(param4);
        }
    }

}
