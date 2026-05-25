import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AudioFiles } from './audio-files';

describe('AudioFiles', () => {
  let component: AudioFiles;
  let fixture: ComponentFixture<AudioFiles>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AudioFiles]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AudioFiles);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
