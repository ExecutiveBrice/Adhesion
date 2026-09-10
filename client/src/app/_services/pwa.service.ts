import { Injectable, signal } from '@angular/core';

type BeforeInstallPromptEvent = Event & {
  prompt(): Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
};

@Injectable({ providedIn: 'root' })
export class PwaService {
  readonly canInstall = signal(false);
  readonly showIosInstallHint = signal(false);
  private deferredPrompt?: BeforeInstallPromptEvent;

  constructor() {
    if (typeof window === 'undefined' || !this.isMobile() || this.isStandalone()) {
      return;
    }

    this.showIosInstallHint.set(this.isIos());
    window.addEventListener('beforeinstallprompt', (event: Event) => {
      event.preventDefault();
      this.deferredPrompt = event as BeforeInstallPromptEvent;
      this.canInstall.set(true);
    });
    window.addEventListener('appinstalled', () => this.resetInstallState());
  }

  async install(): Promise<void> {
    if (!this.deferredPrompt) {
      return;
    }

    await this.deferredPrompt.prompt();
    await this.deferredPrompt.userChoice;
    this.resetInstallState();
  }

  private resetInstallState(): void {
    this.deferredPrompt = undefined;
    this.canInstall.set(false);
    this.showIosInstallHint.set(false);
  }

  private isMobile(): boolean {
    return /Android|iPhone|iPad|iPod|IEMobile|Opera Mini/i.test(navigator.userAgent)
      || (navigator.maxTouchPoints > 1 && window.matchMedia('(pointer: coarse)').matches);
  }

  private isIos(): boolean {
    return /iPhone|iPad|iPod/i.test(navigator.userAgent)
      || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
  }

  private isStandalone(): boolean {
    const iosNavigator = navigator as Navigator & { standalone?: boolean };
    return window.matchMedia('(display-mode: standalone)').matches || iosNavigator.standalone === true;
  }
}
