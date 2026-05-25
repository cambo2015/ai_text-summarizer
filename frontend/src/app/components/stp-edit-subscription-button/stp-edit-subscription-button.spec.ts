import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StpEditSubscriptionButton } from './stp-edit-subscription-button';

describe('StpEditSubscriptionButton', () => {
  let component: StpEditSubscriptionButton;
  let fixture: ComponentFixture<StpEditSubscriptionButton>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StpEditSubscriptionButton]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StpEditSubscriptionButton);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
