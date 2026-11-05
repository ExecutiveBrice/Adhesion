import { ChangeDetectorRef, Component, EventEmitter, inject, Input, OnDestroy, Output } from '@angular/core';

@Component({
  selector: 'app-photo-materiel',
  templateUrl: './photo-materiel.component.html',
  styleUrl: './photo-materiel.component.css'
})
export class PhotoMaterielComponent implements OnDestroy {
  private readonly changeDetector = inject(ChangeDetectorRef);
  private lecteur?: FileReader;
  private decodage?: HTMLImageElement;
  private detruit = false;
  private deplacement?: { id: number; x: number; y: number };
  @Input() image: string | null | undefined;
  @Input() disabled = false;
  @Output() imageChange = new EventEmitter<string>();
  // Le formulaire attend aussi la validation ou l’annulation du recadrage.
  @Output() chargementChange = new EventEmitter<boolean>();
  source?: HTMLImageElement;
  chargement = false;
  erreur = '';
  zoom = 1;
  horizontal = 50;
  vertical = 50;

  get cadrage(): { x: number; y: number; largeur: number; hauteur: number } {
    if (!this.source) return { x: 0, y: 0, largeur: 1, hauteur: 1 };
    const largeur = Math.min(this.source.naturalWidth, this.source.naturalHeight * 4 / 3) / this.zoom;
    const hauteur = largeur * 3 / 4;
    return {
      x: (this.source.naturalWidth - largeur) * this.horizontal / 100,
      y: (this.source.naturalHeight - hauteur) * this.vertical / 100,
      largeur, hauteur
    };
  }

  selectionner(input: HTMLInputElement): void {
    const fichier = input.files?.[0];
    if (!fichier || this.disabled || this.chargement || this.source) return;
    this.erreur = '';
    if (!['image/jpeg', 'image/png'].includes(fichier.type) || fichier.size > 5 * 1024 * 1024) {
      this.erreur = 'Choisissez une photo JPEG ou PNG de 5 Mo maximum.';
      input.value = '';
      return;
    }
    const lecteur = this.lecteur = new FileReader();
    this.chargement = true;
    this.chargementChange.emit(true);
    lecteur.onload = () => this.decoder(lecteur.result as string);
    lecteur.onerror = () => this.echec();
    lecteur.readAsDataURL(fichier);
    input.value = '';
  }

  recadrer(): void {
    if (!this.image || this.disabled || this.chargement || this.source) return;
    this.erreur = '';
    this.chargement = true;
    this.chargementChange.emit(true);
    this.decoder(this.image);
  }

  private decoder(url: string): void {
    const source = this.decodage = new Image();
    source.onload = () => {
      if (this.detruit) return;
      if (!source.naturalWidth || !source.naturalHeight || source.naturalWidth * source.naturalHeight > 40_000_000) {
        this.echec('La photo ne doit pas dépasser 40 mégapixels.');
        return;
      }
      this.source = source;
      this.zoom = 1;
      this.horizontal = this.vertical = 50;
      this.chargement = false;
      this.changeDetector.markForCheck();
    };
    source.onerror = () => this.echec();
    source.src = url;
  }

  valider(): void {
    if (!this.source || this.disabled) return;
    try {
      const zone = this.cadrage;
      const canvas = document.createElement('canvas');
      canvas.width = 640;
      canvas.height = 480;
      const dessin = canvas.getContext('2d');
      if (!dessin) throw new Error('Canvas indisponible');
      dessin.fillStyle = '#fff';
      dessin.fillRect(0, 0, 640, 480);
      dessin.drawImage(this.source, zone.x, zone.y, zone.largeur, zone.hauteur, 0, 0, 640, 480);
      this.imageChange.emit(canvas.toDataURL('image/jpeg', 0.9));
      this.annuler();
    } catch {
      this.erreur = 'Le recadrage n’a pas pu être appliqué. Réessayez.';
    }
  }

  annuler(): void {
    if (this.disabled) return;
    this.source = undefined;
    this.decodage = undefined;
    this.deplacement = undefined;
    this.chargementChange.emit(false);
    this.changeDetector.markForCheck();
  }

  debutDeplacement(event: PointerEvent): void {
    if (!this.source || this.disabled || !event.isPrimary || event.button !== 0) return;
    event.preventDefault();
    (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
    this.deplacement = { id: event.pointerId, x: event.clientX, y: event.clientY };
  }

  deplacer(event: PointerEvent): void {
    if (!this.source || this.disabled || this.deplacement?.id !== event.pointerId) return;
    const cadre = (event.currentTarget as HTMLElement).getBoundingClientRect();
    if (!cadre.width || !cadre.height) return;
    const zone = this.cadrage;
    const margeX = this.source.naturalWidth - zone.largeur;
    const margeY = this.source.naturalHeight - zone.hauteur;
    if (margeX > 0) this.horizontal = this.borner(this.horizontal - (event.clientX - this.deplacement.x) / cadre.width * zone.largeur / margeX * 100);
    if (margeY > 0) this.vertical = this.borner(this.vertical - (event.clientY - this.deplacement.y) / cadre.height * zone.hauteur / margeY * 100);
    this.deplacement = { id: event.pointerId, x: event.clientX, y: event.clientY };
  }

  finDeplacement(event: PointerEvent): void {
    if (this.deplacement?.id === event.pointerId) this.deplacement = undefined;
  }

  private borner(valeur: number): number { return Math.max(0, Math.min(100, valeur)); }

  retirer(input: HTMLInputElement): void {
    if (this.disabled || this.chargement || this.source) return;
    input.value = '';
    this.erreur = '';
    this.imageChange.emit('');
  }

  private echec(message = 'La photo est illisible. Choisissez une autre photo JPEG ou PNG.'): void {
    if (this.detruit) return;
    this.erreur = message;
    this.chargement = false;
    this.decodage = undefined;
    this.chargementChange.emit(false);
    this.changeDetector.markForCheck();
  }

  ngOnDestroy(): void {
    this.detruit = true;
    this.lecteur?.abort();
    if (this.decodage) this.decodage.onload = this.decodage.onerror = null;
  }
}
