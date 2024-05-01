package com.wild.corp.adhesion.services;


import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.wild.corp.adhesion.utils.Accords.*;
import static java.time.LocalDateTime.now;

@Service
public class AdherentServices {

    @Autowired
    AdherentRepository adherentRepository;
    @Autowired
    ActiviteNm1Repository activiteNm1Repository;
    @Autowired
    AdhesionRepository adhesionRepository;
    @Autowired
    AccordServices accordServices;
    @Autowired
    AdhesionServices adhesionServices;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    ActiviteServices activiteServices;
    @Autowired
    UserServices userServices;
    @Autowired
    TribuServices tribuServices;


    @Transactional
    public void nouvelleAnnee (){
        activiteNm1Repository.deleteAll();

        adherentRepository.findAll().stream().forEach(adherent -> {
            adherent.getActivitesNm1().clear();
            adherent.getAdhesions().stream().forEach(adhesion -> {
                ActiviteNm1 activiteNm1 = new ActiviteNm1();
                activiteNm1.setAdherent(adherent);
                activiteNm1.setNom(adhesion.getActivite().getNom());
                activiteNm1.setHoraire(adhesion.getActivite().getHoraire());
                activiteNm1.setGroupe(adhesion.getActivite().getGroupe());
                activiteNm1.setSalle(adhesion.getActivite().getSalle());
                activiteNm1.setTarif(adhesion.getActivite().getTarif());
                activiteNm1.setGroupeFiltre(adhesion.getActivite().getGroupeFiltre());
                adherent.getActivitesNm1().add(activiteNm1);

                adhesion.setActivite(null);
                adhesion.setSurClassement(null);
                adhesion.setAdherent(null);
            });



            adherent.getAdhesions().clear();
            adherentRepository.save(adherent);

        });
        adhesionRepository.deleteAll();
    }

    public Set<String> findByGroup(String groupe) {
        Set<String> listDiffusion = null;
        String[] groupes = groupe.split("#");
        System.out.println(groupe);
        if (groupes[0].equals("groupe")) {

            if ("Tous les adhérents".equals(groupes[1])) {
                return adherentRepository.findAll().stream().map(adherent -> {
                            if (adherent.getEmailRepresentant()) {
                                return adherent.getRepresentant().getUser().getUsername();
                            }
                            if (adherent.getUser().getUsername() != null) {
                                return adherent.getUser().getUsername();
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
                            if (adherent.getEmailRepresentant()) {
                                return adherent.getRepresentant().getUser().getUsername();
                            }
                            if (adherent.getUser().getUsername() != null) {
                                return adherent.getUser().getUsername();
                            }
                            return null;
                        }).filter(Objects::nonNull)
                        .collect(Collectors.toSet());

            }
        }

        if (groupes[0].equals("activite")) {
            return adherentRepository.findAll().stream()
                    .filter(adherent -> adherent.getAdhesions().stream().anyMatch(adhesion -> activiteServices.findByNom(groupes[1]).contains(adhesion.getActivite())) || adherent.getCours().stream().anyMatch(activite -> activite.
                            equals(activiteServices.findByNom(groupes[1]))))
                    .map(adherent -> {
                        if (adherent.getEmailRepresentant()) {
                            return adherent.getRepresentant().getUser().getUsername();
                        }
                        if (adherent.getUser().getUsername() != null) {
                            return adherent.getUser().getUsername();
                        }
                        return null;
                    }).filter(Objects::nonNull)
                    .collect(Collectors.toSet());

        }

        if (groupes[0].equals("horaire")) {
            return adherentRepository.findAll().stream()
                    .filter(adherent -> adherent.getAdhesions().stream().anyMatch(adhesion -> adhesion.getActivite().getId().equals(Long.parseLong(groupes[1]))) || adherent.getCours().stream().anyMatch(activite -> activite.getId().equals(Long.parseLong(groupes[1]))))
                    .map(adherent -> {
                        if (adherent.getEmailRepresentant()) {
                            return adherent.getRepresentant().getUser().getUsername();
                        }
                        if (adherent.getUser().getUsername() != null) {
                            return adherent.getUser().getUsername();
                        }
                        return null;
                    }).filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        return null;
    }

    public AdherentLite changeTribu(Long referentId, Long adherentId) {
        Adherent adherentReferent = adherentRepository.findById(referentId).get();
        Adherent adherentCible = adherentRepository.findById(adherentId).get();

        adherentCible.setTribu(adherentReferent.getTribu());

        adherentRepository.save(adherentCible);
        return reduceAdherent(adherentCible);
    }


    public AdherentLite update(AdherentLite adherent) {

        Optional<Adherent> indbAdherent = adherentRepository.findById(adherent.getId());
        if (indbAdherent.isPresent()) {
            fillAdherent(adherent, indbAdherent.get());
            return reduceAdherent(adherentRepository.save(indbAdherent.get()));
        }
        return null;
    }

    public void deleteAdherent(Long adherentId) {
        Adherent adherent = adherentRepository.findById(adherentId).get();
        adherentRepository.deleteById(adherentId);
        if (adherent.getUser() != null) {
            userServices.deleteById(adherent.getUser().getId());
        }
    }

    public Adherent save(Adherent adherent) {
        return adherentRepository.save(adherent);
    }

    public Adherent newAdherent(UUID tribuId, boolean initialiseRGPD) {
        Tribu tribu;
        if (tribuId == null) {
            tribu = new Tribu(UUID.randomUUID());
        } else {
            tribu = tribuServices.getTribuByUuid(tribuId);
            if (tribu == null) {
                tribu = new Tribu(UUID.randomUUID());
            }
        }
        tribuServices.save(tribu);

        Adherent adherent = new Adherent();
        adherent.setTribu(tribu);

        Accord accordRgpd = accordServices.createAccord(RGPD);
        if (initialiseRGPD) {
            accordRgpd.setEtat(true);
            accordRgpd.setDatePassage(LocalDate.now());
        }
        adherent.getAccords().add(accordRgpd);
        adherent.getAccords().add(accordServices.createAccord(DROIT_IMAGE));

        adherent.setMineur(false);
        adherent.setAdresseRepresentant(true);
        adherent.setTelephoneRepresentant(true);
        adherent.setEmailRepresentant(true);
        adherent.setCompletAdhesion(false);

        save(adherent);
        return adherent;
    }

    public AdherentLite newAdherentDansTribu(String username, String tribuUuid) {
        UUID tribuId;
        if (tribuUuid.isBlank()) {
            User user = userServices.findByEmail(username);
            tribuId = user.getAdherent().getTribu().getUuid();
        } else {
            tribuId = UUID.fromString(tribuUuid);
        }
        Adherent adherent = newAdherent(tribuId, false);

        Random random = new Random();
        String password = random.toString();

        adherent.setUser(userServices.addNewUser(UUID.randomUUID() + "@mailfictif.com", password));
        save(adherent);
        return reduceAdherent(adherent);
    }

    private void isComplet(Adherent adherent) {

        if (adherent.getNom() != null && adherent.getNom().length() > 3 &&
                        adherent.getPrenom() != null & adherent.getPrenom().length() > 3 &&
                        adherent.getNaissance() != null &&
                        adherent.getLieuNaissance() != null &&
                        adherent.getGenre() != null &&
                        adherent.getAccords().stream().anyMatch(accord -> accord.getNom().equals("RGPD") && accord.getEtat() != null) &&
                        adherent.getAccords().stream().anyMatch(accord -> accord.getNom().equals("DroitImage") && accord.getEtat() != null) &&
                        (adherent.getAdresse() != null || adherent.getRepresentant() != null) &&
                        (adherent.getTelephone() != null || adherent.getRepresentant() != null)
        ) {
            adherent.setCompletAdhesion(true);
            setActiviteNm1(adherent);
        } else {
            adherent.setCompletAdhesion(false);
        }

    }

    public void setActiviteNm1(Adherent adherent){
        if(adherent.getMineur()){
            adherent.getTribu().getAutorisations().stream().filter(activiteNm1 -> activiteNm1.getHoraire().contains("Mineur")).forEach(activiteNm1 -> {
                activiteNm1.setHoraire("Autorisation d'inscription manuelle");
                activiteNm1.setAdherent(adherent);
                adherent.getActivitesNm1().add(activiteNm1);
            });
        }else{
            adherent.getTribu().getAutorisations().stream().filter(activiteNm1 -> activiteNm1.getHoraire().contains("Majeur")).forEach(activiteNm1 -> {
                activiteNm1.setHoraire("Autorisation d'inscription manuelle");
                activiteNm1.setAdherent(adherent);
                adherent.getActivitesNm1().add(activiteNm1);
            });
        }


    }
    public Adherent fillAdherent(AdherentLite frontAdherent, Adherent dataAdherent) {
        dataAdherent.setNom(frontAdherent.getNom().toUpperCase());
        dataAdherent.setPrenom(frontAdherent.getPrenom().substring(0, 1).toUpperCase() + frontAdherent.getPrenom()
                .substring(1));

        dataAdherent.setGenre(frontAdherent.getGenre());
        dataAdherent.setNaissance(frontAdherent.getNaissance());
        dataAdherent.setLieuNaissance(frontAdherent.getLieuNaissance());

        if(!frontAdherent.getMineur() || Boolean.FALSE.equals(frontAdherent.getTelephoneRepresentant())){
            dataAdherent.setTelephone(frontAdherent.getTelephone());
        }
        if(!frontAdherent.getMineur() || Boolean.FALSE.equals(frontAdherent.getEmailRepresentant())){
            dataAdherent.getUser().setUsername(frontAdherent.getUser().getUsername() != null ? frontAdherent.getUser().getUsername().toLowerCase() : dataAdherent.getUser().getUsername().toLowerCase());
        }
        if(!frontAdherent.getMineur() || Boolean.FALSE.equals(frontAdherent.getAdresseRepresentant())){
            dataAdherent.setAdresse(frontAdherent.getAdresse());
            dataAdherent.setCodePostal(frontAdherent.getCodePostal());
            dataAdherent.setVille(frontAdherent.getVille());
        }

        dataAdherent.setMineur(frontAdherent.getMineur());
        if(frontAdherent.getRepresentant() != null){
            dataAdherent.setRepresentant(userServices.adherentServices.getById(frontAdherent.getRepresentant().getId()));
        }

        dataAdherent.setTelephoneRepresentant(frontAdherent.getTelephoneRepresentant());
        dataAdherent.setEmailRepresentant(frontAdherent.getEmailRepresentant());
        dataAdherent.setAdresseRepresentant(frontAdherent.getAdresseRepresentant());


        Accord frontaccordRgpd = frontAdherent.getAccords().stream().filter(accord -> "RGPD".equals(accord.getNom())).findFirst().get();
        Accord dataaccordRgpd = dataAdherent.getAccords().stream().filter(accord -> "RGPD".equals(accord.getNom())).findFirst().get();
        if(dataaccordRgpd.getDatePassage()==null){
            dataaccordRgpd.setEtat(frontaccordRgpd.getEtat());
            dataaccordRgpd.setDatePassage(LocalDate.now());
        }


        Accord frontaccordImage = frontAdherent.getAccords().stream().filter(accord -> "DroitImage".equals(accord.getNom())).findFirst().get();
        Accord dataaccordImage = dataAdherent.getAccords().stream().filter(accord -> "DroitImage".equals(accord.getNom())).findFirst().get();
        if(dataaccordImage.getDatePassage()==null){
            dataaccordImage.setEtat(frontaccordImage.getEtat());
            dataaccordImage.setDatePassage(LocalDate.now());
        }

        isComplet(dataAdherent);
        return dataAdherent;
    }


    public Set<ActiviteLite> getAllCours(String username) {
        User user = userServices.findByEmail(username);
        Set<ActiviteLite> activiteLites = user.getAdherent().getCours().stream().map(activite -> {
            Set<AdherentLite> adherentsLite = activite.getAdhesions().stream().map(this::veryReduceAdherent).collect(Collectors.toSet());

            adherentsLite.addAll(activite.getSousClassement().stream().map(this::veryReduceAdherent).collect(Collectors.toSet()));

            return ActiviteLite.builder().id(activite.getId())
                    .salle(activite.getSalle())
                    .lien(activite.getLien())
                    .horaire(activite.getHoraire())
                    .nom(activite.getNom())
                    .adherents(adherentsLite).build();
        }).collect(Collectors.toSet());
        return activiteLites;
    }

    private AdherentLite veryReduceAdherent(Adhesion adhesion) {
        AdherentLite adhLite = AdherentLite.builder().id(adhesion.getAdherent().getId())
                .prenom(adhesion.getAdherent().getPrenom())
                .nom(adhesion.getAdherent().getNom())
                .accords(adhesion.getAccords())
                .telephone(adhesion.getAdherent().getTelephone())
                .email(adhesion.getAdherent().getUser().getUsername())
                .mineur(adhesion.getAdherent().getMineur())
                .naissance((adhesion.getAdherent().getNaissance()))
                .representant(adhesion.getAdherent().getRepresentant() != null?reduceAdherent(adhesion.getAdherent().getRepresentant()):null)
                .statut(adhesion.getStatutActuel())
                .commentaire(adhesion.getRemarqueSecretariat())
                .flag(adhesion.getFlag())
                .paiement(adhesion.getValidPaiementSecretariat())
                .build();
        Optional<Accord> accordimage = adhesion.getAdherent().getAccords().stream().filter(accord -> accord.getNom().equals("DroitImage")).findFirst();
        if (accordimage.isPresent()) {
            adhLite.getAccords().add(accordimage.get());
        }
        return adhLite;
    }

    public List<Long> getAllId() {
        return adherentRepository.getAllIds();
    }

    public Adherent getBasicById(Long adherentId) {
        Adherent indbAdherent = adherentRepository.findById(adherentId).get();
        return indbAdherent;
    }

    public Adherent getById(Long adherentId) {
        Adherent indbAdherent = adherentRepository.findById(adherentId).get();
        return indbAdherent;
    }


    public List<Accord> addAccord(Long adherentId, String nomAccord) {
        Adherent indbAdherent = adherentRepository.findById(adherentId).get();
        if (indbAdherent.getAccords().stream().noneMatch(accord -> nomAccord.equals(accord.getNom()))) {
            indbAdherent.getAccords().add(accordServices.createAccord(nomAccord));
        }
        return adherentRepository.save(indbAdherent).getAccords();
    }

    public List<Accord> removeAccord(Long adherentId, String nomAccord) {
        Adherent indbAdherent = adherentRepository.findById(adherentId).get();
        if (indbAdherent.getAccords().stream().anyMatch(accord -> nomAccord.equals(accord.getNom()))) {
            indbAdherent.getAccords().remove(indbAdherent.getAccords().stream().filter(accord -> nomAccord.equals(accord.getNom())).findFirst().get());
        }
        return adherentRepository.save(indbAdherent).getAccords();
    }

    public List<Adherent> getAll() {
        List<Adherent> adherents = adherentRepository.findAll();
        return adherents;
    }

    public List<AdherentLite> getAllLite() {
        return adherentRepository.findAll().stream().map(this::reduceAdherent).collect(Collectors.toList());
    }

    public List<AdherentLite> getByRole(Integer roleId) {
        return adherentRepository.findByUserRoleId(roleId).stream().map(this::reduceAdherent).collect(Collectors.toList());
    }

    private AdherentLite reduceAdherent(Adherent adherent) {
        StringBuilder activites = new StringBuilder();
        adherent.getAdhesions().forEach(adhesion -> activites.append(adhesion.getActivite().getNom() + " " + adhesion.getActivite().getHoraire() + "\n\r"));

        AdherentLite adherentLite = AdherentLite.builder()
                .id(adherent.getId())
                .prenom(adherent.getPrenom())
                .adhesions(adhesionServices.reduceAdhesions(adherent.getAdhesions()))
                .nom(adherent.getNom())
                .genre(adherent.getGenre())
                .completAdhesion(adherent.getCompletAdhesion())
                .nomPrenom((adherent.getNom() == null ? "zzzz" : adherent.getNom()) + (adherent.getPrenom() == null ? "zzzz" : adherent.getPrenom()))
                .adresse(adherent.getAdresse())
                .codePostal(adherent.getCodePostal())
                .ville(adherent.getVille())
                .email(adherent.getUser().getUsername())
                .telephone(adherent.getTelephone())
                .mineur(adherent.getMineur())
                .representant(adherent.getRepresentant() != null ? reduceAdherent(adherent.getRepresentant()) : null)
                .telephoneRepresentant(adherent.getTelephoneRepresentant())
                .adresseRepresentant(adherent.getAdresseRepresentant())
                .emailRepresentant(adherent.getEmailRepresentant())
                .lieuNaissance(adherent.getLieuNaissance())
                .naissance(adherent.getNaissance())
                .tribuId(adherent.getTribu().getUuid())
                .tribuSize(adherent.getTribu().getAdherents().size())
                .accords(adherent.getAccords())
                .activitesNm1(adherent.getActivitesNm1())
                .activites(activites.toString())
                .lien("www.alod.fr/adhesion/#/inscription/" + adherent.getTribu().getUuid())
                .build();
        UserLite userLite = new UserLite();
        userLite.setUsername(adherent.getUser().getUsername());
        adherentLite.setUser(userLite);
        return adherentLite;
    }
    @Autowired
    NotificationRepository notificationRepository;
    public void cleanNotification(){

        List<Adherent> adherents = adherentRepository.findAll();
        adherents.stream().forEach(adherent -> {
        adherent.setDerniereModifs(null);
        adherent.setDerniereVisites(null);
        });

        List<Notification> notifications = notificationRepository.findAll();
        notifications.forEach(notification -> {
            notification.setUser(null);
        });
notificationRepository.deleteAll();
    }

    public void refreshAccords(){
        accordServices.refreshAccords();

    }
    public Adherent addModification(String userEmail, Long adherentId) {
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

    public Adherent addVisite(String userEmail, Long adherentId) {
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