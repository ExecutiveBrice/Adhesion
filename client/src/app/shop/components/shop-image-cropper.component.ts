import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild, inject } from '@angular/core';
import { optimizeShopImage } from './shop-image-optimization';

@Component({
  selector: 'app-shop-image-cropper',
  templateUrl: './shop-image-cropper.component.html',
  styleUrl: './shop-image-cropper.component.css'
})
export class ShopImageCropperComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input({ required: true }) file!: File;
  @Output() readonly cropped = new EventEmitter<File>();
  @Output() readonly cancelled = new EventEmitter<void>();
  @ViewChild('dialog', { static: true }) private dialog!: ElementRef<HTMLDialogElement>;
  private readonly changeDetector = inject(ChangeDetectorRef);
  private sourceUrl = '';
  private destroyed = false;
  private drag?: { id: number; x: number; y: number };
  source?: HTMLImageElement;
  loading = true;
  exporting = false;
  error = '';
  zoom = 1;
  horizontal = 50;
  vertical = 50;

  get minZoom(): number {
    if (!this.source) return 1;
    const width = this.source.naturalWidth;
    const heightAsWidth = this.source.naturalHeight * 4 / 3;
    return Math.min(width, heightAsWidth) / Math.max(width, heightAsWidth);
  }

  get canMoveHorizontally(): boolean {
    return !!this.source && Math.abs(this.source.naturalWidth - this.crop.width) > 0.001;
  }

  get canMoveVertically(): boolean {
    return !!this.source && Math.abs(this.source.naturalHeight - this.crop.height) > 0.001;
  }

  get crop(): { x: number; y: number; width: number; height: number } {
    if (!this.source) return { x: 0, y: 0, width: 1, height: 1 };
    const width = Math.min(this.source.naturalWidth, this.source.naturalHeight * 4 / 3) / this.zoom;
    const height = width * 3 / 4;
    return {
      x: (this.source.naturalWidth - width) * this.horizontal / 100,
      y: (this.source.naturalHeight - height) * this.vertical / 100,
      width, height
    };
  }

  ngOnInit(): void {
    const source = new Image();
    this.sourceUrl = URL.createObjectURL(this.file);
    source.onload = () => {
      if (this.destroyed) return;
      this.loading = false;
      if (!source.naturalWidth || !source.naturalHeight) {
        this.error = 'Cette image est illisible. Choisissez une autre image.';
      } else {
        this.source = source;
      }
      this.changeDetector.markForCheck();
    };
    source.onerror = () => {
      if (this.destroyed) return;
      this.loading = false;
      this.error = 'Cette image est illisible. Choisissez une autre image.';
      this.changeDetector.markForCheck();
    };
    source.src = this.sourceUrl;
  }

  ngAfterViewInit(): void {
    this.dialog.nativeElement.showModal();
  }

  fitImage(): void {
    if (!this.source || this.exporting) return;
    this.zoom = this.minZoom;
    this.horizontal = this.vertical = 50;
  }

  async confirm(): Promise<void> {
    if (!this.source || this.exporting) return;
    this.exporting = true;
    this.error = '';
    try {
      const canvas = document.createElement('canvas');
      canvas.width = 800;
      canvas.height = 600;
      const context = canvas.getContext('2d');
      if (!context) throw new Error('Canvas indisponible');
      const crop = this.crop;
      context.fillStyle = '#fff';
      context.fillRect(0, 0, canvas.width, canvas.height);
      // La même transformation que l’aperçu, y compris les marges blanches au dézoom.
      context.drawImage(this.source,
        -crop.x / crop.width * canvas.width, -crop.y / crop.height * canvas.height,
        this.source.naturalWidth / crop.width * canvas.width, this.source.naturalHeight / crop.height * canvas.height);
      const blob = await optimizeShopImage(canvas);
      if (this.destroyed) return;
      this.cropped.emit(new File([blob], `${this.file.name.replace(/\.[^.]+$/, '')}-recadree.jpg`, { type: 'image/jpeg' }));
    } catch {
      if (!this.destroyed) this.exportFailed();
    }
  }

  cancel(): void {
    this.cancelled.emit();
  }

  startDrag(event: PointerEvent): void {
    if (!this.source || this.exporting || !event.isPrimary || event.button !== 0) return;
    event.preventDefault();
    (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
    this.drag = { id: event.pointerId, x: event.clientX, y: event.clientY };
  }

  moveDrag(event: PointerEvent): void {
    if (!this.source || this.exporting || this.drag?.id !== event.pointerId) return;
    const frame = (event.currentTarget as HTMLElement).getBoundingClientRect();
    if (!frame.width || !frame.height) return;
    const crop = this.crop;
    const marginX = this.source.naturalWidth - crop.width;
    const marginY = this.source.naturalHeight - crop.height;
    if (this.canMoveHorizontally) this.horizontal = this.clamp(this.horizontal - (event.clientX - this.drag.x) / frame.width * crop.width / marginX * 100);
    if (this.canMoveVertically) this.vertical = this.clamp(this.vertical - (event.clientY - this.drag.y) / frame.height * crop.height / marginY * 100);
    this.drag = { id: event.pointerId, x: event.clientX, y: event.clientY };
  }

  endDrag(event: PointerEvent): void {
    if (this.drag?.id === event.pointerId) this.drag = undefined;
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.dialog.nativeElement.close();
    URL.revokeObjectURL(this.sourceUrl);
  }

  private clamp(value: number): number { return Math.max(0, Math.min(100, value)); }

  private exportFailed(): void {
    this.exporting = false;
    this.error = 'Le recadrage n’a pas pu être appliqué. Réessayez avec une autre image.';
    this.changeDetector.markForCheck();
  }
}
