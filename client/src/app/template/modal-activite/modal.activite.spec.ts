import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModalActivite } from './modal.activite';

describe('ModalActivite', () => {
  let component: ModalActivite;
  let fixture: ComponentFixture<ModalActivite>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ModalActivite ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ModalActivite);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
