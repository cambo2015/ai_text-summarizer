import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CustomSummaryTemplate } from './custom-summary-template';

describe('CustomSummaryTemplate', () => {
  let component: CustomSummaryTemplate;
  let fixture: ComponentFixture<CustomSummaryTemplate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomSummaryTemplate]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CustomSummaryTemplate);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
