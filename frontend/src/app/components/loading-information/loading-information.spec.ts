import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoadingInformation } from './loading-information';

describe('LoadingInformation', () => {
  let component: LoadingInformation;
  let fixture: ComponentFixture<LoadingInformation>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoadingInformation]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LoadingInformation);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
