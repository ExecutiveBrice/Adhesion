import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModalPDFComponent } from './modal-pdf.component';

describe('ModalPDFComponent', () => {
  let component: ModalPDFComponent;
  let fixture: ComponentFixture<ModalPDFComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ModalPDFComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ModalPDFComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});