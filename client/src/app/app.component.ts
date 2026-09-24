import { Component, computed, ElementRef, inject, signal, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TokenStorageService } from './_services/token-storage.service';
import { ParamService } from './_services/param.service';
import { ParamTransmissionService } from './_services/transmission.service';
import { ToastService } from './_services/toast.service';
import { registerApiViewRefresh } from './_services/api-render.service';
import { NgbCollapse } from '@ng-bootstrap/ng-bootstrap/collapse';
import { NavigationEnd, Router, RouterLinkActive, RouterLink, RouterOutlet } from '@angular/router';
import { NgbToast, NgbToastHeader } from '@ng-bootstrap/ng-bootstrap/toast';
import { PwaService } from './_services/pwa.service';
import { AuthService } from './_services/auth.service';
import { LoginPageService } from './_services/login-page.service';
import { ShopCartLinkComponent } from './shop/components/shop-cart-link.component';
import { filter } from 'rxjs';


@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    imports: [NgbCollapse, RouterLinkActive, RouterLink, RouterOutlet, NgbToast, NgbToastHeader, ShopCartLinkComponent]
})
export class AppComponent {
  private readonly apiViewRefresh = registerApiViewRefresh();
  readonly toastService = inject(ToastService);
  readonly pwaService = inject(PwaService);
  readonly loginPage = inject(LoginPageService);
  private readonly router = inject(Router);
  private readonly currentUrl = signal(this.router.url);
  readonly isLoginPage = computed(() => ['/', '/login'].includes(this.currentUrl().split(/[?#]/)[0]));
  readonly pageTitle = signal('ALOD');
  @ViewChild('menuToggle') private menuToggle?: ElementRef<HTMLButtonElement>;
  transmissionService = inject(ParamTransmissionService);
  private paramService = inject(ParamService);
  private tokenStorageService = inject(TokenStorageService);
  private authService = inject(AuthService);

  isCollapsed = true
  private roles: string[] = [];
  isLoggedIn = false;
  showAdmin = false;
  showBureau = false;
  showMembreCA = false;
  showSecretaire = false;
  showProf=false;
  showReferent=false;
  showSeances=false;
  showComptable=false;
  username?: string;
  maintenance: Boolean = false

  constructor() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntilDestroyed()
    ).subscribe(() => {
      this.updatePage();
      this.updateSession();
      this.closeMenu(false);
    });
    this.tokenStorageService.sessionChanges$
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.updateSession();
        if (this.isLoggedIn) {
          window.location.reload();
        } else {
          this.closeMenu(false);
          void this.router.navigate(['/login'], { replaceUrl: true });
        }
      });
  }

  ngOnInit(): void {
    this.updatePage();
    this.updateSession();

    this.paramService.isClose().subscribe({
      next: data => this.maintenance = data,
      error: () => this.maintenance = true
    });
  }

  closeMenu(restoreFocus = true): void {
    const wasOpen = !this.isCollapsed;
    this.isCollapsed = true;
    if (wasOpen && restoreFocus) {
      this.menuToggle?.nativeElement.focus();
    }
  }

  private updatePage(): void {
    this.currentUrl.set(this.router.url);
    let route = this.router.routerState.snapshot.root;
    let title = route.title;
    while (route.firstChild) {
      route = route.firstChild;
      title = route.title ?? title;
    }
    this.pageTitle.set(title || 'ALOD');
  }

  private updateSession(): void {
    this.isLoggedIn = !!this.tokenStorageService.getToken();
    const user = this.isLoggedIn ? this.tokenStorageService.getUser() : {};
    this.roles = user.roles ?? [];

    this.showAdmin = this.roles.includes('ROLE_ADMIN');
    this.showBureau = this.roles.includes('ROLE_BUREAU');
    this.showMembreCA = this.roles.includes('ROLE_MEMBRECA');
    this.showSecretaire = this.roles.includes('ROLE_SECRETAIRE');
    this.showProf = this.roles.includes('ROLE_ENCADRANT');
    this.showReferent = this.roles.includes('ROLE_REFERENT');
    this.showSeances = this.showProf || this.showReferent;
    this.showComptable = this.roles.includes('ROLE_COMPTABLE');
    this.username = user.username;
  }


  logout(): void {
    this.authService.logout().subscribe();
    this.updateSession();
    this.closeMenu(false);
    void this.router.navigate(['/login'], { replaceUrl: true });
  }
}
