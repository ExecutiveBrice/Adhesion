import { Injectable, signal } from '@angular/core';

type BeforeInstallPromptEvent = Event & {
  prompt(): Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
};

@Injectable({ providedIn: 'root' })
export class PwaService {
  private static readonly installPromptDismissedKey = 'pwa-install-prompt-dismissed';
  readonly canInstall = signal(false);
  readonly showIosInstallHint = signal(false);
  private deferredPrompt?: BeforeInstallPromptEvent;

  constructor() {
    if (typeof window === 'undefined' || !this.isMobile() || this.isStandalone() || this.wasInstallPromptDismissed()) {
      return;
    }

    this.showIosInstallHint.set(this.isIos());
    window.addEventListener('beforeinstallprompt', (event: Event) => {
      event.preventDefault();
      if (!this.wasInstallPromptDismissed()) {
        this.deferredPrompt = event as BeforeInstallPromptEvent;
        this.canInstall.set(true);
      }
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

  dismissInstallPrompt(): void {
    try {
      sessionStorage.setItem(PwaService.installPromptDismissedKey, 'true');
    } catch {
      // L'invite reste masquée pour la durée de vie de cette instance si le stockage est indisponible.
    }
    this.resetInstallState();
  }

  isMobileStandalone(): boolean {
    return typeof window !== 'undefined' && this.isMobile() && this.isStandalone();
  }

  private resetInstallState(): void {
    this.deferredPrompt = undefined;
    this.canInstall.set(false);
    this.showIosInstallHint.set(false);
  }

  private wasInstallPromptDismissed(): boolean {
    try {
      return sessionStorage.getItem(PwaService.installPromptDismissedKey) === 'true';
    } catch {
      return false;
    }
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
