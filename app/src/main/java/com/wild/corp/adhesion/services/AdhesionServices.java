package com.wild.corp.adhesion.services;


import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.AdhesionRepository;
import com.wild.corp.adhesion.repository.PaiementRepository;
import com.wild.corp.adhesion.utils.Status;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
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

import static com.wild.corp.adhesion.services.AccordServices.*;
import static java.time.LocalDate.now;

@Slf4j
@Service
public class AdhesionServices {

    @Autowired
    AdhesionRepository adhesionRepository;
    @Autowired
    PaiementRepository paiementRepository;
    @Autowired
    UserServices userServices;
    @Autowired
    AdherentServices adherentServices;
    @Autowired
    ActiviteServices activiteServices;
    @Autowired
    AccordServices accordServices;
    @Autowired
    EmailService emailService;
    @Autowired
    PdfService pdfService;
    public List<AdhesionLite> getLiteBySection(String section) {
        String[] sections = section.split("#");

        if (sections[0].equals("activite")) {
            return adhesionRepository.findAll().stream()
                    .filter(adhesion -> activiteServices.findByNom(sections[1]).contains(adhesion.getActivite()))
                    .map(this::reduceAdhesion).filter(Objects::nonNull).toList();
        } else if (sections[0].equals("horaire")) {
            return adhesionRepository.findAll().stream()
                    .filter(adhesion -> adhesion.getActivite().getId().equals(Long.parseLong(sections[1])))
                    .map(this::reduceAdhesion).filter(Objects::nonNull).toList();
        } else {
            List<Adhesion> adhesions = adhesionRepository.findAll();
            return adhesions.stream().map(this::reduceAdhesion).toList();
        }
    }

    public Set<AdhesionLite> reduceAdhesions(Set<Adhesion> adhesions){
        return adhesions.stream().map(this::reduceAdhesion).collect(Collectors.toSet());
    }

    private AdhesionLite reduceAdhesion(Adhesion adhesion) {
        return AdhesionLite.builder().id(adhesion.getId())
                .adherent(AdherentLite.builder()
                        .id(adhesion.getAdherent().getId())
                        .nomPrenom((adhesion.getAdherent().getNom() == null ? "zzzz" : adhesion.getAdherent().getNom()) + (adhesion.getAdherent().getPrenom() == null ? "zzzz" : adhesion.getAdherent().getPrenom()))
                        .nom(adhesion.getAdherent().getNom())
                        .prenom(adhesion.getAdherent().getPrenom())
                        .email(adhesion.getAdherent().getUser().getUsername())
                        .adresse(adhesion.getAdherent().getAdresse())
                        .derniereVisites(adhesion.getAdherent().getDerniereVisites())
                        .derniereModifs(adhesion.getAdherent().getDerniereModifs())
                        .tribuId(adhesion.getAdherent().getTribu().getUuid())
                        .build())
                .activite(ActiviteLite.builder()
                        .nom(adhesion.getActivite().getNom())
                        .id(adhesion.getActivite().getId())
                        .horaire(adhesion.getActivite().getHoraire())
                        .groupe(adhesion.getActivite().getGroupe())
                        .build())
                .accords(adhesion.getAccords())
                .paiements(adhesion.getPaiements())
                .dateAjoutPanier(adhesion.getDateAjoutPanier())
                .dateChangementStatut(adhesion.getDateChangementStatut())
                .remarqueSecretariat(adhesion.getRemarqueSecretariat())
                .flag(adhesion.getFlag())
                .validDocumentSecretariat(adhesion.getValidDocumentSecretariat())
                .validPaiementSecretariat(adhesion.getValidPaiementSecretariat())
                .statutActuel(adhesion.getStatutActuel())
                .derniereVisites(adhesion.getDerniereVisites())
                .derniereModifs(adhesion.getDerniereModifs())
                .build();
    }

    public List<AdhesionLite> getAllLite() {
        List<Adhesion> adhesions = adhesionRepository.findAll();

        return adhesions.stream().map(adhesion -> reduceAdhesion(adhesion)).toList();
    }


    public void deleteSurclassement(Long adhesionId) {
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();
        adhesion.setSurClassement(null);
    }
    @Value("${image-storage-dir}")
    private Path imageStorageDir;
    public Adhesion saveSurclassement(Long adhesionId, Long surClassementId) throws IOException {
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();
        Activite surClassement = activiteServices.getById(surClassementId);
        adhesion.setSurClassement(surClassement);
        adhesionRepository.save(adhesion);
        return  adhesion;
    }

    public void deletePaiement(Long adhesionId, Long paiementId) {
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();
        adhesion.getPaiements().remove(paiementRepository.findById(paiementId).get());

        paiementRepository.deleteById(paiementId);
    }

    public Adhesion savePaiement(Long adhesionId, Paiement paiement) {
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();

        List<Paiement> paiements = adhesion.getPaiements();
        if (paiements.contains(paiement)) {
            paiements.stream().filter(paiement1 -> paiement1.getId().equals(paiement.getId()))
                    .forEach(paiement1 -> {
                                paiement1.setTypeReglement(paiement.getTypeReglement());
                                paiement1.setMontant(paiement.getMontant());
                                paiement1.setDateReglement(paiement.getDateReglement());
                            }
                    );

        } else {
            paiement.setDateReglement(now());
            paiement.setTypeReglement("HelloAsso");
            paiement.setMontant(adhesion.getActivite().getTarif());
            paiements.add(paiement);
        }

        return adhesionRepository.save(adhesion);
    }


    public List<Adhesion> getAll() {
        List<Adhesion> adhesions = adhesionRepository.findAll();

        return adhesions;
    }

    public Adhesion validation(List<Accord> accords, Long adhesionId) {
        Adhesion dataAdhesion = adhesionRepository.findById(adhesionId).get();

        accords.forEach(accord -> {
            accord.setDatePassage(LocalDate.now());
        });

        dataAdhesion.setAccords(accords);

        adhesionRepository.save(dataAdhesion);
        return choisirStatut(dataAdhesion.getId(),  Status.ATTENTE_SECRETARIAT.label);
    }

    public Adhesion update(Adhesion frontAdhesion) {
        Adhesion dataAdhesion = adhesionRepository.findById(frontAdhesion.getId()).get();

        Accord frontaccordImage = frontAdhesion.getAccords().stream().filter(accord -> "ReglementInterieur".equals(accord.getNom())).findFirst().get();
        Accord dataaccordImage = dataAdhesion.getAccords().stream().filter(accord -> "ReglementInterieur".equals(accord.getNom())).findFirst().get();
        dataaccordImage.setEtat(frontaccordImage.getEtat());
        dataaccordImage.setDatePassage(LocalDate.now());

        if (frontAdhesion.getAccords().stream().anyMatch(accord -> "AttestationSante".equals(accord.getNom()))) {
            Accord frontaccordAS = frontAdhesion.getAccords().stream().filter(accord -> "AttestationSante".equals(accord.getNom())).findFirst().get();
            Accord dataaccordAS = dataAdhesion.getAccords().stream().filter(accord -> "AttestationSante".equals(accord.getNom())).findFirst().get();
            dataaccordAS.setEtat(frontaccordAS.getEtat());
            dataaccordAS.setDatePassage(LocalDate.now());
        }

        if (frontAdhesion.getAccords().stream().anyMatch(accord -> "VieClub".equals(accord.getNom()))) {
            Accord frontaccordVC = frontAdhesion.getAccords().stream().filter(accord -> "VieClub".equals(accord.getNom())).findFirst().get();
            Accord dataaccordVC = dataAdhesion.getAccords().stream().filter(accord -> "VieClub".equals(accord.getNom())).findFirst().get();
            dataaccordVC.setEtat(frontaccordVC.getEtat());
            dataaccordVC.setDatePassage(LocalDate.now());
        }

        if (frontAdhesion.getAccords().stream().anyMatch(accord -> "AutorisationParentale".equals(accord.getNom()))) {
            Accord frontaccordAP = frontAdhesion.getAccords().stream().filter(accord -> "AutorisationParentale".equals(accord.getNom())).findFirst().get();
            Accord dataaccordAP = dataAdhesion.getAccords().stream().filter(accord -> "AutorisationParentale".equals(accord.getNom())).findFirst().get();
            dataaccordAP.setEtat(frontaccordAP.getEtat());
            dataaccordAP.setDatePassage(LocalDate.now());
        }

        if (frontAdhesion.getAccords().stream().anyMatch(accord -> "PriseEnCharge".equals(accord.getNom()))) {
            Accord frontaccordPC = frontAdhesion.getAccords().stream().filter(accord -> "PriseEnCharge".equals(accord.getNom())).findFirst().get();
            Accord dataaccordPC = dataAdhesion.getAccords().stream().filter(accord -> "PriseEnCharge".equals(accord.getNom())).findFirst().get();
            dataaccordPC.setEtat(frontaccordPC.getEtat());
            dataaccordPC.setDatePassage(LocalDate.now());
        }
        adhesionRepository.save(dataAdhesion);
        return choisirStatut(dataAdhesion.getId(), Status.ATTENTE_SECRETARIAT.label);
    }

    public Adhesion saveUnique(Adhesion adhesion) {
        return adhesionRepository.save(adhesion);
    }

    public Adhesion save(Long adherentId, Long activiteId) {
        Activite activite = activiteServices.getById(activiteId);
        Adhesion newAdhesion = new Adhesion();
        newAdhesion.setRappel(false);
        newAdhesion.setDateAjoutPanier(now());
        newAdhesion.setAdherent(adherentServices.getBasicById(adherentId));
        newAdhesion.setActivite(activite);
        newAdhesion.setDejaLicencie("ALOD_B".equals(activite.getGroupeFiltre())?true:false);
        newAdhesion.setFlag(false);
        newAdhesion.setInscrit(false);
        newAdhesion.setValidDocumentSecretariat(false);
        newAdhesion.setValidPaiementSecretariat(false);
        newAdhesion.setTarif(activite.getTarif());

        newAdhesion.getAccords().add(accordServices.createAccord(REGLEMENT_INTERIEUR));

        if (activite.isCertificatMedical() && activite.getDureeVieCertif() > 1) {
            newAdhesion.getAccords().add(accordServices.createAccord(ATTESTATION_SANTE));
        }

        if (activite.isVieClub()) {
            newAdhesion.getAccords().add(accordServices.createAccord(VIE_CLUB));
        }

        if (activite.isAutorisationParentale() && newAdhesion.getAdherent().getMineur()) {
            newAdhesion.getAccords().add(accordServices.createAccord(AUTORISATION_PARENTALE));
        }

        if (activite.isPriseEnCharge() && newAdhesion.getAdherent().getMineur()) {
            newAdhesion.getAccords().add(accordServices.createAccord(PRISE_EN_CHARGE));
        }

        if (activite.getAdhesions().stream().filter(adh -> adh.isEnCours() || adh.isValide()).count() >= activite.getNbPlaces()) {
            newAdhesion.setStatutActuel(Status.LISTE_ATTENTE.label);
            Optional<Adhesion> adhesionAttente = activite.getAdhesions().stream().filter(adh -> !adh.isEnCours() && !adh.isValide())
                    .max(Comparator.comparing(Adhesion::getPosition));

            if (adhesionAttente.isPresent()) {
                newAdhesion.setPosition(adhesionAttente.get().getPosition() + 1);
            } else {
                newAdhesion.setPosition(1);
            }
        } else {
            newAdhesion.setStatutActuel(Status.ATTENTE_ADHERENT.label);
        }
        return adhesionRepository.save(newAdhesion);
    }

    public Adhesion updateDocumentsSecretariat(Long adhesionId, Boolean statut) {
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();
        adhesion.setValidDocumentSecretariat(statut);
        return adhesionRepository.save(adhesion);
    }

    public Adhesion updatePaiementSecretariat(Long adhesionId, Boolean statut) {
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();

        adhesion.setValidPaiementSecretariat(statut);
        return adhesionRepository.save(adhesion);
    }

    public Adhesion updateFlag(Long adhesionId, Boolean statut) {
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();
        adhesion.setFlag(statut);
        return adhesionRepository.save(adhesion);
    }

    public Adhesion enregistrerRemarque(Long adhesionId, String remarqueSecretariat) {
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();
        adhesion.setRemarqueSecretariat(remarqueSecretariat);
        return adhesionRepository.save(adhesion);
    }

    public Adhesion choisirStatut(Long adhesionId, String nouveauStatut) {
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();

        if (Status.LICENCE_T.label.equals(nouveauStatut) && !Status.LICENCE_T.label.equals(adhesion.getStatutActuel())) {
           adhesion.setValidPaiementSecretariat(true);
           adhesion.setValidDocumentSecretariat(true);
           adhesion.setAccords(null);
            adhesion.setInscrit(true);
        }


        if ((Status.VALIDEE.label.equals(nouveauStatut) && !Status.VALIDEE.label.equals(adhesion.getStatutActuel()))
        ||
                (Status.LICENCE_GENEREE.label.equals(nouveauStatut) && !Status.LICENCE_GENEREE.label.equals(adhesion.getStatutActuel()))
        ) {
            adhesion.setInscrit(true);
            adhesion.setStatutActuel(nouveauStatut);
            adhesion.setDateChangementStatut(now());
            adhesionRepository.save(adhesion);

            byte[] attestation = pdfService.generateSynthese(adhesion.getAdherent());
            Path prePath = this.imageStorageDir.resolve(String.valueOf(adhesion.getAdherent().getId()));
            try {
                if (!Files.exists(prePath)) {
                    Files.createDirectories(prePath);
                }
                final Path targetPath = prePath.resolve("Attestation_ALOD_"+adhesion.getAdherent().getPrenom()+"_"+adhesion.getAdherent().getNom()+".pdf");
                try (InputStream in = new ByteArrayInputStream(attestation)) {
                    try (OutputStream out = Files.newOutputStream(targetPath, StandardOpenOption.CREATE)) {
                        in.transferTo(out);
                        log.error("Succes " +in);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            emailService.sendAutoMail(adhesion, "Sujet_Mail_Validation", "Corp_Mail_Validation", true);
        } else if (!Status.VALIDEE.label.equals(nouveauStatut) || !Status.LICENCE_GENEREE.label.equals(nouveauStatut)) {
            adhesion.setInscrit(false);
        }
        if (Status.ATTENTE_ADHERENT.label.equals(nouveauStatut) && Status.LISTE_ATTENTE.label.equals(adhesion.getStatutActuel())) {
            adhesion.setDateAjoutPanier(now());
            emailService.sendAutoMail(adhesion, "Sujet_Mail_Reactivation", "Corp_Mail_Reactivation", false);
        }

        if (Status.ANNULEE.label.equals(nouveauStatut) && !Status.ANNULEE.label.equals(adhesion.getStatutActuel())) {
            emailService.sendAutoMail(adhesion, "Sujet_Mail_Annulation_Manuelle", "Corp_Mail_Annulation_Manuelle", false);
        }

        adhesion.setStatutActuel(nouveauStatut);
        adhesion.setDateChangementStatut(now());
        return adhesionRepository.save(adhesion);
    }


    public Activite changeActivite(Long adhesionId, Long activiteId) {
        Adhesion inscription = adhesionRepository.findById(adhesionId).get();
        Activite activite = activiteServices.getById(activiteId);
        inscription.setActivite(activite);
        return adhesionRepository.save(inscription).getActivite();
    }

    public Adhesion addAccord(Long adhesionId, String nomAccord) {
        Adhesion inscription = adhesionRepository.findById(adhesionId).get();
        if (inscription.getAccords().stream().noneMatch(accord -> nomAccord.equals(accord.getNom()))) {
            inscription.getAccords().add(accordServices.createAccord(nomAccord));
            inscription.setStatutActuel(Status.ATTENTE_ADHERENT.label);
        }

        return adhesionRepository.save(inscription);
    }

    public List<Accord> removeAccord(Long adhesionId, String nomAccord) {
        Adhesion inscription = adhesionRepository.findById(adhesionId).get();
        if (inscription.getAccords().stream().anyMatch(accord -> nomAccord.equals(accord.getNom()))) {
            inscription.getAccords().remove(inscription.getAccords().stream().filter(accord -> nomAccord.equals(accord.getNom())).findFirst().get());
        }

        return adhesionRepository.save(inscription).getAccords();
    }

    public void deleteAdhesion(Long adhesionId) {
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();
        adhesionRepository.delete(adhesion);
        adhesion.getAdherent().getAdhesions().remove(adhesion);
        adherentServices.save(adhesion.getAdherent());
    }



    public Adhesion addModification(String userEmail, Long adhesionId) {

        User user = userServices.findByEmail(userEmail);
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();
        List<Notification> modifications = new java.util.ArrayList<>(adhesion.getDerniereModifs().stream().filter(visite -> !visite.getUser().equals(user)).toList());
        Notification nouvelleModif = new Notification();
        nouvelleModif.setDate(LocalDateTime.now());
        nouvelleModif.setUser(user);
        modifications.add(nouvelleModif);
        adhesion.setDerniereModifs(modifications);
        adhesionRepository.save(adhesion);

        return addVisite(userEmail, adhesionId);
    }

    public Adhesion addVisite(String userEmail, Long adhesionId) {
        User user = userServices.findByEmail(userEmail);
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();
        List<Notification> visites = new java.util.ArrayList<>(adhesion.getDerniereVisites().stream().filter(visite -> !visite.getUser().equals(user)).toList());
        Notification nouvelleVisite = new Notification();
        nouvelleVisite.setDate(LocalDateTime.now());
        nouvelleVisite.setUser(user);
        visites.add(nouvelleVisite);
        adhesion.setDerniereVisites(visites);

        return adhesionRepository.save(adhesion);

    }
}