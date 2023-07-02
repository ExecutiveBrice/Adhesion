package com.gestion.adhesion.services;

import com.gestion.adhesion.models.*;
import com.gestion.adhesion.repository.AdhesionRepository;
import com.gestion.adhesion.repository.PaiementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.time.LocalDate.now;

@Service
public class AdhesionServices {

    @Autowired
    AdhesionRepository adhesionRepository;

    @Autowired
    PaiementRepository paiementRepository;
    @Autowired
    TribuServices tribuServices;
    @Autowired
    AdherentServices adherentServices;
    @Autowired
    ActiviteServices activiteServices;

    @Autowired
    EmailService emailService;


    public List<AdhesionLite> getAllLite(){
        List<Adhesion> adhesions =adhesionRepository.findAll();

        return adhesions.stream().map(adhesion -> AdhesionLite.builder().id(adhesion.getId())
                .adherent(AdherentLite.builder()
                        .id(adhesion.getAdherent().getId())
                        .prenomNom(adhesion.getAdherent().getPrenom()+adhesion.getAdherent().getNom())
                        .nom(adhesion.getAdherent().getNom())
                        .prenom(adhesion.getAdherent().getPrenom())
                        .email(adhesion.getAdherent().getTribu().getAdherents().stream().filter(Adherent::isReferent).findFirst().get().getEmail())
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
                .build()).toList();
    }


    public void deletePaiement(Long adhesionId, Long paiementId){
        Adhesion adhesion =  adhesionRepository.findById(adhesionId).get();
        adhesion.getPaiements().remove(paiementRepository.findById(paiementId).get());
        adhesionRepository.save(adhesion);
        paiementRepository.deleteById(paiementId);
    }

    public Adhesion savePaiement(Long adhesionId, Paiement paiement){
         Adhesion adhesion = adhesionRepository.findById(adhesionId).get();

        List<Paiement> paiements = adhesion.getPaiements();
        if(paiements.contains(paiement)) {
            paiements.stream().filter(paiement1 -> paiement1.getId().equals(paiement.getId()))
                    .forEach(paiement1 -> {
                                paiement1.setTypeReglement(paiement.getTypeReglement());
                                paiement1.setMontant(paiement.getMontant());
                                paiement1.setDateReglement(paiement.getDateReglement());
                            }
                    );

        }else{
            paiement.setDateReglement(now());
            paiement.setTypeReglement("HelloAsso");
            paiement.setMontant(adhesion.getActivite().getTarif());
            paiements.add(paiement);
        }

        return adhesionRepository.save(adhesion);
    }
    public void transfertPaiement(){
        List<Adhesion> adhesions =adhesionRepository.findAll();
        System.out.println("transfertPaiement");
        adhesions.forEach(adhesion -> {
            if(adhesion.getDateReglement() != null && adhesion.getPaiements().size() ==0){
                System.out.println(adhesion.getId());
                Paiement paiement = new Paiement();
                paiement.setDateReglement(adhesion.getDateReglement());
                paiement.setMontant(adhesion.getTarif());
                paiement.setTypeReglement(adhesion.getTypeReglement());
                adhesion.getPaiements().add(paiement);

                adhesionRepository.save(adhesion);
            }
        });
    }

    public List<Adhesion> getAll(){
        List<Adhesion> adhesions =adhesionRepository.findAll();


        adhesions.forEach(adhesion -> {

            Adherent adherentRef =   adhesion.getAdherent().getTribu().getAdherents().stream().filter(Adherent::isReferent).findFirst().get();
            if(adhesion.getAdherent().isEmailReferent()){
                adhesion.getAdherent().setEmail(adherentRef.getEmail());
            }
            if(adhesion.getAdherent().isAdresseReferent()){
                adhesion.getAdherent().setAdresse(adherentRef.getAdresse());
            }
            if(adhesion.getAdherent().isTelephoneReferent()){
                adhesion.getAdherent().setTelephone(adherentRef.getTelephone());
            }

        });

        return adhesions;

    }

    public Adhesion update(Adhesion frontAdhesion){
        Adhesion dataAdhesion = adhesionRepository.findById(frontAdhesion.getId()).get();
        dataAdhesion.setTypeReglement(frontAdhesion.getTypeReglement());

        Accord frontaccordImage = frontAdhesion.getAccords().stream().filter(accord -> "Reglement Interieur".equals(accord.getNom())).findFirst().get();
        Accord dataaccordImage = dataAdhesion.getAccords().stream().filter(accord -> "Reglement Interieur".equals(accord.getNom())).findFirst().get();
        dataaccordImage.setEtat(frontaccordImage.getEtat());
        dataaccordImage.setDatePassage(frontaccordImage.getDatePassage());


        if(frontAdhesion.getAccords().stream().anyMatch(accord -> "Attestation Sante".equals(accord.getNom()))) {
            Accord frontaccordAS = frontAdhesion.getAccords().stream().filter(accord -> "Attestation Sante".equals(accord.getNom())).findFirst().get();
            Accord dataaccordAS = dataAdhesion.getAccords().stream().filter(accord -> "Attestation Sante".equals(accord.getNom())).findFirst().get();
            dataaccordAS.setEtat(frontaccordAS.getEtat());
            dataaccordAS.setDatePassage(frontaccordAS.getDatePassage());
        }

        if(frontAdhesion.getAccords().stream().anyMatch(accord -> "Vie du Club".equals(accord.getNom()))) {
            Accord frontaccordVC = frontAdhesion.getAccords().stream().filter(accord -> "Vie du Club".equals(accord.getNom())).findFirst().get();
            Accord dataaccordVC = dataAdhesion.getAccords().stream().filter(accord -> "Vie du Club".equals(accord.getNom())).findFirst().get();
            dataaccordVC.setEtat(frontaccordVC.getEtat());
            dataaccordVC.setDatePassage(frontaccordVC.getDatePassage());
        }

        if(frontAdhesion.getAccords().stream().anyMatch(accord -> "Autorisation Parentale".equals(accord.getNom()))) {
            Accord frontaccordAP = frontAdhesion.getAccords().stream().filter(accord -> "Autorisation Parentale".equals(accord.getNom())).findFirst().get();
            Accord dataaccordAP = dataAdhesion.getAccords().stream().filter(accord -> "Autorisation Parentale".equals(accord.getNom())).findFirst().get();
            dataaccordAP.setEtat(frontaccordAP.getEtat());
            dataaccordAP.setDatePassage(frontaccordAP.getDatePassage());
        }

        if(frontAdhesion.getAccords().stream().anyMatch(accord -> "Prise en Charge".equals(accord.getNom()))) {
            Accord frontaccordPC = frontAdhesion.getAccords().stream().filter(accord -> "Prise en Charge".equals(accord.getNom())).findFirst().get();
            Accord dataaccordPC = dataAdhesion.getAccords().stream().filter(accord -> "Prise en Charge".equals(accord.getNom())).findFirst().get();
            dataaccordPC.setEtat(frontaccordPC.getEtat());
            dataaccordPC.setDatePassage(frontaccordPC.getDatePassage());
        }
        adhesionRepository.save(dataAdhesion);
        return choisirStatut(dataAdhesion.getId(), "Attente validation secrétariat");
    }

    public Adhesion saveUnique(Adhesion adhesion){
        return adhesionRepository.save(adhesion);
    }

    public List<Adhesion> save(List<Adhesion> adhesions){

        adhesions.stream().forEach(adhesion -> {

            adhesion.setId(null);
            adhesion.setRappel(false);
            adhesion.setDateAjoutPanier(now());
            adhesion.setAdherent(adherentServices.getBasicById(adhesion.getAdherent().getId()));
            adhesion.setActivite(activiteServices.getById(adhesion.getActivite().getId()));

            adhesion.getAccords().add(new Accord("Reglement Interieur"));

            if(adhesion.getActivite().isCertificatMedical() && adhesion.getActivite().getDureeVieCertif() > 1) {
                adhesion.getAccords().add(new Accord("Attestation Sante"));
            }

            if(adhesion.getActivite().isVieClub()) {
                adhesion.getAccords().add(new Accord("Vie du Club"));
            }

            if(adhesion.getActivite().isAutorisationParentale() && adhesion.getAdherent().isMineur()) {
                adhesion.getAccords().add(new Accord("Autorisation Parentale"));
            }

            if(adhesion.getActivite().isPriseEnCharge() && adhesion.getAdherent().isMineur()) {
                adhesion.getAccords().add(new Accord("Prise en Charge"));
            }

            if(adhesion.getActivite().getAdhesions().stream().filter(adh -> adh.isEnCours() || adh.isValide()).count() >= adhesion.getActivite().getNbPlaces()){
                adhesion.setStatutActuel("Sur liste d'attente");
                Optional<Adhesion> adhesionAttente = adhesion.getActivite().getAdhesions().stream().filter(adh -> !adh.isEnCours() && !adh.isValide())
                        .max(Comparator.comparing(Adhesion::getPosition));
                if(adhesionAttente.isPresent()){
                    adhesion.setPosition(adhesionAttente.get().getPosition()+1);
                }else{
                    adhesion.setPosition(1);
                }

            }else{
                adhesion.setStatutActuel("Attente validation adhérent");
            }

            adhesionRepository.save(adhesion);
        });

        return adhesions;
    }



    public Adhesion updateDocumentsSecretariat(Long adhesionId, Boolean statut){
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();
        adhesion.setValidDocumentSecretariat(statut);
        return adhesionRepository.save(adhesion);
    }

    public Adhesion updatePaiementSecretariat(Long adhesionId, Boolean statut){
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();

        adhesion.setValidPaiementSecretariat(statut);
        return adhesionRepository.save(adhesion);
    }


    public Adhesion updateFlag(Long adhesionId, Boolean statut){
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();
        adhesion.setFlag(statut);
        return adhesionRepository.save(adhesion);
    }

    public Adhesion enregistrerRemarque(Long adhesionId, String remarqueSecretariat){
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();
        adhesion.setRemarqueSecretariat(remarqueSecretariat);
        return adhesionRepository.save(adhesion);
    }

    public Adhesion choisirStatut(Long adhesionId, String nouveauStatut){
        Adhesion adhesion = adhesionRepository.findById(adhesionId).get();

        if("Validée".equals(nouveauStatut) && !"Validée".equals(adhesion.getStatutActuel())){
            adhesion.setInscrit(true);
            emailService.sendAutoMail(adhesion, "Sujet_Mail_Validation", "Corp_Mail_Validation");
        }else if(!"Validée".equals(nouveauStatut)){
            adhesion.setInscrit(false);
        }

        if("Attente validation adhérent".equals(nouveauStatut) && "Sur liste d'attente".equals(adhesion.getStatutActuel())){
            emailService.sendAutoMail(adhesion, "Sujet_Mail_Reactivation", "Corp_Mail_Reactivation");
        }

        if("Annulée".equals(nouveauStatut) && !"Annulée".equals(adhesion.getStatutActuel())){
            emailService.sendAutoMail(adhesion, "Sujet_Mail_Annulation_Manuelle", "Corp_Mail_Annulation_Manuelle");
        }

        adhesion.setStatutActuel(nouveauStatut);
        adhesion.setDateChangementStatut(now());
        return adhesionRepository.save(adhesion);
    }


    public Activite changeActivite(Long adhesionId, Long activiteId){
        Adhesion inscription = adhesionRepository.findById(adhesionId).get();
        Activite activite = activiteServices.getById(activiteId);
        inscription.setActivite(activite);
        return adhesionRepository.save(inscription).getActivite();
    }

    public List<Accord> addAccord(Long adhesionId, String nomAccord){
        Adhesion inscription = adhesionRepository.findById(adhesionId).get();

        if(inscription.getAccords().stream().noneMatch(accord -> nomAccord.equals(accord.getNom()))) {
            Accord newAccord = new Accord(nomAccord);
            inscription.getAccords().add(newAccord);
        }

        return adhesionRepository.save(inscription).getAccords();
    }

    public List<Accord> removeAccord(Long adhesionId, String nomAccord){
        Adhesion inscription = adhesionRepository.findById(adhesionId).get();
        if(inscription.getAccords().stream().anyMatch(accord -> nomAccord.equals(accord.getNom()))) {
            inscription.getAccords().remove(inscription.getAccords().stream().filter(accord -> nomAccord.equals(accord.getNom())).findFirst().get());
        }

        return adhesionRepository.save(inscription).getAccords();
    }

    public Adhesion updateTypePaiement(Long adhesionId, String typePaiement){
        Adhesion inscription = adhesionRepository.findById(adhesionId).get();
        inscription.setTypeReglement(typePaiement);
        return adhesionRepository.save(inscription);
    }

    public void deleteAdhesion(Long adhesionId){
        adhesionRepository.deleteById(adhesionId);
    }



}