import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { of } from 'rxjs';
import { ActiviteService } from 'src/app/_services/activite.service';
import { ParamService } from 'src/app/_services/param.service';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { Activite } from 'src/app/models';
import { ActivitesComponent } from './activites.component';

describe('ActivitesComponent', () => {
  let component: ActivitesComponent;
  let fixture: ComponentFixture<ActivitesComponent>;
  let activiteService: jasmine.SpyObj<ActiviteService>;

  const activite = Object.assign(new Activite(), {
    id: 7,
    nom: 'Yoga',
    groupeFiltre: 'Bien-être',
    horaire: 'Mardi 18h',
    lien: 'association.example/activites/yoga',
    salle: { nom: 'Salle des fêtes', adresse: '1 rue du Parc, Paris', couleur: '#ffffff' }
  });

  beforeEach(async () => {
    activiteService = jasmine.createSpyObj<ActiviteService>('ActiviteService', ['getPage']);
    activiteService.getPage.and.returnValue(of({
      content: [activite],
      totalElements: 41,
      totalPages: 3,
      number: 0,
      size: 20
    }));

    await TestBed.configureTestingModule({
      imports: [FormsModule],
      declarations: [ActivitesComponent],
      providers: [
        { provide: ActiviteService, useValue: activiteService },
        { provide: ParamService, useValue: {} },
        { provide: TokenStorageService, useValue: { getUser: () => ({ roles: ['ROLE_ADMIN'] }) } },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) },
        { provide: NgbModal, useValue: jasmine.createSpyObj<NgbModal>('NgbModal', ['open']) }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(ActivitesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('charge la première page au démarrage', () => {
    expect(component.activites).toEqual([activite]);
    expect(component.totalElements).toBe(41);
    expect(component.totalPages).toBe(3);
    expect(activiteService.getPage).toHaveBeenCalledOnceWith({ page: 0, size: 20, search: '' });
  });

  it('affiche les activités dans des cartes responsives', () => {
    const cards = fixture.nativeElement.querySelectorAll('.activity-card');

    expect(cards.length).toBe(1);
    expect(cards[0].textContent).toContain('Yoga');
    expect(fixture.nativeElement.querySelector('.activite-table')).toBeNull();
    expect(fixture.nativeElement.querySelector('.activity-link').href).toBe('https://association.example/activites/yoga');
    expect(fixture.nativeElement.querySelector('[href*="google.com/maps"]').textContent).toContain('1 rue du Parc, Paris');
  });

  it('recherche sur le serveur après temporisation et revient à la première page', fakeAsync(() => {
    component.page = 3;
    component.searchTerm = '  yoga  ';

    component.onSearchChange(component.searchTerm);
    tick(349);
    expect(activiteService.getPage).toHaveBeenCalledTimes(1);
    tick(1);

    expect(activiteService.getPage).toHaveBeenCalledTimes(2);
    expect(activiteService.getPage).toHaveBeenCalledWith({ page: 0, size: 20, search: 'yoga' });
  }));

  it('demande la page sélectionnée au serveur', () => {
    component.totalPages = 3;

    component.goToPage(2);

    expect(activiteService.getPage).toHaveBeenCalledWith({ page: 1, size: 20, search: '' });
  });

  it('revient à la première page quand la taille change', () => {
    component.page = 3;
    component.pageSize = 50;

    component.onPageSizeChange();

    expect(activiteService.getPage).toHaveBeenCalledWith({ page: 0, size: 50, search: '' });
  });

  it('applique les filtres spécifiques dans la requête paginée', () => {
    component.tarif = 200;
    component.complete = true;
    component.reinscription = false;
    component.age = 16;
    component.genre = 'Féminine';

    component.applyFilters();

    expect(activiteService.getPage).toHaveBeenCalledWith({
      page: 0,
      size: 20,
      search: '',
      tarif: 200,
      complete: true,
      reinscription: false,
      age: 16,
      genre: 'Féminine'
    });
  });
});
