import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProfsComponent } from './profs.component';

describe('AdherentsComponent', () => {
  let component: ProfsComponent;
  let fixture: ComponentFixture<ProfsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProfsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProfsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
