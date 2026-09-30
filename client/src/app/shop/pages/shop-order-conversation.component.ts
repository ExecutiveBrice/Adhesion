import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { timeout } from 'rxjs';
import { TokenStorageService } from '../../_services/token-storage.service';
import { ShopOrderMessageDto } from '../models/shop.models';
import { ShopAdminApiService } from '../services/shop-admin-api.service';
import { ShopApiService } from '../services/shop-api.service';

@Component({
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './shop-order-conversation.component.html',
  styleUrl: './shop-order-conversation.component.css'
})
export class ShopOrderConversationComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(ShopApiService);
  private readonly adminApi = inject(ShopAdminApiService);
  private readonly storage = inject(TokenStorageService);
  readonly messages = signal<ShopOrderMessageDto[]>([]);
  readonly loading = signal(true);
  readonly sending = signal(false);
  readonly error = signal<string | null>(null);
  readonly isManager = this.storage.getUser().roles?.includes('ROLE_RESPONSABLE_BOUTIQUE') ?? false;
  message = '';
  orderNumber = '';

  ngOnInit(): void {
    this.orderNumber = this.route.snapshot.paramMap.get('orderNumber') ?? '';
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    const request = this.isManager
      ? this.adminApi.conversation(this.orderNumber)
      : this.api.conversation(this.orderNumber);
    request.pipe(timeout({ first: 10_000 })).subscribe({
      next: messages => { this.messages.set(messages); this.loading.set(false); },
      error: () => {
        this.error.set('Cette conversation est indisponible. Elle est accessible après une demande de remboursement.');
        this.loading.set(false);
      }
    });
  }

  send(): void {
    const content = this.message.trim();
    if (!content || this.sending()) return;
    this.sending.set(true);
    this.error.set(null);
    const request = this.isManager
      ? this.adminApi.sendConversationMessage(this.orderNumber, content)
      : this.api.sendConversationMessage(this.orderNumber, content);
    request.pipe(timeout({ first: 10_000 })).subscribe({
      next: message => {
        this.messages.update(messages => [...messages, message]);
        this.message = '';
        this.sending.set(false);
      },
      error: () => {
        this.error.set('Votre message n’a pas pu être envoyé. Réessayez.');
        this.sending.set(false);
      }
    });
  }

  senderLabel(message: ShopOrderMessageDto): string {
    if (message.senderType === 'CUSTOMER') return this.isManager ? 'Client' : 'Vous';
    return this.isManager ? 'Vous' : 'Gestion boutique';
  }
}
