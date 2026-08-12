import { ChangeDetectorRef, DestroyRef, Injectable, inject } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ApiRenderService {
  private readonly views = new Set<ChangeDetectorRef>();

  register(changeDetectorRef: ChangeDetectorRef, destroyRef: DestroyRef): void {
    this.views.add(changeDetectorRef);
    destroyRef.onDestroy(() => this.views.delete(changeDetectorRef));
  }

  notify(): void {
    this.views.forEach(view => view.detectChanges());
  }
}

export function registerApiViewRefresh(): void {
  inject(ApiRenderService).register(inject(ChangeDetectorRef), inject(DestroyRef));
}
