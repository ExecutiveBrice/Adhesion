package com.gestion.adhesion.services;

import com.gestion.adhesion.models.ParamBoolean;
import com.gestion.adhesion.models.ParamText;
import com.gestion.adhesion.repository.ParamBooleanRepository;
import com.gestion.adhesion.repository.ParamTextRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParamBooleanServices {

    @Autowired
    ParamTextRepository paramTextRepository;

    @Autowired
    ParamBooleanRepository paramBooleanRepository;

    public List<ParamBoolean> getAll (){
        List<ParamBoolean> params = paramBooleanRepository.findAll();
        return params;
    }

    public Boolean isClose (){
        List<ParamBoolean> params = paramBooleanRepository.findAll();
        return params.stream().anyMatch(paramBoolean -> "Maintenance".equals(paramBoolean.getParamName()) && paramBoolean.getParamValue() || "Ouvert".equals(paramBoolean.getParamName()) && !paramBoolean.getParamValue());
    }

    public ParamBoolean findByParamName(String paramName){
        return paramBooleanRepository.findByParamName(paramName).get();
    }

    public boolean findByParamValue(String paramName){
        return paramBooleanRepository.findByParamName(paramName).get().getParamValue();
    }
    public ParamBoolean save(ParamBoolean param){

        if(paramBooleanRepository.existsByParamName(param.getParamName())){
            ParamBoolean dbparam = findByParamName(param.getParamName());
            dbparam.setParamValue(param.getParamValue());
            return paramBooleanRepository.save(dbparam);
        }
        return paramBooleanRepository.save(param);
    }

    public void fillParamBoolean(){
        if(!paramBooleanRepository.existsByParamName("Maintenance")) {
            paramBooleanRepository.save(ParamBoolean.builder()
                    .paramName("Maintenance")
                    .paramValue(false).build());
        }
        if(!paramBooleanRepository.existsByParamName("Ouvert")){
            paramBooleanRepository.save(ParamBoolean.builder()
                    .paramName("Ouvert")
                    .paramValue(true).build());
        }
        if(!paramBooleanRepository.existsByParamName("Inscription")){
            paramBooleanRepository.save(ParamBoolean.builder()
                    .paramName("Inscription")
                    .paramValue(false).build());
        }
        if(!paramBooleanRepository.existsByParamName("Mail_Rappel")){
            paramBooleanRepository.save(ParamBoolean.builder()
                    .paramName("Mail_Rappel")
                    .paramValue(false).build());
        }
        if(!paramBooleanRepository.existsByParamName("Mail_Annulation")){
            paramBooleanRepository.save(ParamBoolean.builder()
                    .paramName("Mail_Annulation")
                    .paramValue(false).build());
        }

    }


}
