import { Component, effect, inject, input, signal } from '@angular/core';
import { PubliciteService } from '../../_services/publicite.service';

@Component({
  selector: 'app-publicite-image',
  template: `
    @if (url()) {
      <img [src]="url()" [alt]="description()" width="1900" height="900">
    } @else {
      <div class="placeholder-image" [attr.role]="error() ? 'alert' : 'status'">
        {{ error() ? 'Image indisponible : ' + description() : 'Chargement de l’image…' }}
      </div>
    }
  `,
  styles: `
    :host { display: block; }
    img, .placeholder-image { display: block; width: 100%; aspect-ratio: 19 / 9; height: auto; }
    img { object-fit: contain; background: #f0f2f5; }
    .placeholder-image { display: grid; place-items: center; background: #f0f2f5; padding: 1rem; }
  `
})
export class PubliciteImageComponent {
  readonly id = input.required<number>();
  readonly description = input.required<string>();
  readonly refresh = input(0);
  readonly url = signal('');
  readonly error = signal(false);
  private readonly service = inject(PubliciteService);

  constructor() {
    effect(onCleanup => {
      this.refresh();
      this.url.set('');
      this.error.set(false);
      let objectUrl = '';
      const subscription = this.service.image(this.id()).subscribe({
        next: blob => { objectUrl = URL.createObjectURL(blob); this.url.set(objectUrl); },
        error: () => this.error.set(true)
      });
      onCleanup(() => {
        subscription.unsubscribe();
        if (objectUrl) URL.revokeObjectURL(objectUrl);
      });
    });
  }
}
