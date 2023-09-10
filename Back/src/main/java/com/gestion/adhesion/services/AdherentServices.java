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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;

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

    public AdherentLite changeTribu( Long referentId, Long adherentId) {
        Adherent adherentReferent = adherentRepository.findById(referentId).get();
        Adherent adherentCible = adherentRepository.findById(adherentId).get();

        if(adherentCible.isReferent()){
            adherentCible.setReferent(false);
            adherentCible.getTribu().getAdherents().stream().forEach(adherent -> adherent.setTribu(adherentReferent.getTribu()));
        }else{
            adherentCible.setTribu(adherentReferent.getTribu());
        }
        adherentRepository.save(adherentCible);
        return reduceAdherent(adherentCible);
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
                 if (adherent.getEmail() != null && !adherent.getEmail().isBlank() && !adherent.getEmail().equals(indbAdherent.get().getEmail())) {
                     userServices.updateUsername(adherent.getEmail().toLowerCase(), indbAdherent.get().getUser());
                 }
                 fillAdherent(adherent, indbAdherent.get());
                 return adherentRepository.save(indbAdherent.get());
             }
         }
        return null;
    }


    public void deleteDoc(Long docId, Long adherentId){
        Adherent adh = adherentRepository.findById(adherentId).get();
        Document doc = documentRepository.findById(docId).get();
        adh.getDocuments().remove(doc);
        adherentRepository.save(adh);
    }

    public void deleteAdherent(Long adherentId){
        Adherent adherent = adherentRepository.findById(adherentId).get();
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

    public Adherent save(Adherent adherent){
        return adherentRepository.save(adherent);
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
        Adherent adherentBDD = adherentRepository.save(adherent);

        if(adherent.getUser() == null) {
            userServices.addUserToAdherent(adherentBDD);
        }




        return adherentBDD;
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

        dataAdherent.setEmail(frontAdherent.getEmail(dataAdherent).toLowerCase());


        dataAdherent.setTelephoneReferent(frontAdherent.isTelephoneReferent());
        dataAdherent.setTelephone(frontAdherent.getTelephone(dataAdherent));

        dataAdherent.setAdresseReferent(frontAdherent.isAdresseReferent());
        dataAdherent.setAdresse(frontAdherent.getAdresse(dataAdherent));


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
                   .telephone(adhesion.getAdherent().getTelephone(adhesionRef))
                   .email(adhesion.getAdherent().getEmail(adhesionRef))
                   .mineur(adhesion.getAdherent().isMineur())
                   .prenomLegal(adhesion.getAdherent().isLegalReferent()?adhesionRef.getPrenom():adhesion.getAdherent().getPrenomLegal())
                   .nomLegal(adhesion.getAdherent().isLegalReferent()?adhesionRef.getNom():adhesion.getAdherent().getNomLegal())
                   .telLegal(adhesion.getAdherent().isTelephoneReferent()?adhesionRef.getTelephone():adhesion.getAdherent().getTelephone())
                   .statut(adhesion.getStatutActuel())
                   .commentaire(adhesion.getRemarqueSecretariat())
                   .flag(adhesion.getFlag())
                   .paiement(adhesion.getValidPaiementSecretariat())
                   .build();
                Optional<Accord> accordimage = adhesion.getAdherent().getAccords().stream().filter(accord -> accord.getNom().equals("Droit Image")).findFirst();
                if(accordimage.isPresent()) {
                    adhLite.getAccords().add(accordimage.get());
                }
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

    public Adherent getBasicById(Long adherentId){
        Adherent indbAdherent = adherentRepository.findById(adherentId).get();
        return indbAdherent;
    }
    public Adherent getById(Long adherentId){
        Adherent indbAdherent = adherentRepository.findById(adherentId).get();
        return indbAdherent;
    }



    public List<Accord> addAccord(Long adherentId, String nomAccord){
        Adherent indbAdherent = adherentRepository.findById(adherentId).get();

        if(indbAdherent.getAccords().stream().noneMatch(accord -> nomAccord.equals(accord.getNom()))) {
            Accord newAccord = new Accord(nomAccord);
            indbAdherent.getAccords().add(newAccord);
        }

        return adherentRepository.save(indbAdherent).getAccords();
    }

    public List<Accord> removeAccord(Long adherentId, String nomAccord){
        Adherent indbAdherent = adherentRepository.findById(adherentId).get();
        if(indbAdherent.getAccords().stream().anyMatch(accord -> nomAccord.equals(accord.getNom()))) {
            indbAdherent.getAccords().remove(indbAdherent.getAccords().stream().filter(accord -> nomAccord.equals(accord.getNom())).findFirst().get());
        }

        return adherentRepository.save(indbAdherent).getAccords();
    }

    public List<Adherent> getAll(){
        List<Adherent> adherents = adherentRepository.findAll();

        return adherents;
    }

    public List<AdherentLite> getAllLite(){

        return adherentRepository.findAll().stream().map(this::reduceAdherent).collect(Collectors.toList());
    }

    public List<AdherentLite> getByRole(Integer roleId){

        return adherentRepository.findByUserRoleId(roleId).stream().map(this::reduceAdherent).collect(Collectors.toList());
    }

    private AdherentLite reduceAdherent(Adherent adherent){
        StringBuilder activites = new StringBuilder();
        adherent.getAdhesions().forEach(adhesion -> activites.append(adhesion.getActivite().getNom()+ " " +adhesion.getActivite().getHoraire()+ "\n\r"));

        Adherent adherentRef =  adherent.getTribu().getAdherents().stream().filter(Adherent::isReferent).findFirst().get();
        AdherentLite adherentLite = AdherentLite.builder()
                .id(adherent.getId())
                .prenom(adherent.getPrenom())
                .nom(adherent.getNom())
                .nomPrenom((adherent.getNom()==null?"zzzz":adherent.getNom())+(adherent.getPrenom()==null?"zzzz":adherent.getPrenom()))
                .adresse(adherent.getAdresse(adherentRef))
                .email(adherent.getEmail(adherentRef))
                .telephone(adherent.getTelephone(adherentRef))
                .mineur(adherent.isMineur())
                .referent(adherent.isReferent())
                .nomLegal(adherentRef.getNom())
                .prenomLegal(adherentRef.getPrenom())
                .telLegal(adherentRef.getTelephone())
                .lieuNaissance(adherent.getLieuNaissance())
                .naissance(adherent.getNaissance())
                .tribuId(adherent.getTribu().getId())
                .tribuSize(adherent.getTribu().getAdherents().size())
                .accords(adherent.getAccords())
                .activites(activites.toString())
                .build();

        return adherentLite;
    }


    public Adherent addModification(String userEmail, Long adherentId){
        User user = userServices.findByEmail(userEmail);
        Adherent adherent = getById(adherentId);
        List<Notification> modifications = new java.util.ArrayList<>(adherent.getDerniereModifs().stream().filter(visite -> !visite.getUser().equals(user)).toList());
        Notification nouvelleModif = new Notification();
        nouvelleModif.setDate(LocalDateTime.now());
        nouvelleModif.setUser(user);
        modifications.add(nouvelleModif);
        adherent.setDerniereModifs(modifications);
        adherentRepository.save(adherent);

        return addVisite(userEmail, adherentId);
    }

    public Adherent addVisite(String userEmail, Long adherentId){
        User user = userServices.findByEmail(userEmail);
        Adherent adherent = getById(adherentId);
        List<Notification> visites = new ArrayList<>(adherent.getDerniereVisites().stream().filter(visite -> !visite.getUser().equals(user)).toList());
        Notification nouvelleVisite = new Notification();

        nouvelleVisite.setDate(now());
        nouvelleVisite.setUser(user);
        visites.add(nouvelleVisite);
        adherent.setDerniereVisites(visites);
        return adherentRepository.save(adherent);

    }
}