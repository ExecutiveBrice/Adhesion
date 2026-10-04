import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { combineLatest, forkJoin, of, Subscription } from 'rxjs';
import { ParamService } from '../../_services/param.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { TribuService } from '../../_services/tribu.service';
import { Accord, Adherent, Tribu, User } from '../../models';
import { UserComponent } from '../../template/user/user.component';

@Component({
  selector: 'app-profil',
  imports: [UserComponent, RouterLink],
  templateUrl: './profil.component.html'
})
export class ProfilComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private readonly tribuService = inject(TribuService);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly paramService = inject(ParamService);
  private readonly route = inject(ActivatedRoute);
  readonly nouveau = this.route.snapshot.data['nouveau'] === true;

  private chargementProfil?: Subscription;

  tribu?: Tribu;
  adherent?: Adherent;
  chargement = true;
  erreur = '';

  get canBrowseTribe(): boolean {
    return this.tokenStorage.getUser().roles?.some((role: string) =>
      ['ROLE_ADMIN', 'ROLE_SECRETAIRE'].includes(role)) ?? false;
  }

  ngOnInit(): void {
    combineLatest([this.route.paramMap, this.route.queryParamMap])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.chargerProfil());
  }

  chargerProfil(): void {
    this.chargementProfil?.unsubscribe();
    this.chargement = true;
    this.erreur = '';
    this.adherent = undefined;
    const user = this.tokenStorage.getUser();
    const userId = user.id;
    const tribuUuid = this.route.snapshot.paramMap.get('tribuUuid');
    const adherentId = this.route.snapshot.queryParamMap.get('adherentId');
    const consultationSecretariat = user.roles?.some((role: string) => ['ROLE_ADMIN', 'ROLE_SECRETAIRE', 'ROLE_MEMBRECA'].includes(role));
    this.chargementProfil = forkJoin({
      tribu: tribuUuid && consultationSecretariat
        ? this.tribuService.getTribuByUuid(tribuUuid)
        : this.tribuService.getConnected(),
      textes: this.nouveau ? this.paramService.getAllText() : of([])
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ({ tribu, textes }) => {
        if (tribuUuid && tribu.uuid !== tribuUuid) {
          this.chargement = false;
          this.erreur = 'Ce profil adhérent est inaccessible.';
          return;
        }
        this.tribu = tribu;
        if (this.nouveau) {
          const adherent = new Adherent();
          adherent.user = new User();
          adherent.mineur = false;
          adherent.completAdhesion = false;
          adherent.tribuId = tribu.uuid;
          adherent.activitesNm1 = [];
          adherent.accords = [
            Object.assign(new Accord(), {
              nom: 'RGPD', title: 'RGPD', refusable: false, etat: true,
              text: textes.find(texte => texte.paramName === 'RGPD')?.paramValue ?? '',
              valide: "J'accepte l'utilisation de mes données personnelles"
            }),
            Object.assign(new Accord(), {
              nom: 'DroitImage', title: 'DroitImage', refusable: true, etat: true,
              text: textes.find(texte => texte.paramName === 'DroitImage')?.paramValue ?? '',
              valide: "J'accepte l'utilisation de mon image",
              refus: "Je refuse l'utilisation de mon image"
            })
          ];
          tribu.adherents.push(adherent);
          this.adherent = adherent;
        } else {
          this.adherent = adherentId
            ? tribu.adherents.find(adherent => adherent.id === Number(adherentId))
            : tribu.adherents.find(adherent => adherent.user?.id === userId)
              ?? (tribuUuid && consultationSecretariat ? tribu.adherents[0] : undefined);
        }
        this.chargement = false;
        if (!this.adherent) {
          this.erreur = 'Aucun profil adhérent associé à votre compte.';
        }
      },
      error: () => {
        this.chargement = false;
        this.erreur = 'Impossible de charger votre profil. Veuillez réessayer.';
      }
    });
  }
}
