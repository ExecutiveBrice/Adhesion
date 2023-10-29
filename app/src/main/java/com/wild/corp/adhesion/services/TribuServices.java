package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Tribu;
import com.wild.corp.adhesion.repository.TribuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TribuServices {

    @Autowired
    TribuRepository tribuRepository;

    @Autowired
    UserServices userServices;

    public Tribu save(Tribu tribu) {
        Tribu newtribu = tribuRepository.save(tribu);
        return newtribu;
    }

    public Tribu getTribuById(Long tribuId) {
        Tribu newtribu = tribuRepository.findById(tribuId).get();
        return newtribu;
    }

    public List<Tribu> getAll() {
        return tribuRepository.findAll();
    }

    public Tribu getConnetedTribu(String email) {
        return userServices.findByEmail(email).getAdherent().getTribu();
    }

    public Tribu getTribuByUuid(UUID uuid) {
        return tribuRepository.findByUuid(uuid);
    }

    public void fillTribuµUuid() {
        List<Tribu> tribus = tribuRepository.findAll();
        tribus.forEach(tribu -> tribu.setUuid(UUID.randomUUID()));
        tribuRepository.saveAll(tribus);
    }

}
