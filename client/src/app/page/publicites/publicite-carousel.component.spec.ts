import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PubliciteCarouselComponent } from './publicite-carousel.component';
import { environment } from '../../../environments/environment';

describe('Carrousel des publicités', () => {
  let http: HttpTestingController;
  const url = environment.server + '/publicites';

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify({ ignoreCancelled: true }));

  it('ne réserve aucun espace quand aucune publicité n’est publiée', () => {
    const fixture = TestBed.createComponent(PubliciteCarouselComponent);
    fixture.detectChanges();
    http.expectOne(url).flush([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('section')).toBeNull();
    fixture.destroy();
  });

  it('affiche les images avec navigation et pause, puis libère les fichiers en mémoire', () => {
    const revoke = spyOn(URL, 'revokeObjectURL');
    const fixture = TestBed.createComponent(PubliciteCarouselComponent);
    fixture.componentInstance.paused.set(false);
    fixture.detectChanges();
    http.expectOne(url).flush([
      { id: 1, description: 'Premier visuel', facebookUrl: 'https://facebook.com/alod', snapchatUrl: 'https://snapchat.com/add/alod', websiteUrl: 'https://alod.fr' },
      { id: 2, description: 'Deuxième visuel' }
    ]);
    fixture.detectChanges();
    http.expectOne(url + '/1/image').flush(new Blob(['image'], { type: 'image/png' }));
    http.expectOne(url + '/2/image').flush(new Blob(['image'], { type: 'image/png' }));
    fixture.detectChanges();
    const images = fixture.nativeElement.querySelectorAll('img');
    expect(images.length).toBe(2);
    expect(images[0].getAttribute('alt')).toBe('Premier visuel');
    const links = fixture.nativeElement.querySelectorAll('.publicite-links a');
    expect(links.length).toBe(3);
    expect([...links].map((link: HTMLAnchorElement) => link.href)).toEqual([
      'https://facebook.com/alod', 'https://snapchat.com/add/alod', 'https://alod.fr/'
    ]);
    expect([...links].every((link: HTMLAnchorElement) => link.target === '_blank' && link.rel === 'noopener noreferrer')).toBeTrue();
    expect(fixture.nativeElement.querySelector('.carousel-control-next')).not.toBeNull();
    fixture.nativeElement.querySelector('button.btn').click();
    expect(fixture.componentInstance.paused()).toBeTrue();
    fixture.destroy();
    expect(revoke).toHaveBeenCalledTimes(2);
  });
});
