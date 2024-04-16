package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.ActiviteNm1;
import com.wild.corp.adhesion.models.Tribu;
import com.wild.corp.adhesion.repository.ActiviteNm1Repository;
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
    ActiviteNm1Repository activiteNm1Repository;
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

    public Tribu addActivitesNm1(UUID uuid, List<ActiviteNm1> activitesNm1) {
        Tribu tribu = tribuRepository.findByUuid(uuid);
        activitesNm1.forEach(activiteNm1 -> activiteNm1Repository.save(activiteNm1));
        tribu.setAutorisations(activitesNm1);
        save(tribu);
        return tribu;
    }


}
