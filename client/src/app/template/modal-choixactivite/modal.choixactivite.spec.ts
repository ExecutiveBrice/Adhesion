import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModalChoixActivite } from './modal.choixactivite';

describe('ModalChoixActivite', () => {
  let component: ModalChoixActivite;
  let fixture: ComponentFixture<ModalChoixActivite>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ModalChoixActivite ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ModalChoixActivite);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
