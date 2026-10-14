package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.repository.AdherentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.util.LinkedHashSet;
import java.util.concurrent.Executor;

/** One running batch per application instance. Progress contains no contact addresses. */
@Service
public class NewsletterSyncService {
    public record Progress(boolean running, int total, int processed, int subscribed, int resubscribed,
                           int alreadySubscribed, int unresolved, int ignored, String error) { }

    private final AdherentRepository adherents;
    private final NewsletterContactService contacts;
    private final Executor executor;
    private volatile Progress progress = new Progress(false, 0, 0, 0, 0, 0, 0, 0, "");

    public NewsletterSyncService(AdherentRepository adherents, NewsletterContactService contacts,
                                @Qualifier("newsletterSyncExecutor") Executor executor) {
        this.adherents = adherents;
        this.contacts = contacts;
        this.executor = executor;
    }

    public Progress status() { return progress; }

    public synchronized Progress start() {
        if (progress.running()) return progress;
        progress = new Progress(true, 0, 0, 0, 0, 0, 0, 0, "");
        try { executor.execute(this::run); }
        catch (RuntimeException exception) {
            finish("Le traitement n’a pas pu démarrer. Réessayez plus tard.");
        }
        return progress;
    }

    private void run() {
        try {
            var emails = new LinkedHashSet<String>();
            int ignored = 0;
            for (String email : adherents.findAllContactEmails()) {
                try {
                    if (!emails.add(NewsletterContactService.normalize(email))) ignored++;
                } catch (IllegalArgumentException exception) { ignored++; }
            }
            progress = new Progress(true, emails.size(), 0, 0, 0, 0, 0, ignored, "");
            for (String email : emails) {
                var result = contacts.subscribeFromAdministration(email);
                var p = progress;
                progress = new Progress(true, p.total(), p.processed() + 1,
                        p.subscribed() + (result == NewsletterContactService.AdminSubscription.SUBSCRIBED ? 1 : 0),
                        p.resubscribed() + (result == NewsletterContactService.AdminSubscription.RESUBSCRIBED ? 1 : 0),
                        p.alreadySubscribed() + (result == NewsletterContactService.AdminSubscription.ALREADY_SUBSCRIBED ? 1 : 0),
                        p.unresolved() + (result == NewsletterContactService.AdminSubscription.STILL_UNSUBSCRIBED ? 1 : 0),
                        p.ignored(), "");
            }
            finish("");
        } catch (NewsletterApiException exception) {
            finish("Synchronisation interrompue : erreur Brevo (HTTP " + exception.getStatusCode() + "). " +
                    (exception.getStatusCode() == 429
                            ? "Réessayez après " + Math.max(1, exception.getRetryAfter().toSeconds()) + " secondes."
                            : "Les contacts déjà traités sont conservés. Vous pouvez relancer la synchronisation."));
        } catch (IllegalStateException exception) {
            finish("Vérifiez la configuration BREVO_APIKEY_PRIVATE et BREVO_NEWSLETTER_LIST_ID.");
        } catch (RuntimeException exception) {
            finish("Synchronisation interrompue. Les contacts déjà traités sont conservés ; consultez la configuration et réessayez.");
        }
    }

    private void finish(String error) {
        var p = progress;
        progress = new Progress(false, p.total(), p.processed(), p.subscribed(), p.resubscribed(),
                p.alreadySubscribed(), p.unresolved(), p.ignored(), error);
    }
}
