package com.gestion.user.services;

import com.gestion.user.models.*;
import com.gestion.user.repository.AdherentRepository;
import com.gestion.user.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdherentServices {

    @Autowired
    AdherentRepository adherentRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    ActiviteServices activiteServices;
    @Autowired
    UserServices userServices;
    @Autowired
    TribuServices tribuServices;

    public List<String> findByGroup(String groupe){
        List<String> listDiffusion = null;
        if("tous".equals(groupe)){
            return adherentRepository.findAll().stream().map(Adherent::getEmail)
                    .collect(Collectors.toList());
        }

        if("bureau".equals(groupe)){
            return userServices.findAll().stream()
                    .filter(user -> user.getRoles().contains(roleRepository.findByName(ERole.ROLE_BUREAU)))
                    .map(User::getUsername)
                    .collect(Collectors.toList());
        }

        if("administrateurs".equals(groupe)){
            return userServices.findAll().stream()
                    .filter(user -> user.getRoles().contains(roleRepository.findByName(ERole.ROLE_ADMINISTRATEUR)))
                    .map(User::getUsername)
                    .collect(Collectors.toList());
        }

            return adherentRepository.findAll().stream()
                    .filter(adherent -> adherent.getAdhesions().stream().allMatch(adhesion -> adhesion.getActivite().equals(activiteServices.findByNom(groupe))))
                    .map(Adherent::getEmail)
                    .collect(Collectors.toList());

    }

    public Adherent changeTribu( Long referentId, Long adherentId) {
        Adherent adherentReferent = getById(referentId);
        Adherent adherentCible = getById(adherentId);

        if(adherentCible.isReferent()){
            adherentCible.setReferent(false);
            adherentCible.getTribu().getAdherents().stream().forEach(adherent -> adherent.setTribu(adherentReferent.getTribu()));
        }else{
            adherentCible.setTribu(adherentReferent.getTribu());
        }

        return adherentRepository.save(adherentCible);

    }

    public Adherent getById(Long id){
        return adherentRepository.findById(id).get();
    }

    public Adherent updateEmail(String email, Long adherentId){
        Adherent indbAdherent = adherentRepository.findById(adherentId).get();
        if(indbAdherent.isReferent()){
            userServices.updateUsername(email.toLowerCase(), indbAdherent.getEmail().toLowerCase());
        }
        indbAdherent.setEmail(email.toLowerCase());
        return adherentRepository.save(indbAdherent);
    }

    public Adherent update(Adherent adherent){
         if (adherent.getId() != null && adherent.getId() > 0) {
            Adherent indbAdherent = adherentRepository.findById(adherent.getId()).get();
            if(adherent.isReferent() && !adherent.getEmail().equals(indbAdherent.getEmail())){
                userServices.updateUsername(adherent.getEmail().toLowerCase(), indbAdherent.getEmail().toLowerCase());
            }
            fillAdherent(adherent,indbAdherent);
            return adherentRepository.save(indbAdherent);
        }
        return null;
    }

    public void deleteAdherent(Long adherentId){
        Adherent adherent = getById(adherentId);
        if(adherent.isReferent()){
            adherent.getTribu().getAdherents().stream().forEach(subAdherent -> adherentRepository.deleteById(subAdherent.getId()));
        }else{
            adherentRepository.deleteById(adherentId);
        }

    }
    public Adherent save(Adherent adherent, Long tribuId){
        Tribu tribu;
        if(tribuId == null){
            tribu = new Tribu();
            tribu = tribuServices.save(tribu);
        }else{
            tribu = tribuServices.getTribuById(tribuId);
        }
        adherent.setTribu(tribu);
        isComplet(adherent);
        return adherentRepository.save(adherent);
    }
    private void isComplet(Adherent adherent){
        if(adherent.getNom() == null || adherent.getNom().length() < 3 ||
                adherent.getPrenom() == null || adherent.getPrenom().length() < 3 ||
                adherent.getAccords().stream().anyMatch(accord -> accord.getEtat() == null)
        ){
            adherent.setCompletReferent(false);
        }else{
            adherent.setCompletReferent(true);

            if(adherent.getNaissance() != null &&
                    adherent.getLieuNaissance() != null &&
                    adherent.getGenre() != null &&
                    (adherent.isAdresseReferent() || adherent.getAdresse() != null) &&
                    (adherent.isEmailReferent() || adherent.getEmail() != null) &&
                    (adherent.isTelephoneReferent() || adherent.getTelephone() != null)
            ){
                adherent.setCompletAdhesion(true);
            }else{
                adherent.setCompletAdhesion(false);
            }
        }
    }
    public Adherent fillAdherent(Adherent frontAdherent, Adherent dataAdherent){
        dataAdherent.setNom(frontAdherent.getNom().toUpperCase());
        dataAdherent.setPrenom(frontAdherent.getPrenom().substring(0, 1).toUpperCase() + frontAdherent.getPrenom()
                .substring(1));

        dataAdherent.setGenre(frontAdherent.getGenre());
        dataAdherent.setNaissance(frontAdherent.getNaissance());
        dataAdherent.setLieuNaissance(frontAdherent.getLieuNaissance());

        dataAdherent.setEmailReferent(frontAdherent.isEmailReferent());
        dataAdherent.setEmail(frontAdherent.getEmail());


        dataAdherent.setTelephoneReferent(frontAdherent.isTelephoneReferent());
        dataAdherent.setTelephone(frontAdherent.getTelephone());

        dataAdherent.setAdresseReferent(frontAdherent.isAdresseReferent());
        dataAdherent.setAdresse(frontAdherent.getAdresse());


        dataAdherent.setMineur(frontAdherent.isMineur());
        dataAdherent.setLegalReferent(frontAdherent.isLegalReferent());
        dataAdherent.setPrenomLegal(frontAdherent.getPrenomLegal());
        dataAdherent.setNomLegal(frontAdherent.getNomLegal());

        Accord frontaccordRgpd = frontAdherent.getAccords().stream().filter(accord -> "RGPD".equals(accord.getNom())).findFirst().get();
        Accord dataaccordRgpd= dataAdherent.getAccords().stream().filter(accord -> "RGPD".equals(accord.getNom())).findFirst().get();
        dataaccordRgpd.setEtat(frontaccordRgpd.getEtat());
        dataaccordRgpd.setDatePassage(frontaccordRgpd.getDatePassage());

        Accord frontaccordImage = frontAdherent.getAccords().stream().filter(accord -> "Droit Image".equals(accord.getNom())).findFirst().get();
        Accord dataaccordImage = dataAdherent.getAccords().stream().filter(accord -> "Droit Image".equals(accord.getNom())).findFirst().get();
        dataaccordImage.setEtat(frontaccordImage.getEtat());
        dataaccordImage.setDatePassage(frontaccordImage.getDatePassage());

        isComplet(dataAdherent);
        return dataAdherent;
    }



    public List<Adherent> getAll(){
        List<Adherent> adherents = adherentRepository.findAll();

        adherents.stream().forEach(adherent -> {
           Adherent adherentRef =  adherent.getTribu().getAdherents().stream().filter(adh -> adh.isReferent()).findFirst().get();
            if(adherent.isEmailReferent()){
                adherent.setEmail(adherentRef.getEmail());
            }
            if(adherent.isAdresseReferent()){
                adherent.setAdresse(adherentRef.getAdresse());
            }
            if(adherent.isTelephoneReferent()){
                adherent.setTelephone(adherentRef.getTelephone());
            }
            adherent.setTribuId(adherent.getTribu().getId());
        });

        return adherents;
    }

}