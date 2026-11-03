import { TestBed } from '@angular/core/testing';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { UtilService } from './util.service';

describe('Ouverture différée de la modale PDF', () => {
  let modal: jasmine.SpyObj<NgbModal>;
  let service: UtilService;

  beforeEach(() => {
    modal = jasmine.createSpyObj<NgbModal>('NgbModal', ['open']);
    TestBed.configureTestingModule({ providers: [{ provide: NgbModal, useValue: modal }] });
    service = TestBed.inject(UtilService);
  });

  it('transmet les paramètres et le document validé après chargement du composant', async () => {
    const document = { nom: 'justificatif.pdf' };
    const componentInstance: any = {};
    modal.open.and.returnValue({ componentInstance, result: Promise.resolve(document) } as any);

    const result = service.openModalPDF('justificatif.pdf', 42);
    expect(modal.open).not.toHaveBeenCalled();

    expect(await result).toBe(document);
    expect(modal.open).toHaveBeenCalledWith(jasmine.any(Function), jasmine.objectContaining({
      size: 'xl', backdrop: 'static', scrollable: true
    }));
    expect(componentInstance.documentName).toBe('justificatif.pdf');
    expect(componentInstance.adherentId).toBe(42);
  });

  it('propage la fermeture sans validation au code appelant', async () => {
    modal.open.and.callFake(() => ({
      componentInstance: {}, result: Promise.reject('annulation')
    }) as any);

    await expectAsync(service.openModalPDF(undefined, 42)).toBeRejectedWith('annulation');
  });
});
