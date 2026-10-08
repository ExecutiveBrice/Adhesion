package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.Chat;
import com.wild.corp.adhesion.models.ChatTarget;
import com.wild.corp.adhesion.models.PlanificationHebdomadaire;
import com.wild.corp.adhesion.models.Section;
import com.wild.corp.adhesion.models.resources.ChatConfiguration;
import com.wild.corp.adhesion.repository.ActiviteRepository;
import com.wild.corp.adhesion.repository.AdhesionRepository;
import com.wild.corp.adhesion.repository.ChatRepository;
import com.wild.corp.adhesion.repository.SectionRepository;
import com.wild.corp.adhesion.services.AdhesionServices;
import com.wild.corp.adhesion.services.ActiviteServices;
import com.wild.corp.adhesion.services.ChatConfigurationServices;
import com.wild.corp.adhesion.utils.Status;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Locale;

@RestController
@RequestMapping("/section-management")
@PreAuthorize("isAuthenticated()")
public class SectionManagementController {

    private final SectionRepository sections;
    private final ActiviteRepository activites;
    private final AdhesionRepository adhesions;
    private final ChatRepository chats;
    private final ChatConfigurationServices chatConfigurations;
    private final AdhesionServices adhesionServices;
    private final ActiviteServices activiteServices;

    public SectionManagementController(SectionRepository sections, ActiviteRepository activites,
                                       AdhesionRepository adhesions, ChatRepository chats,
                                       ChatConfigurationServices chatConfigurations, AdhesionServices adhesionServices,
                                       ActiviteServices activiteServices) {
        this.sections = sections;
        this.activites = activites;
        this.adhesions = adhesions;
        this.chats = chats;
        this.chatConfigurations = chatConfigurations;
        this.adhesionServices = adhesionServices;
        this.activiteServices = activiteServices;
    }

    public record ManagedSection(Long id, String nom, String couleur) { }
    public record ActivitySummary(Long id, String nom, String horaire, Integer tarif, Integer nbPlaces,
                                  boolean complete, boolean reinscription, String lien,
                                  long adhesionsValidees, long adhesionsEnCours, long adhesionsEnAttente) { }
    public record ActivityChanges(String nom, Integer tarif, Integer nbPlaces, Boolean complete,
                                  Boolean reinscription, String lien) { }
    public record ActivityCreation(String nom, Integer tarif, Integer nbPlaces, Integer ageMin,
                                   Integer ageMax, String genre, DayOfWeek jour, LocalTime horaireDebut,
                                   Long duree) { }
    public record MembershipSummary(Long id, String adherent, Long activiteId, String activite,
                                    String statut, LocalDate dateChangementStatut,
                                    Boolean paiementValide, Boolean documentsValides) { }
    public record StatusChange(String statut) { }

    @GetMapping("/sections")
    @Transactional
    public List<ManagedSection> managedSections(Principal principal) {
        return sections.findManagedByUsername(principal.getName()).stream()
                .map(section -> new ManagedSection(section.getId(), section.getNom(), section.getCouleur()))
                .toList();
    }

    @GetMapping("/sections/{sectionId}/activities")
    @Transactional
    public List<ActivitySummary> activities(Principal principal, @PathVariable Long sectionId) {
        requireManaged(principal, sectionId);
        return activites.findBySectionIdOrderByNomAsc(sectionId).stream().map(this::activitySummary).toList();
    }

    @PutMapping("/sections/{sectionId}/activities/{activityId}")
    @Transactional
    public ActivitySummary updateActivity(Principal principal, @PathVariable Long sectionId,
                                         @PathVariable Long activityId, @RequestBody ActivityChanges changes) {
        requireManaged(principal, sectionId);
        Activite activity = activites.findById(activityId)
                .filter(item -> item.getSection() != null && sectionId.equals(item.getSection().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activité introuvable dans cette section"));
        if (changes == null || changes.nom() == null || changes.nom().trim().isEmpty()
                || changes.nom().trim().length() > 100 || changes.tarif() == null || changes.tarif() < 0
                || changes.nbPlaces() == null || changes.nbPlaces() < 0
                || (changes.lien() != null && changes.lien().length() > 255)
                || changes.complete() == null || changes.reinscription() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les informations de l’activité sont invalides");
        }
        activity.setNom(changes.nom().trim());
        activity.setTarif(changes.tarif());
        activity.setNbPlaces(changes.nbPlaces());
        activity.setComplete(changes.complete());
        activity.setReinscription(changes.reinscription());
        activity.setLien(changes.lien() == null ? null : changes.lien().trim());
        return activitySummary(activites.save(activity));
    }

    @PostMapping("/sections/{sectionId}/activities")
    @Transactional
    public ActivitySummary createActivity(Principal principal, @PathVariable Long sectionId,
                                          @RequestBody ActivityCreation request) {
        Section section = requireManaged(principal, sectionId);
        if (request == null || request.nom() == null || request.nom().trim().isEmpty()
                || request.nom().trim().length() > 100 || request.tarif() == null || request.tarif() < 0
                || request.nbPlaces() == null || request.nbPlaces() < 1
                || request.ageMin() == null || request.ageMin() < 0 || request.ageMax() == null
                || request.ageMax() < request.ageMin() || request.ageMax() > 120
                || (request.genre() != null && !request.genre().isBlank()
                    && !List.of("Non genrée", "Féminine", "Masculine").contains(request.genre().trim()))
                || request.jour() == null
                || request.horaireDebut() == null || request.duree() == null || request.duree() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les informations de l’activité sont invalides");
        }
        Activite activity = new Activite();
        activity.setSection(section);
        activity.setNom(request.nom().trim());
        activity.setTarif(request.tarif());
        activity.setNbPlaces(request.nbPlaces());
        activity.setAgeMin(request.ageMin());
        activity.setAgeMax(request.ageMax());
        activity.setGenre(request.genre() == null || request.genre().isBlank() ? "Non genrée" : request.genre().trim());
        activity.setComplete(false);
        activity.setReinscription(false);
        PlanificationHebdomadaire schedule = new PlanificationHebdomadaire();
        schedule.setJour(request.jour());
        schedule.setHoraireDebut(request.horaireDebut());
        schedule.setDuree(request.duree());
        activity.getPlanificationsHebdomadaires().add(schedule);
        return activitySummary(activiteServices.save(activity));
    }

    @GetMapping("/sections/{sectionId}/memberships")
    @Transactional
    public Page<MembershipSummary> memberships(Principal principal, @PathVariable Long sectionId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(defaultValue = "") String search,
                                                @RequestParam(defaultValue = "") String status) {
        requireManaged(principal, sectionId);
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pagination invalide");
        }
        if (search != null && search.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La recherche est limitée à 100 caractères");
        }
        Specification<Adhesion> filter = (root, query, criteria) ->
                criteria.equal(root.get("activite").get("section").get("id"), sectionId);
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            filter = filter.and((root, query, criteria) -> criteria.or(
                    criteria.like(criteria.lower(root.get("adherent").get("nom")), pattern, '\\'),
                    criteria.like(criteria.lower(root.get("adherent").get("prenom")), pattern, '\\'),
                    criteria.like(criteria.lower(root.get("activite").get("nom")), pattern, '\\')));
        }
        if (status != null && !status.isBlank()) {
            filter = filter.and((root, query, criteria) -> criteria.equal(root.get("statutActuel"), status));
        }
        return adhesions.findAll(filter, PageRequest.of(page, size,
                        Sort.by("adherent.nom", "adherent.prenom", "id")))
                .map(this::membershipSummary);
    }

    @PutMapping("/sections/{sectionId}/memberships/{membershipId}/status")
    @Transactional
    public MembershipSummary updateMembershipStatus(Principal principal, @PathVariable Long sectionId,
                                                     @PathVariable Long membershipId, @RequestBody StatusChange change) {
        requireManaged(principal, sectionId);
        Adhesion membership = adhesions.findById(membershipId)
                .filter(item -> item.getActivite() != null && item.getActivite().getSection() != null
                        && sectionId.equals(item.getActivite().getSection().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adhésion introuvable dans cette section"));
        if (change == null || Arrays.stream(Status.values()).noneMatch(status -> status.label.equals(change.statut()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut d’adhésion invalide");
        }
        if (!Objects.equals(membership.getStatutActuel(), change.statut())) {
            adhesionServices.addModification(principal.getName(), membershipId,
                    "Mise à jour du statut de l’adhésion : " + change.statut());
            membership = adhesionServices.choisirStatut(membershipId, change.statut());
        }
        return membershipSummary(membership);
    }

    @GetMapping("/sections/{sectionId}/chats")
    @Transactional
    public List<ChatConfiguration> chats(Principal principal, @PathVariable Long sectionId) {
        requireManaged(principal, sectionId);
        return chatConfigurations.getAll(sectionId);
    }

    @PostMapping("/sections/{sectionId}/chats")
    @Transactional
    public ChatConfiguration createChat(Principal principal, @PathVariable Long sectionId,
                                        @RequestBody ChatConfiguration chat) {
        requireManaged(principal, sectionId);
        requireTargetInSection(chat, sectionId);
        return chatConfigurations.create(chat);
    }

    @PutMapping("/sections/{sectionId}/chats/{chatId}")
    @Transactional
    public ChatConfiguration updateChat(Principal principal, @PathVariable Long sectionId,
                                        @PathVariable Long chatId, @RequestBody ChatConfiguration chat) {
        requireManaged(principal, sectionId);
        requireExistingChat(sectionId, chatId);
        requireTargetInSection(chat, sectionId);
        return chatConfigurations.update(sectionId, chatId, chat);
    }

    @DeleteMapping("/sections/{sectionId}/chats/{chatId}")
    @Transactional
    public ResponseEntity<Void> deleteChat(Principal principal, @PathVariable Long sectionId,
                                           @PathVariable Long chatId) {
        requireManaged(principal, sectionId);
        requireExistingChat(sectionId, chatId);
        chatConfigurations.delete(sectionId, chatId);
        return ResponseEntity.noContent().build();
    }

    private Section requireManaged(Principal principal, Long sectionId) {
        return sections.findById(sectionId)
                .filter(section -> section.getReferents().stream()
                        .anyMatch(user -> user.getUsername().equals(principal.getName())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette section ne vous est pas confiée"));
    }

    private void requireExistingChat(Long sectionId, Long chatId) {
        Chat chat = chats.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat introuvable"));
        boolean inSection = chat.getCible() == ChatTarget.SECTION && chat.getSection() != null
                && sectionId.equals(chat.getSection().getId());
        if (!inSection) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat introuvable dans cette section");
        }
    }

    private void requireTargetInSection(ChatConfiguration chat, Long sectionId) {
        if (chat == null || !"SECTION".equals(chat.cible()) || !sectionId.equals(chat.cibleId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le chat doit être réservé à cette section");
        }
    }

    private ActivitySummary activitySummary(Activite activity) {
        return new ActivitySummary(activity.getId(), activity.getNom(), activity.getHoraire(), activity.getTarif(),
                activity.getNbPlaces(), activity.isComplete(), Boolean.TRUE.equals(activity.getReinscription()),
                activity.getLien(), activity.getAdhesions().stream().filter(Adhesion::isValide).count(),
                activity.getAdhesions().stream().filter(Adhesion::isEnCours).count(),
                activity.getAdhesions().stream().filter(Adhesion::isEnAttente).count());
    }

    private MembershipSummary membershipSummary(Adhesion membership) {
        return new MembershipSummary(membership.getId(),
                membership.getAdherent().getPrenom() + " " + membership.getAdherent().getNom(),
                membership.getActivite().getId(), membership.getActivite().getNom(),
                membership.getStatutActuel(), membership.getDateChangementStatut(),
                membership.getValidPaiementSecretariat(), membership.getValidDocumentSecretariat());
    }
}
