import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgbCarousel, NgbSlide } from '@ng-bootstrap/ng-bootstrap/carousel';
import { Publicite, PubliciteService } from '../../_services/publicite.service';
import { PubliciteImageComponent } from './publicite-image.component';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faFacebook, faSnapchat } from '@fortawesome/free-brands-svg-icons';
import { faGlobe } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-publicite-carousel',
  imports: [NgbCarousel, NgbSlide, PubliciteImageComponent, FontAwesomeModule],
  template: `
    @if (publicites().length) {
      <section class="publicite-carousel my-3" aria-label="Publicités de l’association">
        <ngb-carousel [interval]="paused() ? 0 : 6000" [pauseOnHover]="true" [pauseOnFocus]="true"
          [showNavigationArrows]="publicites().length > 1" [showNavigationIndicators]="publicites().length > 1">
          @for (item of publicites(); track item.id) {
            <ng-template ngbSlide [id]="'' + item.id">
              <app-publicite-image [id]="item.id" [description]="item.description" />
              @if (item.facebookUrl || item.snapchatUrl || item.websiteUrl) {
                <nav class="publicite-links" [attr.aria-label]="'Liens pour ' + item.description">
                  @if (item.facebookUrl) {
                    <a [href]="item.facebookUrl" target="_blank" rel="noopener noreferrer"
                      aria-label="Ouvrir la page Facebook dans un nouvel onglet" title="Facebook">
                      <fa-icon [icon]="faFacebook" aria-hidden="true" />
                    </a>
                  }
                  @if (item.snapchatUrl) {
                    <a [href]="item.snapchatUrl" target="_blank" rel="noopener noreferrer"
                      aria-label="Ouvrir le lien Snapchat dans un nouvel onglet" title="Snapchat">
                      <fa-icon [icon]="faSnapchat" aria-hidden="true" />
                    </a>
                  }
                  @if (item.websiteUrl) {
                    <a [href]="item.websiteUrl" target="_blank" rel="noopener noreferrer"
                      aria-label="Ouvrir le site internet dans un nouvel onglet" title="Site internet">
                      <fa-icon [icon]="faGlobe" aria-hidden="true" />
                    </a>
                  }
                </nav>
              }
            </ng-template>
          }
        </ngb-carousel>
        @if (publicites().length > 1) {
          <div class="text-end mt-1">
            <button type="button" class="btn btn-sm btn-outline-secondary" (click)="paused.set(!paused())" [attr.aria-pressed]="paused()">
              {{ paused() ? 'Reprendre le défilement' : 'Mettre en pause' }}
            </button>
          </div>
        }
      </section>
    }
  `,
  styles: `
    .publicite-carousel { max-width: 1100px; margin-inline: auto; }
    ngb-carousel { display: block; overflow: hidden; border-radius: .75rem; }
    .publicite-links { position: absolute; right: 1rem; bottom: 1rem; z-index: 3; display: flex; gap: .6rem; }
    .publicite-links a { display: grid; place-items: center; width: 2.75rem; height: 2.75rem; border-radius: 50%; color: #fff; background: rgba(0, 0, 0, .78); font-size: 1.35rem; text-decoration: none; box-shadow: 0 1px 5px rgba(0, 0, 0, .35); }
    .publicite-links a:hover, .publicite-links a:focus-visible { background: #000; outline: 3px solid #fff; }
  `
})
export class PubliciteCarouselComponent implements OnInit {
  readonly faFacebook = faFacebook;
  readonly faSnapchat = faSnapchat;
  readonly faGlobe = faGlobe;
  private readonly service = inject(PubliciteService);
  private readonly destroyRef = inject(DestroyRef);
  readonly publicites = signal<Publicite[]>([]);
  readonly paused = signal(typeof matchMedia !== 'undefined' && matchMedia('(prefers-reduced-motion: reduce)').matches);

  ngOnInit(): void {
    this.service.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: items => this.publicites.set(items),
      error: () => this.publicites.set([])
    });
  }
}
