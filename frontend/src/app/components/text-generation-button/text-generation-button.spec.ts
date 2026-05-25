import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TextGenerationButton } from './text-generation-button';

describe('TextGenerationButton', () => {
  let component: TextGenerationButton;
  let fixture: ComponentFixture<TextGenerationButton>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TextGenerationButton]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TextGenerationButton);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
