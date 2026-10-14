import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NgbConfig } from '@ng-bootstrap/ng-bootstrap';
import { of, Subject, throwError } from 'rxjs';
import { MaillingComponent } from './mailling.component';
import { MailService, NewsletterCampaign } from '../../_services/mail.service';
import { ActiviteService } from '../../_services/activite.service';
import { ParamService } from '../../_services/param.service';
import { ToastService } from '../../_services/toast.service';
import { ApiRenderService } from '../../_services/api-render.service';
import { Historique } from '../../models/historique';

const campaign: NewsletterCampaign = { id: 7, name: 'Octobre', subject: 'Actualités ALOD',
  previewText: 'Cette semaine', htmlContent: '<h1>Bonjour</h1>', listId: 42,
  listName: 'Newsletter ALOD', subscribers: 120, brevoStatus: 'draft', confirmationVersion: 'v1' };
const result = (status: string): Historique => Object.assign(new Historique(), {
  id: 1, campaignId: 7, campaignStatus: status, brevoStatus: status === 'COMPLETED' ? 'sent' : 'draft'
});

describe('Mailing newsletter Brevo', () => {
  let fixture: ComponentFixture<MaillingComponent>;
  let component: MaillingComponent;
  let mail: jasmine.SpyObj<MailService>;

  function refresh() { TestBed.inject(ApiRenderService).notify(); fixture.detectChanges(); }

  beforeEach(() => {
    localStorage.removeItem('alod-newsletter-pending');
    mail = jasmine.createSpyObj('MailService', ['getNewsletterDrafts', 'getNewsletterCampaign',
      'sendNewsletterCampaign', 'getNewsletterStatus', 'getHistorique', 'sendTemplate', 'sendMail']);
    mail.getNewsletterDrafts.and.returnValue(of([campaign]));
    mail.getNewsletterCampaign.and.returnValue(of(campaign));
    mail.getHistorique.and.returnValue(of([]));
    TestBed.configureTestingModule({ imports: [MaillingComponent], providers: [provideRouter([]),
      { provide: MailService, useValue: mail },
      { provide: ActiviteService, useValue: { getAll: () => of([]), getAllNm1: () => of([]) } },
      { provide: ParamService, useValue: {} },
      { provide: ToastService, useValue: jasmine.createSpyObj('ToastService', ['success', 'warning', 'error']) }
    ] });
    TestBed.inject(NgbConfig).animation = false;
    fixture = TestBed.createComponent(MaillingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  afterEach(() => { localStorage.removeItem('alod-newsletter-pending'); fixture.destroy(); });

  it('shows subject, isolated preview, audience and a recap before sending', () => {
    mail.sendNewsletterCampaign.and.returnValue(of(result('ACCEPTED')));
    component.openNewsletter(); component.selectNewsletterCampaign(7); refresh();
    expect(fixture.nativeElement.textContent).toContain('Actualités ALOD');
    expect(fixture.nativeElement.textContent).toContain('Newsletter ALOD');
    expect(fixture.nativeElement.querySelector('iframe').getAttribute('sandbox')).toBe('');
    component.confirmNewsletter();
    expect(mail.sendNewsletterCampaign).not.toHaveBeenCalled();
    component.newsletterConfirming = true; refresh();
    expect(fixture.nativeElement.textContent).toContain('Confirmer le lancement');
    component.confirmNewsletter(); refresh();
    expect(mail.sendNewsletterCampaign).toHaveBeenCalledWith(campaign);
    expect(fixture.nativeElement.textContent).toContain('Demande acceptée');
    expect(fixture.nativeElement.textContent).not.toContain('Campagne terminée');
    expect(mail.sendTemplate).not.toHaveBeenCalled();
  });

  it('separates individual mail and campaigns while preserving the current mail draft', () => {
    component.subject = 'Mon objet';
    component.content = '<p>Mon contenu conservé</p>';
    component.role_bouton = true;
    const role = component.roleListe.at(0).get('horaires') as any;
    role.at(0).get('checked').setValue(true);
    refresh();
    const tabs: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('[role="tab"]'));
    expect(tabs.map(tab => tab.textContent?.trim())).toEqual(['E-mails individuels', 'Campagnes Brevo']);
    expect(fixture.nativeElement.querySelectorAll('.mailing-panel').length).toBe(3);
    expect(fixture.nativeElement.querySelector('angular-editor')).not.toBeNull();
    tabs[1].click(); refresh();
    expect(fixture.nativeElement.querySelector('.history-panel')).toBeNull();
    expect(fixture.nativeElement.querySelector('.recipients-panel')).toBeNull();
    expect(fixture.nativeElement.querySelector('angular-editor')).toBeNull();
    expect(fixture.nativeElement.querySelectorAll('.campaign-card').length).toBe(1);
    expect(fixture.nativeElement.querySelector('iframe')).toBeNull();
    expect(mail.getNewsletterDrafts).toHaveBeenCalled();
    tabs[0].click(); refresh();
    expect(component.subject).toBe('Mon objet');
    expect(component.content).toContain('Mon contenu conservé');
    expect(role.at(0).get('checked').value).toBeTrue();
    expect(fixture.nativeElement.querySelector('.history-panel')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#mail-subject').value).toBe('Mon objet');
  });

  it('opens and closes campaign cards with a fresh preview and no send on expansion', () => {
    component.openNewsletter(); refresh();
    let toggle: HTMLButtonElement = fixture.nativeElement.querySelector('.campaign-toggle');
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    toggle.click(); refresh();
    toggle = fixture.nativeElement.querySelector('.campaign-toggle');
    expect(toggle.getAttribute('aria-expanded')).toBe('true');
    expect(fixture.nativeElement.querySelector('.campaign-body iframe')).not.toBeNull();
    expect(mail.getNewsletterCampaign).toHaveBeenCalledWith(7);
    expect(mail.sendNewsletterCampaign).not.toHaveBeenCalled();
    toggle.click(); refresh();
    expect(fixture.nativeElement.querySelector('.campaign-body')).toBeNull();
    expect(fixture.nativeElement.querySelector('.campaign-toggle').getAttribute('aria-expanded')).toBe('false');
  });

  it('keeps a confirmed launch disabled when its card is reopened', () => {
    mail.sendNewsletterCampaign.and.returnValue(of(result('ACCEPTED')));
    component.openNewsletter(); component.toggleNewsletterCampaign(7);
    component.newsletterConfirming = true; component.confirmNewsletter();
    component.toggleNewsletterCampaign(7); component.toggleNewsletterCampaign(7); refresh();
    expect(component.newsletterResult?.campaignStatus).toBe('ACCEPTED');
    const prepare = Array.from(fixture.nativeElement.querySelectorAll('.campaign-body button'))
      .find(button => (button as HTMLButtonElement).textContent?.includes('Préparer')) as HTMLButtonElement;
    expect(prepare.disabled).toBeTrue();
    expect(mail.sendNewsletterCampaign).toHaveBeenCalledTimes(1);
  });

  function inspectLayout(width: number, inspect: (document: Document) => void) {
    const frame = document.createElement('iframe');
    frame.style.cssText = `position:absolute;left:-10000px;width:${width}px;height:900px;border:0`;
    document.body.appendChild(frame);
    try {
      const target = frame.contentDocument!;
      const styles = target.createElement('style');
      styles.textContent = Array.from(document.styleSheets).map(sheet => {
        try { return Array.from(sheet.cssRules).map(rule => rule.cssText).join('\n'); }
        catch { return ''; }
      }).join('\n');
      target.head.appendChild(styles);
      target.body.appendChild(fixture.nativeElement.cloneNode(true));
      inspect(target);
    } finally { frame.remove(); }
  }

  it('stacks the three individual sections on mobile without horizontal overflow', () => {
    component.historiques = [Object.assign(new Historique(), { id: 3, subject: 'Un objet de mail très long pour vérifier la mise en page mobile',
      listing: 'DestinatairesAvecUnNomTrèsLongSansEspacesPourVérifierLeRetourÀLaLigne', dateEnvoi: new Date(), nombre: 120, echecs: 0 })];
    refresh();
    inspectLayout(375, target => {
      const panels = Array.from(target.querySelectorAll('.mailing-panel')) as HTMLElement[];
      const positions = panels.map(panel => panel.getBoundingClientRect());
      expect(positions[1].top).toBeGreaterThan(positions[0].bottom);
      expect(positions[2].top).toBeGreaterThan(positions[1].bottom);
      expect(target.documentElement.scrollWidth).toBeLessThanOrEqual(375);
      expect(target.defaultView!.getComputedStyle(target.querySelector('.mailing-history td')!).display).toBe('grid');
    });
    inspectLayout(1280, target => {
      const panels = Array.from(target.querySelectorAll('.mailing-panel')) as HTMLElement[];
      const positions = panels.map(panel => panel.getBoundingClientRect());
      expect(Math.round(positions[0].top)).toBe(Math.round(positions[1].top));
      expect(Math.round(positions[1].top)).toBe(Math.round(positions[2].top));
      expect(target.documentElement.scrollWidth).toBeLessThanOrEqual(1280);
    });
  });

  it('fits an open campaign card on a narrow mobile screen without history', () => {
    component.openNewsletter(); component.selectNewsletterCampaign(7); refresh();
    inspectLayout(320, target => {
      expect(target.querySelector('.history-panel')).toBeNull();
      expect(target.documentElement.scrollWidth).toBeLessThanOrEqual(320);
      const card = target.querySelector('.campaign-card')!.getBoundingClientRect();
      const preview = target.querySelector('.campaign-preview')!.getBoundingClientRect();
      expect(preview.width).toBeGreaterThan(0);
      expect(preview.right).toBeLessThanOrEqual(card.right);
    });
  });

  it('keeps the convocation route available among individual mails', () => {
    mail.sendTemplate.and.returnValue(of(Object.assign(new Historique(), { id: 3, subject: 'Convocation' })));
    component.template = 6; refresh();
    expect(fixture.nativeElement.querySelector('angular-editor')).toBeNull();
    fixture.nativeElement.querySelector('.send-message').click(); refresh();
    expect(mail.sendTemplate).toHaveBeenCalledWith(jasmine.any(Array), 6);
    expect(mail.sendNewsletterCampaign).not.toHaveBeenCalled();
  });

  it('prevents repeated clicks and reconciles a lost response before allowing any action', () => {
    const request = new Subject<Historique>();
    mail.sendNewsletterCampaign.and.returnValue(request);
    mail.getNewsletterStatus.and.returnValue(of(result('UNKNOWN')));
    component.selectNewsletterCampaign(7); component.newsletterConfirming = true;
    component.confirmNewsletter(); component.confirmNewsletter();
    expect(mail.sendNewsletterCampaign).toHaveBeenCalledTimes(1);
    request.error({ status: 0 });
    expect(mail.getNewsletterStatus).toHaveBeenCalledWith(7);
    expect(component.pendingCampaignId).toBe(7);
    component.newsletterConfirming = true; component.confirmNewsletter();
    expect(mail.sendNewsletterCampaign).toHaveBeenCalledTimes(1);
    expect(localStorage.getItem('alod-newsletter-pending')).toBe('7');
  });

  it('keeps uncertain sends locked when status is unavailable and can observe completion later', () => {
    component.pendingCampaignId = 7;
    mail.getNewsletterStatus.and.returnValue(throwError(() => ({ status: 502 })));
    component.checkNewsletterStatus(7);
    expect(component.pendingCampaignId).toBe(7);
    expect(component.newsletterError).toContain('Aucun nouvel envoi');
    mail.getNewsletterStatus.and.returnValue(of(result('COMPLETED')));
    component.checkNewsletterStatus(7);
    expect(component.campaignStatusLabel(component.newsletterResult?.campaignStatus)).toContain('terminée');
  });

  it('releases loading and explains a pending request timeout', () => {
    mail.getNewsletterDrafts.and.returnValue(throwError(() => ({ name: 'TimeoutError' })));
    component.openNewsletter();
    expect(component.newsletterLoading).toBeFalse();
    expect(component.newsletterError).toContain('30 secondes');
    expect(mail.sendNewsletterCampaign).not.toHaveBeenCalled();
  });

  it('shows a backend retrieval failure without hiding its diagnostic', () => {
    mail.getNewsletterDrafts.and.returnValue(throwError(() => ({ status: 502,
      error: { detail: 'Brevo : lecture des brouillons impossible (HTTP 401). Vérifiez la clé API Brevo.' } })));
    component.openNewsletter();
    expect(component.newsletterLoading).toBeFalse();
    expect(component.newsletterError).toContain('HTTP 401');
    expect(component.newsletterError).toContain('clé API Brevo');
  });

  it('does not resend after disabled sending or a changed confirmation', () => {
    mail.sendNewsletterCampaign.and.returnValue(throwError(() => ({ status: 503 })));
    mail.getNewsletterStatus.and.returnValue(of(result('NOT_REQUESTED')));
    component.selectNewsletterCampaign(7); component.newsletterConfirming = true; component.confirmNewsletter();
    expect(component.newsletterError).toContain('désactivé');
    expect(component.selectedCampaign).toBeUndefined();
    expect(mail.sendNewsletterCampaign).toHaveBeenCalledTimes(1);
  });
});
