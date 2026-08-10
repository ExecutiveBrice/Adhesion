import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { ActivitePageQuery, ActiviteService } from 'src/app/_services/activite.service';
import { ParamService } from 'src/app/_services/param.service';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { Activite } from 'src/app/models';
import { Router } from '@angular/router';
import { faCircleCheck, faCircleXmark, faCartPlus, faPencilSquare } from '@fortawesome/free-solid-svg-icons';
import { ModalActivite } from 'src/app/template/modal-activite/modal.activite';

@Component({
  standalone: false,
  selector: 'ngbd-modal-component',
  templateUrl: './activites.component.html',
  styleUrls: ['./activites.component.css'],
})
export class ActivitesComponent implements OnInit, OnDestroy {
  activiteService = inject(ActiviteService);
  paramService = inject(ParamService);
  private tokenStorageService = inject(TokenStorageService);
  router = inject(Router);

  faCircleXmark = faCircleXmark;
  faCircleCheck = faCircleCheck;
  faPencilSquare = faPencilSquare;
  faCartPlus = faCartPlus;

  newActivite: Activite = new Activite();
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

  private readonly searchChanges = new Subject<string>();
  private readonly destroy$ = new Subject<void>();
  private readonly modalService = inject(NgbModal);

  ngOnInit(): void {
    if (this.tokenStorageService.getUser().roles) {
      this.showAdmin = this.tokenStorageService.getUser().roles.includes('ROLE_ADMIN');
      this.showSecretaire = this.tokenStorageService.getUser().roles.includes('ROLE_SECRETAIRE');
    } else {
      this.router.navigate(['login']);
      return;
    }

    this.searchChanges.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => this.getActivites(true));

    this.getActivites();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  openActivite(activite: Activite): void {
    const modalRef = this.modalService.open(ModalActivite, {
      size: 'xl',
      centered: true,
      backdrop: 'static',
      scrollable: true
    });
    modalRef.componentInstance.activite = activite;
    modalRef.result.then(
      () => this.getActivites(),
      () => undefined
    );
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

    this.activiteService.getPage(query).subscribe({
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

  lienActivite(activite: Activite): string | null {
    if (!activite.lien?.trim()) return null;
    const lien = activite.lien.trim();
    return /^https?:\/\//i.test(lien) ? lien : `https://${lien}`;
  }

  lienAdresseSalle(activite: Activite): string | null {
    const adresse = activite.salle?.adresse?.trim();
    return adresse ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(adresse)}` : null;
  }
}
