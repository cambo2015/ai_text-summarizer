import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VerifySuccess } from './verify-success';

describe('VerifySuccess', () => {
  let component: VerifySuccess;
  let fixture: ComponentFixture<VerifySuccess>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VerifySuccess]
    })
    .compileComponents();

    fixture = TestBed.createComponent(VerifySuccess);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
