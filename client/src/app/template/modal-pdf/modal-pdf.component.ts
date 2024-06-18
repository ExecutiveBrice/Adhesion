import { Component, inject, Input } from '@angular/core'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { FileService } from 'src/app/_services/file.service';
import { Document } from 'src/app/models';

export let pdfDefaultOptions = {
  externalLinkTarget: 0,
  renderer: 'canvas',
  assetsFolder: 'assets',
  workerSrc: () => './' + pdfDefaultOptions.assetsFolder + '/pdf.worker.js',
};



@Component({
  selector: 'modal-pdf',
  templateUrl: './modal-pdf.component.html',
  styleUrls: ['./modal-pdf.component.css']
})
export class ModalPDFComponent {
  activeModal = inject(NgbActiveModal);

  @Input()
  documentName!: string;

  @Input()
  adherentId!: number;

  pdfFile: Document = new Document;

  constructor(
    public fileService: FileService) { }

  ngOnInit(): void {
    if (this.documentName) {
      this.getFile();
    }
  }

  messagePdf: string = ""
  isFailedPdf: boolean=false;
  isLoaded: boolean=false;
  loadFile(event: any) {
    if (event.target.files && event.target.files[0]) {
      if (event.target.files[0].size > 1000000) {
        this.isFailedPdf = true;
        this.messagePdf = "La taille maximale du fichier est de 1Mo"
      } else if (event.target.files[0].type != "application/pdf") {
        this.isFailedPdf = true;
        this.messagePdf = "Le fichier doit etre un PDF"
      } else {
        this.isFailedPdf = false;
        var reader = new FileReader();
        reader.onload = (e: any) => {
          this.isLoaded = true;
          this.pdfFile.nom = event.target.files[0].name
          console.log(e.target)
          this.pdfFile.content = e.target.result.replace("data:application/pdf;base64,", "");
        }
        reader.readAsDataURL(event.target.files[0]);

      }
    }
  }

  getFile() {
    console.log(this.documentName)

  }


}

