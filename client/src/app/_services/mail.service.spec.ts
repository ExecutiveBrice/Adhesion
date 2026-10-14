import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MailService, NewsletterCampaign } from './mail.service';

describe('Newsletter HTTP transport', () => {
  let service: MailService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(MailService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('loads the saved member newsletter and submits the explicit preference without an arbitrary target email', () => {
    service.getAdherentNewsletter(7).subscribe();
    const status = http.expectOne(request => request.url.endsWith('/adherent/7/newsletter'));
    expect(status.request.method).toBe('GET');
    expect(status.request.params.get('ngsw-bypass')).toBe('true'); status.flush({});
    service.updateAdherentNewsletter(7, false, 'owner@example.org').subscribe();
    const update = http.expectOne(request => request.url.endsWith('/adherent/7/newsletter'));
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual({ subscribed: false, expectedEmail: 'owner@example.org' });
    update.flush({});
  });

  it('bypasses the Angular worker for draft, preview, status and launch requests', () => {
    service.getNewsletterDrafts().subscribe();
    service.getNewsletterCampaign(7).subscribe();
    service.getNewsletterStatus(7).subscribe();
    service.sendNewsletterCampaign({ id: 7, confirmationVersion: 'v1' } as NewsletterCampaign).subscribe();
    const requests = http.match(request => request.url.startsWith(service.apiUrl + '/newsletter/campaigns'));
    expect(requests.length).toBe(4);
    for (const request of requests) {
      expect(request.request.params.get('ngsw-bypass')).toBe('true');
      expect(request.request.headers.has('ngsw-bypass')).toBeFalse();
      if (request.request.method === 'POST') expect(request.request.body).toEqual({ confirmationVersion: 'v1' });
      request.flush(request.request.url.endsWith('campaigns') ? [] : {});
    }
  });

  it('terminates a draft request that stays pending instead of leaving the UI loading', fakeAsync(() => {
    let error: any;
    service.getNewsletterDrafts().subscribe({ error: failure => error = failure });
    const request = http.expectOne(req => req.url.endsWith('/newsletter/campaigns'));
    tick(29999);
    expect(error).toBeUndefined();
    tick(1);
    expect(error.name).toBe('TimeoutError');
    expect(request.cancelled).toBeTrue();
  }));

  it('never retries a launch when the response times out', fakeAsync(() => {
    let error: any;
    service.sendNewsletterCampaign({ id: 7, confirmationVersion: 'v1' } as NewsletterCampaign)
      .subscribe({ error: failure => error = failure });
    const request = http.expectOne(req => req.url.endsWith('/7/send'));
    tick(30000);
    expect(error.name).toBe('TimeoutError');
    expect(request.cancelled).toBeTrue();
    http.expectNone(req => req.url.endsWith('/7/send'));
  }));

  it('keeps transactional template requests unchanged', () => {
    service.sendTemplate([], 6).subscribe();
    const request = http.expectOne(req => req.url.endsWith('/sendTemplate'));
    expect(request.request.params.get('templateId')).toBe('6');
    expect(request.request.params.has('ngsw-bypass')).toBeFalse();
    request.flush({});
  });
});
