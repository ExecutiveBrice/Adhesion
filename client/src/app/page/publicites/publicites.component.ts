import { Component, DestroyRef, inject, OnDestroy, OnInit, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Publicite, PubliciteService } from '../../_services/publicite.service';
import { PubliciteImageComponent } from './publicite-image.component';
import { ImageCroppedEvent, ImageCropperComponent, ImageTransform } from 'ngx-image-cropper';
import { ParamService } from '../../_services/param.service';
import { SectionConfiguration } from '../../models';

@Component({
  selector: 'app-publicites',
  imports: [FormsModule, PubliciteImageComponent, ImageCropperComponent],
  templateUrl: './publicites.component.html',
  styleUrl: './publicites.component.css'
})
export class PublicitesComponent implements OnInit, OnDestroy {
  private readonly service = inject(PubliciteService);
  private readonly paramService = inject(ParamService);
  private readonly destroyRef = inject(DestroyRef);
  readonly publicites = signal<Publicite[]>([]);
  readonly sections = signal<SectionConfiguration[]>([]);
  readonly sectionsLoading = signal(true);
  readonly sectionsLoadError = signal(false);
  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly saving = signal(false);
  readonly validating = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly editingId = signal<number | null>(null);
  readonly error = signal('');
  readonly success = signal('');
  readonly preview = signal('');
  readonly sourceFile = signal<File | undefined>(undefined);
  readonly pendingDelete = signal<Publicite | null>(null);
  readonly cropper = viewChild(ImageCropperComponent);
  description = '';
  facebookUrl = '';
  instagramUrl = '';
  websiteUrl = '';
  selectedSectionIds: number[] = [];
  displayOrder = 0;
  disabled = false;
  file?: File;
  zoom = 1;
  transform: ImageTransform = { scale: 1 };
  private selectionVersion = 0;

  ngOnInit(): void {
    this.load();
    this.paramService.getSections().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: sections => {
        this.sections.set(sections.filter(section => section.id != null));
        this.sectionsLoading.set(false);
      },
      error: () => {
        this.sectionsLoading.set(false);
        this.sectionsLoadError.set(true);
        this.error.set('Impossible de charger la liste des sections. La publication est désactivée.');
      }
    });
  }

  toggleSection(sectionId: number, checked: boolean): void {
    this.selectedSectionIds = checked
      ? [...new Set([...this.selectedSectionIds, sectionId])]
      : this.selectedSectionIds.filter(id => id !== sectionId);
  }

  sectionName(sectionId: number): string {
    return this.sections().find(section => section.id === sectionId)?.nom ?? `Section ${sectionId}`;
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.service.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: items => { this.publicites.set(items); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); }
    });
  }

  async selectFile(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    const version = ++this.selectionVersion;
    this.clearPreview();
    this.sourceFile.set(undefined);
    this.zoom = 1;
    this.transform = { scale: 1 };
    this.error.set('');
    this.success.set('');
    this.validating.set(false);
    if (!file) return;
    if (!['image/png', 'image/jpeg'].includes(file.type) || file.size > 10 * 1024 * 1024 || !file.size) {
      this.error.set('Choisissez une image PNG ou JPEG de 10 Mo maximum.');
      input.value = '';
      return;
    }
    this.validating.set(true);
    const url = URL.createObjectURL(file);
    const image = new Image();
    try {
      await new Promise<void>((resolve, reject) => {
        image.onload = () => resolve();
        image.onerror = () => reject(new Error('L’image est illisible. Choisissez une autre image.'));
        image.src = url;
      });
      if (version !== this.selectionVersion) return;
      if (image.naturalWidth * image.naturalHeight > 16_000_000) {
        throw new Error('L’image ne doit pas dépasser 16 millions de pixels.');
      }
      this.sourceFile.set(file);
    } catch (error) {
      if (version === this.selectionVersion) {
        this.error.set((error as Error).message);
        input.value = '';
      }
    } finally {
      URL.revokeObjectURL(url);
      if (version === this.selectionVersion) this.validating.set(false);
    }
  }

  setZoom(value: string): void {
    this.zoom = Number(value);
    this.transform = { ...this.transform, scale: this.zoom };
  }

  async imageCropped(event: ImageCroppedEvent): Promise<void> {
    if (!this.sourceFile() || !event.blob || this.saving()) return;
    const version = ++this.selectionVersion;
    this.clearPreview();
    this.validating.set(true);
    this.error.set('');
    try {
      const blob = await this.normalizeCrop(event.blob);
      if (version !== this.selectionVersion) return;
      if (blob.size > 1024 * 1024) {
        throw new Error('Le cadrage produit une image de plus de 1 Mo. Choisissez une zone plus simple.');
      }
      this.file = new File([blob], 'publicite.jpg', { type: 'image/jpeg' });
      this.preview.set(URL.createObjectURL(this.file));
    } catch (error) {
      if (version === this.selectionVersion) this.error.set((error as Error).message || 'Le recadrage a échoué.');
    } finally {
      if (version === this.selectionVersion) this.validating.set(false);
    }
  }

  loadImageFailed(input: HTMLInputElement): void {
    ++this.selectionVersion;
    this.clearPreview();
    this.sourceFile.set(undefined);
    input.value = '';
    this.validating.set(false);
    this.error.set('L’image est illisible. Choisissez une autre image.');
  }

  private async normalizeCrop(blob: Blob): Promise<Blob> {
    const imageUrl = URL.createObjectURL(blob);
    try {
      const image = new Image();
      await new Promise<void>((resolve, reject) => {
        image.onload = () => resolve();
        image.onerror = () => reject(new Error('Le recadrage a échoué.'));
        image.src = imageUrl;
      });
      const canvas = document.createElement('canvas');
      canvas.width = 1900;
      canvas.height = 900;
      const context = canvas.getContext('2d');
      if (!context) throw new Error('Le recadrage est indisponible dans ce navigateur.');
      context.fillStyle = '#ffffff';
      context.fillRect(0, 0, canvas.width, canvas.height);
      context.drawImage(image, 0, 0, canvas.width, canvas.height);
      let result = blob;
      for (const quality of [0.86, 0.72, 0.56, 0.4]) {
        result = await new Promise<Blob>((resolve, reject) =>
          canvas.toBlob(value => value ? resolve(value) : reject(new Error('Le recadrage a échoué.')), 'image/jpeg', quality));
        if (result.size <= 1024 * 1024) break;
      }
      return result;
    } finally {
      URL.revokeObjectURL(imageUrl);
    }
  }

  async publish(input: HTMLInputElement): Promise<void> {
    if (this.saving()) return;
    if (this.sectionsLoading() || this.sectionsLoadError()) {
      this.error.set('Attendez le chargement des sections avant de publier.');
      return;
    }
    if (!this.sourceFile() && this.editingId() === null) {
      this.error.set('Choisissez une image à publier.');
      return;
    }
    if (!this.description.trim() || this.description.trim().length > 200) {
      this.error.set('Renseignez une description de 1 à 200 caractères.');
      return;
    }
    if (!Number.isInteger(this.displayOrder) || this.displayOrder < 0 || this.displayOrder > 10_000) {
      this.error.set('Le numéro d’ordre doit être compris entre 0 et 10 000.');
      return;
    }
    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    let file = this.file;
    if (this.sourceFile() && (!file || this.validating())) {
      ++this.selectionVersion;
      this.clearPreview();
      this.validating.set(false);
      try {
        const result = await this.cropper()?.crop('blob');
        if (!result?.blob) throw new Error('L’image est encore en cours de chargement. Réessayez dans un instant.');
        const blob = await this.normalizeCrop(result.blob);
        if (blob.size > 1024 * 1024) throw new Error('Le cadrage produit une image de plus de 1 Mo. Choisissez une zone plus simple.');
        file = new File([blob], 'publicite.jpg', { type: 'image/jpeg' });
      } catch (error) {
        this.saving.set(false);
        this.error.set((error as Error).message || 'Le recadrage a échoué.');
        return;
      }
    }
    const editingId = this.editingId();
    const links = {
      facebookUrl: this.facebookUrl,
      instagramUrl: this.instagramUrl,
      websiteUrl: this.websiteUrl
    };
    const request = editingId === null
      ? this.service.create(this.description, file!, links, this.selectedSectionIds, this.displayOrder, !this.disabled)
      : this.service.update(editingId, this.description, file, links, this.selectedSectionIds,
          this.displayOrder, !this.disabled);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: item => {
        const saved = { ...item, imageVersion: file ? Date.now() : undefined };
        this.publicites.update(items => this.sortPublicites(editingId === null
          ? [saved, ...items]
          : items.map(current => current.id === saved.id ? saved : current)));
        this.resetForm(input);
        this.saving.set(false);
        this.success.set(editingId === null
          ? 'La publicité est maintenant enregistrée.'
          : 'La publicité a été modifiée.');
      },
      error: () => {
        this.saving.set(false);
        this.error.set(editingId === null
          ? 'La publication a échoué. Vérifiez le format de l’image et réessayez.'
          : 'La modification a échoué. Vérifiez les informations et réessayez.');
      }
    });
  }

  startEdit(item: Publicite, input: HTMLInputElement): void {
    ++this.selectionVersion;
    this.clearPreview();
    this.sourceFile.set(undefined);
    input.value = '';
    this.editingId.set(item.id);
    this.description = item.description;
    this.facebookUrl = item.facebookUrl ?? '';
    this.instagramUrl = item.instagramUrl ?? '';
    this.websiteUrl = item.websiteUrl ?? '';
    this.selectedSectionIds = [...item.sectionIds];
    this.displayOrder = item.displayOrder;
    this.disabled = !item.active;
    this.zoom = 1;
    this.transform = { scale: 1 };
    this.error.set('');
    this.success.set('');
    document.getElementById('publicite-description')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    document.getElementById('publicite-description')?.focus();
  }

  cancelEdit(input: HTMLInputElement): void {
    this.resetForm(input);
    this.error.set('');
    this.success.set('');
  }

  private resetForm(input: HTMLInputElement): void {
    this.editingId.set(null);
    this.description = '';
    this.facebookUrl = '';
    this.instagramUrl = '';
    this.websiteUrl = '';
    this.selectedSectionIds = [];
    this.displayOrder = 0;
    this.disabled = false;
    input.value = '';
    this.clearPreview();
    this.sourceFile.set(undefined);
    this.zoom = 1;
    this.transform = { scale: 1 };
  }

  private sortPublicites(items: Publicite[]): Publicite[] {
    return [...items].sort((left, right) => left.displayOrder - right.displayOrder || right.id - left.id);
  }

  remove(item: Publicite): void {
    if (this.deletingId() !== null) return;
    this.deletingId.set(item.id);
    this.error.set('');
    this.success.set('');
    this.service.delete(item.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.publicites.update(items => items.filter(current => current.id !== item.id));
        this.pendingDelete.set(null);
        this.deletingId.set(null);
        this.success.set('La publicité a été supprimée de l’accueil.');
      },
      error: () => { this.deletingId.set(null); this.error.set('La suppression a échoué. Réessayez.'); }
    });
  }

  private clearPreview(): void {
    if (this.preview()) URL.revokeObjectURL(this.preview());
    this.preview.set('');
    this.file = undefined;
  }

  ngOnDestroy(): void { ++this.selectionVersion; this.clearPreview(); this.sourceFile.set(undefined); }
}
