package com.gestion.adhesion.services;

import com.gestion.adhesion.models.*;
import com.gestion.adhesion.repository.AdherentRepository;
import com.gestion.adhesion.repository.DocumentRepository;
import com.gestion.adhesion.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdherentServices {

    @Autowired
    AdherentRepository adherentRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    DocumentRepository documentRepository;
    @Autowired
    ActiviteServices activiteServices;
    @Autowired
    UserServices userServices;
    @Autowired
    TribuServices tribuServices;

    public Set<String> findByGroup(String groupe){
        Set<String> listDiffusion = null;
        String[] groupes = groupe.split("#");
        System.out.println(groupe);
        if(groupes[0].equals("groupe")) {

            if ("Tous les adhérents".equals(groupes[1])) {
                return adherentRepository.findAll().stream().map(adherent -> {
                            if (adherent.isEmailReferent()) {
                                return adherent.getTribu().getAdherents().stream().filter(Adherent::isReferent).findFirst().get().getEmail();
                            }
                            if (adherent.getEmail() != null) {
                                return adherent.getEmail();
                            }
                            return null;
                        }).filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }

            if ("Les membres du bureau".equals(groupes[1])) {
                return userServices.findAll().stream()
                        .filter(user -> user.getRoles().contains(roleRepository.findByName(ERole.ROLE_BUREAU)))
                        .map(User::getUsername)
                        .collect(Collectors.toSet());
            }

            if ("Les administrateurs".equals(groupes[1])) {
                return userServices.findAll().stream()
                        .filter(user -> user.getRoles().contains(roleRepository.findByName(ERole.ROLE_ADMINISTRATEUR)))
                        .map(User::getUsername)
                        .collect(Collectors.toSet());
            }

            if ("Sans certificat médical".equals(groupes[1])) {
                return adherentRepository.findAll().stream()
                        .filter(adherent -> adherent.getAdhesions().stream().noneMatch(Adhesion::getValidDocumentSecretariat))
                        .map(adherent -> {
                            if (adherent.isEmailReferent()) {
                                return adherent.getTribu().getAdherents().stream().filter(Adherent::isReferent).findFirst().get().getEmail();
                            }
                            if (adherent.getEmail() != null) {
                                return adherent.getEmail();
                            }
                            return null;
                        }).filter(Objects::nonNull)
                        .collect(Collectors.toSet());

            }
        }

        if(groupes[0].equals("activite")) {
            return adherentRepository.findAll().stream()
                    .filter(adherent -> adherent.getAdhesions().stream().anyMatch(adhesion -> activiteServices.findByNom(groupes[1]).contains(adhesion.getActivite())) || adherent.getCours().stream().anyMatch(activite -> activite.
                            equals(activiteServices.findByNom(groupes[1]))))
                    .map(adherent -> {
                        if (adherent.isEmailReferent()) {
                            return adherent.getTribu().getAdherents().stream().filter(Adherent::isReferent).findFirst().get().getEmail();
                        }
                        if (adherent.getEmail() != null) {
                            return adherent.getEmail();
                        }
                        return null;
                    }).filter(Objects::nonNull)
                    .collect(Collectors.toSet());

        }

        if(groupes[0].equals("horaire")) {
            return adherentRepository.findAll().stream()
                    .filter(adherent -> adherent.getAdhesions().stream().anyMatch(adhesion -> adhesion.getActivite().getId().equals(Long.parseLong(groupes[1]))) || adherent.getCours().stream().anyMatch(activite -> activite.getId().equals(Long.parseLong(groupes[1]))))
                    .map(adherent -> {
                        if (adherent.isEmailReferent()) {
                            return adherent.getTribu().getAdherents().stream().filter(Adherent::isReferent).findFirst().get().getEmail();
                        }
                        if (adherent.getEmail() != null) {
                            return adherent.getEmail();
                        }
                        return null;
                    }).filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        return null;
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



    public Adherent updateEmail(String email, Long adherentId){
        Optional<Adherent> indbAdherent = adherentRepository.findById(adherentId);
        if(indbAdherent.isPresent()) {
            if (indbAdherent.get().isReferent()) {
                userServices.updateUsername(email.toLowerCase(), indbAdherent.get().getEmail().toLowerCase());
            }
            indbAdherent.get().setEmail(email.toLowerCase());
            return adherentRepository.save(indbAdherent.get());
        }else{
            return null;
        }
    }

    public Document addDocument(MultipartFile file, Long adherentId) throws IOException {
        Document doc = uploadImage(file);

        Optional<Adherent> indbAdherent = adherentRepository.findById(adherentId);
        if (indbAdherent.isPresent()) {
            indbAdherent.get().getDocuments().add(doc);
            adherentRepository.save(indbAdherent.get());
        }
        return doc;
    }

    public Document uploadImage(MultipartFile file) throws IOException {

        return documentRepository.save(Document.builder()
                .dateDepot(LocalDate.now())
                .nom(file.getOriginalFilename())
                .type(file.getContentType())
                .file(file.getBytes()).build());
    }

    public Adherent update(Adherent adherent){
         if (adherent.getId() != null && adherent.getId() > 0) {
             Optional<Adherent> indbAdherent = adherentRepository.findById(adherent.getId());
             if (indbAdherent.isPresent()) {
                 if (adherent.isReferent() && !adherent.getEmail().equals(indbAdherent.get().getEmail())) {
                     userServices.updateUsername(adherent.getEmail().toLowerCase(), indbAdherent.get().getEmail().toLowerCase());
                 }
                 fillAdherent(adherent, indbAdherent.get());
                 return adherentRepository.save(indbAdherent.get());
             }
         }
        return null;
    }


    public void deleteDoc(Long docId, Long adherentId){
        Adherent adh = getById(adherentId);
        Document doc = documentRepository.findById(docId).get();
        adh.getDocuments().remove(doc);
        adherentRepository.save(adh);
    }

    public void deleteAdherent(Long adherentId){
        Adherent adherent = getById(adherentId);
        if(adherent.isReferent()){
            adherent.getTribu().getAdherents().stream().forEach(subAdherent -> {
                adherentRepository.deleteById(subAdherent.getId());
                if(subAdherent.getUser() != null){
                    userServices.deleteById(subAdherent.getUser().getId());
                }
            });
        }else{
            adherentRepository.deleteById(adherentId);
            if(adherent.getUser() != null){
                userServices.deleteById(adherent.getUser().getId());
            }
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
                adherent.getAccords().stream().anyMatch(accord -> accord.getNom().equals("RGPD") && accord.getEtat() == null)
        ){
            adherent.setCompletReferent(false);
        }else{
            adherent.setCompletReferent(true);

            if(adherent.getNaissance() != null &&
                    adherent.getLieuNaissance() != null &&
                    adherent.getGenre() != null &&
                    (adherent.isAdresseReferent() || adherent.getAdresse() != null) &&
                    (adherent.isEmailReferent() || adherent.getEmail() != null) &&
                    (adherent.isTelephoneReferent() || adherent.getTelephone() != null) &&
                    adherent.getAccords().stream().anyMatch(accord -> accord.getNom().equals("Droit Image") && accord.getEtat() != null)
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



    public Set<ActiviteLite> getAllCours(String username){
        User user = userServices.findByEmail(username);
        Set<ActiviteLite> activiteLites = user.getAdherent().getCours().stream().map(activite -> {

           Set<AdherentLite> adherentsLite = activite.getAdhesions().stream().map(adhesion -> {

               final Adherent adhesionRef = adhesion.getAdherent().getTribu().getAdherents().stream().filter(Adherent::isReferent).findFirst().get();
               AdherentLite adhLite = AdherentLite.builder().id(adhesion.getAdherent().getId())
                   .prenom(adhesion.getAdherent().getPrenom())
                   .nom(adhesion.getAdherent().getNom())
                   .accords(adhesion.getAccords())
                   .telephone(adhesion.getAdherent().getTelephone())
                   .email(adhesion.getAdherent().getEmail())
                   .prenomLegal(adhesion.getAdherent().isLegalReferent()?adhesionRef.getPrenom():adhesion.getAdherent().getPrenomLegal())
                   .nomLegal(adhesion.getAdherent().isLegalReferent()?adhesionRef.getNom():adhesion.getAdherent().getNomLegal())
                   .telLegal(adhesion.getAdherent().isTelephoneReferent()?adhesionRef.getNom():adhesion.getAdherent().getTelephone())
                   .build();

               adhLite.getAccords().add(adhesion.getAdherent().getAccords().stream().filter(accord -> accord.getNom().equals("Droit Image")).findFirst().get());
               return adhLite;
           }).collect(Collectors.toSet());

        return ActiviteLite.builder().id(activite.getId())
            .salle(activite.getSalle())
            .lien(activite.getLien())
            .horaire(activite.getHoraire())
            .nom(activite.getNom())
            .adherents(adherentsLite).build();
        }).collect(Collectors.toSet());

        return activiteLites;
    }


    public List<Long> getAllId(){
        return adherentRepository.getAllIds();
    }


    public Adherent getById(Long adherentId){
        Adherent indbAdherent = adherentRepository.findById(adherentId).get();
        sanitizeAdherent(indbAdherent);
        return indbAdherent;
    }


    public List<Adherent> getAll(){
        List<Adherent> adherents = adherentRepository.findAll();

        adherents.forEach(this::sanitizeAdherent);

        return adherents;
    }


    private void sanitizeAdherent(Adherent adherent){
        Adherent adherentRef =  adherent.getTribu().getAdherents().stream().filter(Adherent::isReferent).findFirst().get();
        if(adherent.isEmailReferent()){
            adherent.setEmail(adherentRef.getEmail());
        }
        if(adherent.isAdresseReferent()){
            adherent.setAdresse(adherentRef.getAdresse());
        }
        if(adherent.isTelephoneReferent()){
            adherent.setTelephone(adherentRef.getTelephone());
        }

        adherent.setAccords(null);
        adherent.setDocuments(null);
    }

}