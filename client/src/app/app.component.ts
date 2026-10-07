import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TokenStorageService } from './_services/token-storage.service';
import { ParamService } from './_services/param.service';
import { ParamTransmissionService } from './_services/transmission.service';
import { ToastService } from './_services/toast.service';
import { registerApiViewRefresh } from './_services/api-render.service';
import { NavigationEnd, Router, RouterLinkActive, RouterLink, RouterOutlet } from '@angular/router';
import { NgbToast, NgbToastHeader } from '@ng-bootstrap/ng-bootstrap/toast';
import { PwaService } from './_services/pwa.service';
import { AuthService } from './_services/auth.service';
import { LoginPageService } from './_services/login-page.service';
import { ChatService } from './_services/chat.service';
import { SectionManagementService } from './_services/section-management.service';
import { ShopCartLinkComponent } from './shop/components/shop-cart-link.component';
import { filter } from 'rxjs';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem, NgbDropdownButtonItem } from '@ng-bootstrap/ng-bootstrap/dropdown';
import { TribuService } from './_services/tribu.service';
import { Adherent } from './models';
import { SessionNavigationService } from './_services/session-navigation.service';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faHouse, faCalendarDays, faUser, faComments, faStore, faGear, faPersonRunning, faUsers, faIdCard, faEnvelope, faChartColumn, faPeopleGroup, faCalendarCheck, faClipboardList, faRightToBracket, faBullhorn } from '@fortawesome/free-solid-svg-icons';


@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    imports: [ RouterLinkActive, RouterLink, RouterOutlet, NgbToast, NgbToastHeader, ShopCartLinkComponent, NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem, NgbDropdownButtonItem, FontAwesomeModule]
})
export class AppComponent {
  readonly faBullhorn = faBullhorn;
  readonly faHouse = faHouse;
  readonly faCalendarDays = faCalendarDays;
  readonly faUser = faUser;
  readonly faComments = faComments;
  readonly faStore = faStore;
  readonly faGear = faGear;
  readonly faPersonRunning = faPersonRunning;
  readonly faUsers = faUsers;
  readonly faIdCard = faIdCard;
  readonly faEnvelope = faEnvelope;
  readonly faChartColumn = faChartColumn;
  readonly faPeopleGroup = faPeopleGroup;
  readonly faCalendarCheck = faCalendarCheck;
  readonly faClipboardList = faClipboardList;
  readonly faRightToBracket = faRightToBracket;
  private readonly apiViewRefresh = registerApiViewRefresh();
  readonly toastService = inject(ToastService);
  readonly pwaService = inject(PwaService);
  readonly loginPage = inject(LoginPageService);
  readonly chat = inject(ChatService);
  private readonly sectionManagement = inject(SectionManagementService);
  private readonly router = inject(Router);
  private readonly currentUrl = signal(this.router.url);
  readonly isLoginPage = computed(() => ['/', '/login'].includes(this.currentUrl().split(/[?#]/)[0]));
  readonly pageTitle = signal('ALOD');
  transmissionService = inject(ParamTransmissionService);
  private paramService = inject(ParamService);
  private tokenStorageService = inject(TokenStorageService);
  private authService = inject(AuthService);
  private readonly sessionNavigation = inject(SessionNavigationService);
  private readonly tribuService = inject(TribuService);
  readonly tribeMembers = signal<Adherent[]>([]);
  readonly activeMember = signal<Adherent | undefined>(undefined);
  readonly loadingMembers = signal(false);
  readonly membersError = signal(false);
  readonly switchingMember = signal(false);
  private loadedUserId?: number;

  memberName(member?: Adherent): string {
    return [member?.prenom, member?.nom].filter(Boolean).join(' ') || this.username || 'Mon compte';
  }

  memberInitials(member?: Adherent): string {
    return [member?.prenom, member?.nom]
      .map(name => Array.from(name?.trim() || '')[0] || '')
      .join('').toLocaleUpperCase('fr-FR');
  }

  memberAvatar(member?: Adherent): string {
    if (!member?.genre) return 'assets/nouveau.png';
    const female = member.genre === 'Féminin';
    return 'assets/' + (member.mineur ? (female ? 'fille' : 'garcon') : (female ? 'femme' : 'homme')) + '.png';
  }

  loadTribeMembers(force = false): void {
    const userId = this.tokenStorageService.getUser().id;
    if (!this.isLoggedIn || this.loadingMembers() || (!force && this.loadedUserId === userId && this.tribeMembers().length)) return;
    this.loadingMembers.set(true);
    this.membersError.set(false);
    this.tribuService.getConnected().subscribe({
      next: tribe => {
        this.loadingMembers.set(false);
        if (this.tokenStorageService.getUser().id !== userId || !this.isLoggedIn) return;
        this.loadedUserId = userId;
        this.tribeMembers.set(tribe.adherents ?? []);
        this.activeMember.set(this.tribeMembers().find(member => member.user?.id === userId));
      },
      error: () => {
        this.loadingMembers.set(false);
        this.membersError.set(true);
      }
    });
  }

  switchMember(member: Adherent): void {
    if (this.switchingMember() || !member.user || member.id === this.activeMember()?.id) return;
    this.switchingMember.set(true);
    this.authService.switchMember(member.id).subscribe({
      next: () => this.reloadMemberSession(),
      error: () => {
        this.switchingMember.set(false);
        this.toastService.error('Impossible de changer d’adhérent. Votre session actuelle est conservée.', 'Changement d’adhérent');
      }
    });
  }

  reloadMemberSession(): void {
    this.sessionNavigation.reloadAccueil();
  }

  private roles: string[] = [];
  isLoggedIn = false;
  showAdmin = false;
  showBureau = false;
  showMembreCA = false;
  showSecretaire = false;
  showProf=false;
  showReferent=false;
  showSeances=false;
  showCommunication = false;
  showShopManager=false;
  showShop = false;
  showChat = false;
  showManagedSections = false;
  private loadedManagedSectionsUserId?: number;
  username?: string;
  maintenance: Boolean = false

  constructor() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntilDestroyed()
    ).subscribe(() => {
      this.updatePage();
      this.updateSession();
      this.loadTribeMembers();
    });
    this.tokenStorageService.sessionChanges$
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.updateSession();
        if (this.isLoggedIn) {
          this.reloadMemberSession();
        } else {          void this.router.navigate(['/login'], { replaceUrl: true });
        }
      });
  }

  ngOnInit(): void {
    this.updatePage();
    this.updateSession();

    this.loadTribeMembers();

    this.paramService.isClose().subscribe({
      next: data => this.maintenance = data,
      error: () => this.maintenance = true
    });
    this.paramService.getAllBoolean().subscribe({
      next: params => {
        this.showShop = params.some(param => param.paramName === 'Show_Boutique' && param.paramValue);
        this.showChat = params.some(param => param.paramName === 'Show_Chat' && param.paramValue);
        this.chat.monitorUnread(this.showChat && this.isLoggedIn ? this.tokenStorageService.getUser().id : undefined);
      },
      error: () => {
        this.showShop = false;
        this.showChat = false;
        this.chat.monitorUnread();
      }
    });
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
    if (this.showChat) {
      this.chat.monitorUnread(this.isLoggedIn ? user.id : undefined);
    }
    this.roles = user.roles ?? [];
    if (this.isLoggedIn && user.id && this.loadedManagedSectionsUserId !== user.id) {
      this.loadedManagedSectionsUserId = user.id;
      this.sectionManagement.sections().subscribe({
        next: sections => {
          if (this.isLoggedIn && this.tokenStorageService.getUser().id === user.id) {
            this.showManagedSections = sections.length > 0;
          }
        },
        error: () => this.showManagedSections = false
      });
    }

    this.showAdmin = this.roles.includes('ROLE_ADMIN');
    this.showBureau = this.roles.includes('ROLE_BUREAU');
    this.showMembreCA = this.roles.includes('ROLE_MEMBRECA');
    this.showSecretaire = this.roles.includes('ROLE_SECRETAIRE');
    this.showProf = this.roles.includes('ROLE_ENCADRANT');
    this.showReferent = this.roles.includes('ROLE_REFERENT_ACTIVITE');
    this.showSeances = this.showProf || this.showReferent;
    this.showCommunication = this.roles.includes('ROLE_COMMUNICATION_GLOBAL');
    this.showShopManager = this.roles.includes('ROLE_RESPONSABLE_BOUTIQUE');
    this.username = user.username;
    if (!this.isLoggedIn) {
      this.showManagedSections = false;
      this.loadedManagedSectionsUserId = undefined;
      this.loadedUserId = undefined;
      this.tribeMembers.set([]);
      this.activeMember.set(undefined);
    }
  }


  logout(): void {
    this.authService.logout().subscribe();
    this.updateSession();    void this.router.navigate(['/login'], { replaceUrl: true });
  }
}
