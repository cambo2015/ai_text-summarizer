import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SummarizationButton } from './summarization-button';

describe('SummarizationButton', () => {
  let component: SummarizationButton;
  let fixture: ComponentFixture<SummarizationButton>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SummarizationButton]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SummarizationButton);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
