import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from "@angular/common/http";
import {Email, Groupe} from '../models';
import { environment } from 'src/environments/environment';
import {Historique} from "../models/historique";
import { timeout } from 'rxjs';

export interface NewsletterCampaign {
  id: number;
  name: string;
  subject: string;
  previewText: string;
  htmlContent: string;
  listId: number;
  listName: string;
  subscribers: number | null;
  brevoStatus: string;
  confirmationVersion: string;
}

export interface NewsletterSyncProgress {
  running: boolean;
  total: number;
  processed: number;
  subscribed: number;
  resubscribed: number;
  alreadySubscribed: number;
  unresolved: number;
  ignored: number;
  error: string;
}

export interface AdherentNewsletterSubscription {
  email: string;
  status: 'NOT_FOUND' | 'NOT_SUBSCRIBED' | 'SUBSCRIBED' | 'UNSUBSCRIBED' | 'BLOCKED';
  canModify: boolean;
  canSubscribe: boolean;
  representativeEmail: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class MailService {
  private http = inject(HttpClient);

  apiUrl = environment.server;
  // Query parameter recognized by Angular's worker, without an extra CORS header.
  private readonly newsletterOptions = { params: new HttpParams().set('ngsw-bypass', 'true') };
  private readonly newsletterTimeoutMs = 30000;

  getAdherentNewsletter(id: number) {
    return this.http.get<AdherentNewsletterSubscription>(this.apiUrl + '/adherent/' + id + '/newsletter', this.newsletterOptions)
      .pipe(timeout(this.newsletterTimeoutMs));
  }

  updateAdherentNewsletter(id: number, subscribed: boolean, expectedEmail: string) {
    return this.http.put<AdherentNewsletterSubscription>(this.apiUrl + '/adherent/' + id + '/newsletter',
      { subscribed, expectedEmail }, this.newsletterOptions).pipe(timeout(this.newsletterTimeoutMs));
  }

  synchronizeNewsletterContacts() {
    return this.http.post<NewsletterSyncProgress>(this.apiUrl + '/newsletter/contacts/sync', {}, this.newsletterOptions)
      .pipe(timeout(this.newsletterTimeoutMs));
  }

  getNewsletterContactSyncStatus() {
    return this.http.get<NewsletterSyncProgress>(this.apiUrl + '/newsletter/contacts/sync', this.newsletterOptions)
      .pipe(timeout(this.newsletterTimeoutMs));
  }

  getNewsletterDrafts() {
    return this.http.get<NewsletterCampaign[]>(this.apiUrl + '/newsletter/campaigns', this.newsletterOptions)
      .pipe(timeout(this.newsletterTimeoutMs));
  }
  getNewsletterCampaign(id: number) {
    return this.http.get<NewsletterCampaign>(this.apiUrl + '/newsletter/campaigns/' + id, this.newsletterOptions)
      .pipe(timeout(this.newsletterTimeoutMs));
  }
  sendNewsletterCampaign(campaign: NewsletterCampaign) {
    return this.http.post<Historique>(this.apiUrl + '/newsletter/campaigns/' + campaign.id + '/send',
      { confirmationVersion: campaign.confirmationVersion }, this.newsletterOptions)
      .pipe(timeout(this.newsletterTimeoutMs));
  }
  getNewsletterStatus(id: number) {
    return this.http.get<Historique>(this.apiUrl + '/newsletter/campaigns/' + id + '/status', this.newsletterOptions)
      .pipe(timeout(this.newsletterTimeoutMs));
  }

  sendTemplate(maillingListe:Groupe[], templateId: number) {
    let params = new HttpParams().set('templateId', '' + templateId + '');
    return this.http.post<Historique>(this.apiUrl + "/email/sendTemplate", maillingListe, {params, responseType: 'json' })
  }


  sendMail(email: Email) {
    return this.http.post<Historique>(this.apiUrl + "/email/", email, { responseType: 'json' })
  }

  isInProgress() {
    return this.http.get<boolean>(this.apiUrl + "/email/isInProgress", { responseType: 'json' })
  }


  getHistorique() {
    return this.http.get<Historique[]>(this.apiUrl + "/email/historique", { responseType: 'json' })
  }
}
