import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StpCheckoutbutton } from './stp-checkoutbutton';

describe('StpCheckoutbutton', () => {
  let component: StpCheckoutbutton;
  let fixture: ComponentFixture<StpCheckoutbutton>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StpCheckoutbutton]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StpCheckoutbutton);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
