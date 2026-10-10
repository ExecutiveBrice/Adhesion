import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LoginPageService {
  readonly view = signal<'connexion' | 'recuperation' | 'inscription'>('connexion');
  readonly inscriptionOpen = signal(false);
}
