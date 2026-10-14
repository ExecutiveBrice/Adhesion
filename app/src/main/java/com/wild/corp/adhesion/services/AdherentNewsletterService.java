package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.repository.AdherentRepository;
import com.wild.corp.adhesion.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdherentNewsletterService {
    public record Subscription(String email, NewsletterContactService.Status status,
                               boolean canModify, boolean canSubscribe, boolean representativeEmail) { }
    private record Context(User owner, boolean canModify, boolean staffEditor, boolean representativeEmail) { }
    private static final Set<String> READ_ROLES = Set.of("ROLE_ADMIN", "ROLE_SECRETAIRE", "ROLE_BUREAU", "ROLE_MEMBRECA", "ROLE_MODERATOR");
    private static final Set<String> WRITE_ROLES = Set.of("ROLE_ADMIN", "ROLE_SECRETAIRE");
    private final AdherentRepository adherents;
    private final UserRepository users;
    private final NewsletterContactService contacts;

    public Subscription status(long id, Authentication authentication) {
        Context context = resolve(id, authentication);
        return subscription(context, contacts.getStatus(context.owner().getUsername()));
    }

    public Subscription update(long id, Authentication authentication, boolean subscribed, String expectedEmail) {
        Context context = resolve(id, authentication);
        if (context.representativeEmail()) throw new AccessDeniedException("L’abonnement newsletter doit être géré depuis la fiche du représentant lorsque son adresse est utilisée.");
        if (!context.canModify()) throw new AccessDeniedException("Modification réservée au titulaire de l’adresse, au secrétariat ou à un administrateur.");
        String email = NewsletterContactService.normalize(context.owner().getUsername());
        if (!email.equals(NewsletterContactService.normalize(expectedEmail))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L’adresse enregistrée a changé. Actualisez le statut newsletter.");
        }
        if (subscribed && !context.staffEditor() && !Boolean.TRUE.equals(context.owner().getEmailValid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Validez votre adresse e-mail avant de vous abonner à la newsletter.");
        }
        var status = subscribed ? contacts.subscribeByOwner(email) : contacts.unsubscribeFromNewsletter(email);
        return subscription(context, status);
    }

    private Subscription subscription(Context context, NewsletterContactService.Status status) {
        return new Subscription(NewsletterContactService.normalize(context.owner().getUsername()), status,
                context.canModify(), context.canModify() && (context.staffEditor() || Boolean.TRUE.equals(context.owner().getEmailValid())),
                context.representativeEmail());
    }

    private Context resolve(long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) throw new AccessDeniedException("Connexion requise.");
        User viewer = users.findByUsername(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Compte introuvable."));
        Adherent adherent = adherents.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adhérent introuvable."));
        boolean representative = Boolean.TRUE.equals(adherent.getEmailRepresentant()) && adherent.getRepresentant() != null;
        User owner = representative ? adherent.getRepresentant().getUser() : adherent.getUser();
        boolean ownsAddress = owner != null && owner.getId() != null && owner.getId().equals(viewer.getId());
        boolean ownMember = adherent.getUser() != null && viewer.getId() != null && viewer.getId().equals(adherent.getUser().getId());
        boolean sameFamily = viewer.getAdherent() != null && viewer.getAdherent().getTribu() != null && adherent.getTribu() != null
                && adherent.getTribu().getId() != null && adherent.getTribu().getId().equals(viewer.getAdherent().getTribu().getId());
        boolean staff = authentication.getAuthorities().stream().anyMatch(a -> READ_ROLES.contains(a.getAuthority()));
        boolean staffEditor = authentication.getAuthorities().stream().anyMatch(a -> WRITE_ROLES.contains(a.getAuthority()));
        if (!ownsAddress && !ownMember && !sameFamily && !staff) throw new AccessDeniedException("Accès à cet adhérent refusé.");
        if (owner == null || owner.getUsername() == null || owner.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Aucune adresse e-mail enregistrée pour cet adhérent.");
        }
        return new Context(owner, !representative && (ownsAddress || staffEditor), staffEditor, representative);
    }
}
