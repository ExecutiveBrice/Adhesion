package com.gestion.user.services;

import com.gestion.user.models.Tribu;
import com.gestion.user.repository.TribuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TribuServices {


    @Autowired
    TribuRepository familleRepository;

            public Tribu save(Tribu tribu){
                Tribu newtribu = familleRepository.save(tribu);
                return  newtribu;
            }

    public Tribu getTribuById(Long tribuId){
        Tribu newtribu = familleRepository.findById(tribuId).get();
        return  newtribu;
    }

            public List<Tribu> getAll(){
                return familleRepository.findAll();
            }

}
