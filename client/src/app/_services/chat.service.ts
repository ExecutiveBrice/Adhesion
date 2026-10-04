import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, defer, EMPTY, exhaustMap, Subscription, tap, timer } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ChatCategory { descriptif: string; jour: string; horaire: string; }
export interface ChatRoom { id: number; nom: string; groupeFiltre: string; categories: ChatCategory[]; unreadCount: number; canWrite: boolean; }
export type ChatSenderRole = 'REFERENT_ACTIVITE' | 'REFERENT_SECTION' | 'COMMUNICATION_SECTION' |
  'COMMUNICATION_GLOBAL' | 'ENCADRANT' | 'MEMBRECA' | 'SECRETAIRE';
export interface ChatMessage { id: number; senderUserId: number; senderName: string; senderRole?: ChatSenderRole; content: string; createdAt: string; }

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.server + '/chat';
  readonly unreadCounts = signal(new Map<number, number>());
  readonly totalUnread = computed(() => [...this.unreadCounts().values()].reduce((sum, count) => sum + count, 0));
  private notifications?: Subscription;
  private notificationUser?: number;
  private revision = 0;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.notifications?.unsubscribe());
  }

  monitorUnread(userId?: number): void {
    if (this.notificationUser === userId) return;
    this.notifications?.unsubscribe();
    this.notificationUser = userId;
    this.revision++;
    this.unreadCounts.set(new Map());
    if (userId === undefined) return;
    this.notifications = timer(0, 5000).pipe(
      exhaustMap(() => this.rooms().pipe(catchError(() => EMPTY)))
    ).subscribe();
  }

  rooms() {
    return defer(() => {
      const revision = this.revision;
      return this.http.get<ChatRoom[]>(this.url).pipe(tap(rooms => {
        if (revision === this.revision) {
          this.unreadCounts.set(new Map(rooms.map(room => [room.id, room.unreadCount ?? 0])));
        }
      }));
    });
  }

  markRead(id: number, lastMessageId: number) {
    return defer(() => {
      const user = this.notificationUser;
      this.revision++;
      return this.http.post<{ unreadCount: number }>(`${this.url}/${id}/read`, { lastMessageId }).pipe(tap(result => {
        if (user !== this.notificationUser) return;
        this.revision++;
        const counts = new Map(this.unreadCounts());
        counts.set(id, result.unreadCount);
        this.unreadCounts.set(counts);
      }));
    });
  }

  messages(id: number, cursor?: { beforeId?: number; afterId?: number }) {
    let params = new HttpParams();
    if (cursor?.beforeId) params = params.set('beforeId', cursor.beforeId);
    if (cursor?.afterId) params = params.set('afterId', cursor.afterId);
    return this.http.get<ChatMessage[]>(`${this.url}/${id}/messages`, { params });
  }

  send(id: number, content: string) {
    return this.http.post<ChatMessage>(`${this.url}/${id}/messages`, { content });
  }

}
