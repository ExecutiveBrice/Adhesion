import { DestroyRef, Directive, inject, signal } from '@angular/core';
import { NgbToast } from '@ng-bootstrap/ng-bootstrap/toast';

@Directive({
  selector: 'ngb-toast[appToastCountdown]',
  exportAs: 'appToastCountdown',
  host: { '(click)': 'close()' }
})
export class ToastCountdownDirective {
  private readonly toast = inject(NgbToast, { self: true });
  readonly durationSeconds = 10;
  readonly remainingSeconds = signal(this.durationSeconds);
  private readonly expiresAt = performance.now() + this.durationSeconds * 1000;
  private timeoutId?: ReturnType<typeof setTimeout>;
  private closing = false;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.timeoutId));
    this.updateCountdown();
  }

  close(): void {
    if (this.closing) return;

    this.closing = true;
    clearTimeout(this.timeoutId);
    this.toast.hide();
  }

  private updateCountdown(): void {
    const remainingMs = this.expiresAt - performance.now();
    const remainingSeconds = Math.max(0, Math.ceil(remainingMs / 1000));
    this.remainingSeconds.set(remainingSeconds);
    if (remainingSeconds === 0) {
      this.close();
      return;
    }

    const nextSecondMs = remainingMs - (remainingSeconds - 1) * 1000;
    this.timeoutId = setTimeout(() => this.updateCountdown(), Math.ceil(nextSecondMs));
  }
}
