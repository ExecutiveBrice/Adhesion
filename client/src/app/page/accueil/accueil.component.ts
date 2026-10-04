import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { ParamService } from '../../_services/param.service';
import { ToastService } from '../../_services/toast.service';
import { TribuService } from '../../_services/tribu.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { CalendrierComponent } from '../../template/calendrier/calendrier.component';

@Component({
  selector: 'app-accueil',
  templateUrl: './accueil.component.html',
  imports: [CalendrierComponent]
})
export class AccueilComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private readonly paramService = inject(ParamService);
  private readonly tribuService = inject(TribuService);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly router = inject(Router);
  private readonly toastr = inject(ToastService);

  adherentId?: number;

  ngOnInit(): void {
    this.paramService.isClose().subscribe({
      next: closed => {
        if (closed) this.router.navigate(['login']);
      },
      error: () => this.showError()
    });
    this.tribuService.getConnected().subscribe({
      next: tribu => {
        const userId = this.tokenStorage.getUser().id;
        this.adherentId = tribu.adherents.find(adherent => adherent.user?.id === userId)?.id;
        if (!this.adherentId) this.showError();
      },
      error: () => this.showError()
    });
  }

  private showError(): void {
    this.toastr.error('Une erreur est survenue. Rechargez la page et recommencez.', 'Erreur');
  }
}
