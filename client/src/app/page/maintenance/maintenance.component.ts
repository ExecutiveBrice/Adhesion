import { Component, OnInit, inject } from '@angular/core';
import { TokenStorageService } from '../../_services/token-storage.service';

@Component({
    selector: 'app-maintenance',
    templateUrl: './maintenance.component.html',
    styleUrls: ['./maintenance.component.css']
})
export class MaintenanceComponent implements OnInit {
  private token = inject(TokenStorageService);

  currentUser: any;

  ngOnInit(): void {
    this.currentUser = this.token.getUser();
  }
}
