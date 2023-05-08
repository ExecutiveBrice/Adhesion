package com.gestion.adhesion.services;

import com.gestion.adhesion.models.ParamBoolean;
import com.gestion.adhesion.models.ParamNumber;
import com.gestion.adhesion.repository.ParamBooleanRepository;
import com.gestion.adhesion.repository.ParamNumberRepository;
import com.gestion.adhesion.repository.ParamTextRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParamNumberServices {

    @Autowired
    ParamNumberRepository paramNumberRepository;

    public List<ParamNumber> getAll (){
        List<ParamNumber> params = paramNumberRepository.findAll();
        return params;
    }


    public ParamNumber findByParamName(String paramName){
        return paramNumberRepository.findByParamName(paramName).get();
    }

    public Integer findByParamValue(String paramName){
        return paramNumberRepository.findByParamName(paramName).get().getParamValue();
    }
    public ParamNumber save(ParamNumber param){

        if(paramNumberRepository.existsByParamName(param.getParamName())){
            ParamNumber dbparam = findByParamName(param.getParamName());
            dbparam.setParamValue(param.getParamValue());
            return paramNumberRepository.save(dbparam);
        }
        return paramNumberRepository.save(param);
    }

    public void fillParamNumber(){
        if(!paramNumberRepository.existsByParamName("Jours_Avant_Rappel")) {
            paramNumberRepository.save(ParamNumber.builder()
                    .paramName("Jours_Avant_Rappel")
                    .paramValue(2).build());
        }
        if(!paramNumberRepository.existsByParamName("Jours_Avant_Annulation")) {
            paramNumberRepository.save(ParamNumber.builder()
                    .paramName("Jours_Avant_Annulation")
                    .paramValue(5).build());
        }

    }


}
