package com.wild.corp.adhesion.services;


import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.models.resources.AdherentFlat;
import com.wild.corp.adhesion.models.resources.AdherentLite;
import com.wild.corp.adhesion.repository.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.wild.corp.adhesion.utils.Accords.*;
import static java.time.LocalDateTime.now;

@Service
@Slf4j

public class AdherentServices {

    @Autowired
    NotificationRepository notificationRepository;
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




    public Set<String> findByGroup(String groupe) {
        Set<String> listDiffusion = null;
        String[] groupes = groupe.split("#");

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

        adherent.getDerniereVisites().clear();

        adherent.getDerniereModifs().clear();

        adherent.getAdhesions().forEach(adhesion -> {
            adhesionServices.deleteAdhesion(adhesion.getId());
        });
        User adh = adherent.getUser();

        adherent.setUser(null);
        userServices.deleteuser(adh);

        adherentRepository.deleteById(adherentId);



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
        adherent.setMineur(false);
        adherent.setCompletAdhesion(false);
        adherent.setTribu(tribu);

        save(adherent);
        Accord accordRgpd = accordServices.createAccord(RGPD, adherent, null);
        if (initialiseRGPD) {
            accordRgpd.setEtat(true);
            accordRgpd.setDatePassage(LocalDate.now());
        }
        adherent.getAccords().add(accordRgpd);
        adherent.getAccords().add(accordServices.createAccord(DROIT_IMAGE, adherent, null));

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

        if (StringUtils.isNotBlank(adherent.getNom()) &&
                StringUtils.isNotBlank(adherent.getPrenom()) &&
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

        if(!Boolean.TRUE.equals(frontAdherent.getTelephoneRepresentant())){
            dataAdherent.setTelephone(frontAdherent.getTelephone());
        }
        if(!Boolean.TRUE.equals(frontAdherent.getEmailRepresentant())){
            dataAdherent.getUser().setUsername(frontAdherent.getUser().getUsername() != null ? frontAdherent.getUser().getUsername().toLowerCase() : dataAdherent.getUser().getUsername().toLowerCase());
        }
        if(!Boolean.TRUE.equals(frontAdherent.getAdresseRepresentant())){
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
                .telephoneRepresentant(adhesion.getAdherent().getTelephoneRepresentant())
                .adresseRepresentant(adhesion.getAdherent().getAdresseRepresentant())
                .emailRepresentant(adhesion.getAdherent().getEmailRepresentant())
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
            indbAdherent.getAccords().add(accordServices.createAccord(nomAccord, indbAdherent, null));
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
    @Autowired
    PdfService pdfService;
    @Value("${image-storage-dir}")
    private Path imageStorageDir;
    public void regenerate(Long adherentId) {
        Adherent indbAdherent = adherentRepository.findById(adherentId).get();
        byte[] attestation = pdfService.generateSynthese(indbAdherent);
        Path prePath = this.imageStorageDir.resolve(String.valueOf(indbAdherent.getId()));
        try {
            if (!Files.exists(prePath)) {
                Files.createDirectories(prePath);
            }
            final Path targetPath = prePath.resolve("Attestation_ALOD_"+indbAdherent.getPrenom()+"_"+indbAdherent.getNom()+".pdf");
            try (InputStream in = new ByteArrayInputStream(attestation)) {
                try (OutputStream out = Files.newOutputStream(targetPath, StandardOpenOption.CREATE)) {
                    in.transferTo(out);

                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AdherentFlat> getAllFlat() {
        List<AdherentFlat> adherentFlats = new ArrayList<>();
        List<Adherent> adherents = adherentRepository.findAll();

        adherents.forEach(adherent -> {
            AdherentFlat adherentFlat = new AdherentFlat();


            StringBuilder adhesions = new StringBuilder();
            adherent.getAdhesions().forEach(adhesion -> adhesions.append(adhesion.getActivite().getNom() + " " + adhesion.getActivite().getHoraire() + "\n\r"));
            adherentFlat.setAdhesions(adhesions.toString());


            adherentFlat.setId(adherent.getId());
            adherentFlat.setEmail(Boolean.TRUE.equals(adherent.getEmailRepresentant()) && adherent.getRepresentant() != null ? adherent.getRepresentant().getUser().getUsername():adherent.getUser().getUsername());
            adherentFlat.setTelephone(Boolean.TRUE.equals(adherent.getTelephoneRepresentant()) && adherent.getRepresentant() != null ? adherent.getRepresentant().getTelephone():adherent.getTelephone());
            adherentFlat.setAdresse(Boolean.TRUE.equals(adherent.getAdresseRepresentant()) && adherent.getRepresentant() != null ?
                    adherent.getRepresentant().getAdresse() + " " + adherent.getRepresentant().getCodePostal() + " " + adherent.getRepresentant().getVille():
                    adherent.getAdresse() + " " + adherent.getCodePostal() + " " + adherent.getVille());
            adherentFlat.setNaissance(adherent.getNaissance());
            adherentFlat.setPrenom(adherent.getPrenom());
            adherentFlat.setNom(adherent.getNom());
            adherentFlat.setNomPrenom((adherent.getNom() == null ? "zzzz" : adherent.getNom()) + (adherent.getPrenom() == null ? "zzzz" : adherent.getPrenom()));
            adherentFlat.setAccords(adherent.getAccords());
                    adherentFlat.setTribuId(adherent.getTribu().getUuid());

            adherentFlats.add(adherentFlat);
        });

            return adherentFlats;

    }

    public List<AdherentLite> getByRole(Integer roleId) {
        return adherentRepository.findByUserRoleId(roleId).stream().map(this::reduceAdherent).collect(Collectors.toList());
    }

    private AdherentLite reduceAdherent(Adherent adherent) {
        log.info("reduceAdherent " + adherent.getUser().getUsername() + ", id "+adherent.getId());
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
                .email(Boolean.TRUE.equals(adherent.getEmailRepresentant()) && adherent.getRepresentant() != null ? adherent.getRepresentant().getUser().getUsername():adherent.getUser().getUsername())
                .telephone(adherent.getTelephone())
                .mineur(adherent.getMineur())
                .representant(adherent.getRepresentant() != null ? ReduceRepresentant(adherent.getRepresentant()) : null)
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

    private AdherentLite ReduceRepresentant(Adherent adherent) {
        AdherentLite adherentLite = AdherentLite.builder()
                .id(adherent.getId())
                .prenom(adherent.getPrenom())
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
                .telephoneRepresentant(adherent.getTelephoneRepresentant())
                .adresseRepresentant(adherent.getAdresseRepresentant())
                .emailRepresentant(adherent.getEmailRepresentant())
                .build();
        UserLite userLite = new UserLite();
        userLite.setUsername(adherent.getUser().getUsername());
        adherentLite.setUser(userLite);
        return adherentLite;
    }

    public Adherent addModification(String userEmail, Long adherentId, String raison) {
        User user = userServices.findByEmail(userEmail);
        Adherent adherent = getById(adherentId);

        Notification nouvelleModif = new Notification();
        nouvelleModif.setDate(LocalDateTime.now());
        nouvelleModif.setUser(user);
        nouvelleModif.setAdherentModif(adherent);
        nouvelleModif.setRaison(raison);
        adherent.getDerniereModifs().add(nouvelleModif);

        adherentRepository.save(adherent);

        return addVisite(userEmail, adherentId);
    }

    public Adherent addVisite(String userEmail, Long adherentId) {
        User user = userServices.findByEmail(userEmail);
        Adherent adherent = getById(adherentId);
        List<Notification> visites = new ArrayList<>(adherent.getDerniereVisites().stream().filter(visite -> visite.getUser().equals(user)).toList());

        if(visites.isEmpty()){
            Notification nouvelleVisite = new Notification();
            nouvelleVisite.setDate(LocalDateTime.now());
            nouvelleVisite.setUser(user);
            nouvelleVisite.setAdherentVisite(adherent);
            adherent.getDerniereVisites().add(nouvelleVisite);
        }else{
            visites.forEach(notification -> notification.setDate(LocalDateTime.now()));
        }

        return adherentRepository.save(adherent);

    }




    //Pour la nouvelle année
    public void nouvelleAnnee (){

        adherentRepository.findAll().stream().forEach(adherent -> {
            List<ActiviteNm1> activitesNm1 = adherent.getActivitesNm1();
            activitesNm1.clear();
            activitesNm1.addAll(adherent.getAdhesions().stream().filter(Adhesion::isValide).map(this::convertAdh).toList());

            adhesionRepository.deleteAll(adherent.getAdhesions());
            adherent.getAdhesions().removeAll(adherent.getAdhesions());
            adherentRepository.save(adherent);
        });

    }

    public ActiviteNm1 convertAdh(Adhesion adhesion){
        ActiviteNm1 activiteNm1 = new ActiviteNm1();
        activiteNm1.setAdherent(adhesion.getAdherent());
        activiteNm1.setNom(adhesion.getActivite().getNom());
        activiteNm1.setHoraire(adhesion.getActivite().getHoraire());
        activiteNm1.setGroupe(adhesion.getActivite().getGroupe());
        activiteNm1.setSalle(adhesion.getActivite().getSalle());
        activiteNm1.setTarif(adhesion.getActivite().getTarif());
        activiteNm1.setGroupeFiltre(adhesion.getActivite().getGroupeFiltre());
        return activiteNm1;
    }


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
}