import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdhesionsComponent } from './adhesions.component';

describe('AdhesionsComponent', () => {
  let component: AdhesionsComponent;
  let fixture: ComponentFixture<AdhesionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ AdhesionsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AdhesionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
