import { ChangeDetectorRef, Component, inject, Input } from '@angular/core'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { FileService } from 'src/app/_services/file.service';
import { Document } from 'src/app/models';
import { NgxExtendedPdfViewerModule } from 'ngx-extended-pdf-viewer';

export let pdfDefaultOptions = {
  externalLinkTarget: 0,
  renderer: 'canvas',
  assetsFolder: 'assets',
  workerSrc: () => './' + pdfDefaultOptions.assetsFolder + '/pdf.worker.js',
};



@Component({
    selector: 'modal-pdf',
    templateUrl: './modal-pdf.component.html',
    styleUrls: ['./modal-pdf.component.css'],
    imports: [NgxExtendedPdfViewerModule]
})
export class ModalPDFComponent {
  fileService = inject(FileService);

  activeModal = inject(NgbActiveModal);

  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  @Input()
  documentName!: string;

  @Input()
  adherentId!: number;

  pdfFile: Document = new Document;

  ngOnInit(): void {
    if (this.documentName) {
      this.getFile();
    }
  }

  messagePdf: string = ""
  isFailedPdf: boolean=false;
  isLoaded: boolean=false;
  loadFile(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    this.isLoaded = false;
    this.isFailedPdf = false;
    this.messagePdf = "";

    if (file) {
      if (file.size > 1000000) {
        this.isFailedPdf = true;
        this.messagePdf = "La taille maximale du fichier est de 1Mo"
      } else if (file.type != "application/pdf") {
        this.isFailedPdf = true;
        this.messagePdf = "Le fichier doit etre un PDF"
      } else {
        const reader = new FileReader();
        reader.onload = (e: any) => {
          this.pdfFile.nom = file.name
          this.pdfFile.file = file;
          this.pdfFile.content = e.target.result;
          this.isLoaded = true;
          this.changeDetectorRef.markForCheck();
        }
        reader.onerror = () => {
          this.isFailedPdf = true;
          this.messagePdf = "Le fichier PDF n'a pas pu être lu";
          this.changeDetectorRef.markForCheck();
        }
        reader.readAsDataURL(file);

      }
    }
  }

  getFile() {
    console.log(this.documentName)

  }


}
