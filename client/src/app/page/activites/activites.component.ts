import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { registerApiViewRefresh } from 'src/app/_services/api-render.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { ActivitePageQuery, ActiviteService } from 'src/app/_services/activite.service';
import { ParamService } from 'src/app/_services/param.service';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { ManagedSection, SectionManagementService } from 'src/app/_services/section-management.service';
import { Activite } from 'src/app/models';
import { UtilisateurSelectionnable } from 'src/app/models/utilisateurSelectionnable';
import { Router, RouterLink } from '@angular/router';
import { faCircleCheck, faCircleXmark, faPlus, faPencilSquare } from '@fortawesome/free-solid-svg-icons';
import { FormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { DatePipe, NgClass } from '@angular/common';
import { SeanceCalendrier } from '../../models/seance';
import { evenementSeance } from '../../models/evenement';
import { EvenementModalService } from '../../_services/evenement-modal.service';

@Component({
    selector: 'ngbd-modal-component',
    templateUrl: './activites.component.html',
    styleUrls: ['./activites.component.css'],
    imports: [
        FormsModule,
        FaIconComponent,
        NgClass,
        RouterLink,
        DatePipe,
    ],
})
export class ActivitesComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  activiteService = inject(ActiviteService);
  paramService = inject(ParamService);
  private sectionManagement = inject(SectionManagementService);
  private tokenStorageService = inject(TokenStorageService);
  router = inject(Router);
  private readonly modales = inject(EvenementModalService);

  faCircleXmark = faCircleXmark;
  faCircleCheck = faCircleCheck;
  faPencilSquare = faPencilSquare;
  faPlus = faPlus;

  activites: Activite[] = [];
  page = 1;
  pageSize = 20;
  readonly pageSizes = [10, 20, 50, 100];
  totalElements = 0;
  totalPages = 0;
  searchTerm = '';
  tarif: number | null = null;
  complete: boolean | null = null;
  reinscription: boolean | null = null;
  age: number | null = null;
  genre = '';
  loading = true;
  isFailed = false;
  errorMessage = '';

  showAdmin = false;
  showSecretaire = false;
  showEncadrant = false;
  scopedToManagedSections = false;
  managedSections: ManagedSection[] = [];

  private readonly searchChanges = new Subject<string>();
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    if (this.tokenStorageService.getUser().roles) {
      this.showAdmin = this.tokenStorageService.getUser().roles.includes('ROLE_ADMIN');
      this.showSecretaire = this.tokenStorageService.getUser().roles.includes('ROLE_SECRETAIRE');
      this.showEncadrant = this.tokenStorageService.getUser().roles.includes('ROLE_ENCADRANT');
      this.scopedToManagedSections = !this.showAdmin && !this.showSecretaire;
    } else {
      this.router.navigate(['login']);
      return;
    }

    this.searchChanges.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.getActivites(true));

    this.getActivites();
    if (this.scopedToManagedSections) {
      this.sectionManagement.sections().subscribe({
        next: sections => this.managedSections = sections,
        error: () => this.errorMessage = 'Les sections confiées n’ont pas pu être chargées.'
      });
    }
  }

  getActivites(resetPage = false): void {
    if (resetPage) {
      this.page = 1;
    }
    this.loading = true;
    this.isFailed = false;
    const query: ActivitePageQuery = {
      page: this.page - 1,
      size: this.pageSize,
      search: this.searchTerm.trim()
    };
    if (this.tarif !== null) query.tarif = this.tarif;
    if (this.complete !== null) query.complete = this.complete;
    if (this.reinscription !== null) query.reinscription = this.reinscription;
    if (this.age !== null) query.age = this.age;
    if (this.genre) query.genre = this.genre;

    this.activiteService.getPage(query, this.scopedToManagedSections).subscribe({
      next: data => {
        this.activites = data.content;
        this.totalElements = data.totalElements;
        this.totalPages = data.totalPages;
        this.page = data.number + 1;
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.isFailed = true;
        this.errorMessage = err.error?.message || err.message;
      }
    });
  }

  onSearchChange(value: string): void {
    this.searchChanges.next(value.trim());
  }

  resetSearch(): void {
    this.searchTerm = '';
    this.tarif = null;
    this.complete = null;
    this.reinscription = null;
    this.age = null;
    this.genre = '';
    this.getActivites(true);
  }

  applyFilters(): void {
    this.getActivites(true);
  }

  get hasActiveFilters(): boolean {
    return !!this.searchTerm.trim() || this.tarif !== null || this.complete !== null
      || this.reinscription !== null || this.age !== null || !!this.genre;
  }

  goToPage(targetPage: number): void {
    if (targetPage < 1 || targetPage > this.totalPages || targetPage === this.page) {
      return;
    }
    this.page = targetPage;
    this.getActivites();
  }

  onPageSizeChange(): void {
    this.getActivites(true);
  }

  get firstResult(): number {
    return this.totalElements === 0 ? 0 : (this.page - 1) * this.pageSize + 1;
  }

  get lastResult(): number {
    return Math.min(this.page * this.pageSize, this.totalElements);
  }

  get visiblePages(): number[] {
    const windowSize = Math.min(4, this.totalPages);
    const start = Math.max(1, Math.min(this.page - 1, this.totalPages - windowSize + 1));
    return Array.from({ length: windowSize }, (_, index) => start + index);
  }

  get showLeadingEllipsis(): boolean {
    return this.visiblePages.length > 0 && this.visiblePages[0] > 1;
  }

  get showTrailingEllipsis(): boolean {
    return this.visiblePages.length > 0 && this.visiblePages[this.visiblePages.length - 1] < this.totalPages;
  }

  trackByActivite(index: number, activite: Activite): number {
    return activite.id;
  }

  get canCreateActivity(): boolean {
    return this.showAdmin || this.showSecretaire || this.managedSections.length > 0;
  }

  canEditActivity(activite: Activite): boolean {
    return this.showAdmin || this.showSecretaire
      || this.managedSections.some(section => section.id === activite.section?.id);
  }

  nomEvenement(evenement: SeanceCalendrier): string {
    const types: Record<string, string> = { ACTIVITE: 'Séance', ENTRAINEMENT: 'Entrainement', RENCONTRE: 'Match', TOURNOI: 'Tournoi', ANIMATION: 'Animation', AUTRE: 'Autre' };
    const nom = evenement.nomEvenement?.trim() || types[evenement.typeEvenement ?? ''] || 'Événement';
    return evenement.typeEvenement === 'RENCONTRE' && evenement.adversaire?.trim()
      ? `${nom} contre ${evenement.adversaire.trim()}` : nom;
  }

  ouvrirEvenement(evenement: SeanceCalendrier): void {
    this.modales.ouvrir({ ...evenementSeance(evenement, false), titre: this.nomEvenement(evenement) });
  }

  lienActivite(activite: Activite): string | null {
    if (!activite.lien?.trim()) return null;
    const lien = activite.lien.trim();
    return /^https?:\/\//i.test(lien) ? lien : `https://${lien}`;
  }

  lienAdresseSalle(activite: Activite): string | null {
    const adresse = activite.salle?.adresse?.trim();
    return adresse ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(adresse)}` : null;
  }

  jourLibelle(jour?: string): string {
    const jours: Record<string, string> = {
      MONDAY: 'Lundi',
      TUESDAY: 'Mardi',
      WEDNESDAY: 'Mercredi',
      THURSDAY: 'Jeudi',
      FRIDAY: 'Vendredi',
      SATURDAY: 'Samedi',
      SUNDAY: 'Dimanche'
    };
    return jours[jour ?? ''] ?? jour ?? 'Jour non renseigné';
  }

  heureLibelle(heure?: string): string {
    if (!heure) return 'Horaire non renseigné';
    return heure.substring(0, 5).replace(':', 'h');
  }

  nomsUtilisateurs(utilisateurs?: UtilisateurSelectionnable[]): string {
    if (!utilisateurs?.length) return 'Aucun';
    return utilisateurs.map(({ prenom, nom }) => `${prenom} ${nom}`.trim()).join(', ');
  }
}
