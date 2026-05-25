import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TranscriptionButton } from './transcription-button';

describe('TranscriptionButton', () => {
  let component: TranscriptionButton;
  let fixture: ComponentFixture<TranscriptionButton>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TranscriptionButton]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TranscriptionButton);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
