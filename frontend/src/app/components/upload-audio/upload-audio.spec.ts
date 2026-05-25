import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UploadAudio } from './upload-audio';

describe('UploadAudio', () => {
  let component: UploadAudio;
  let fixture: ComponentFixture<UploadAudio>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UploadAudio]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UploadAudio);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
