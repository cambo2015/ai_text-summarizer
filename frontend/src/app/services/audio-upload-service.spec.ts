import { TestBed } from '@angular/core/testing';

import { AudioUploadService } from './audio-upload-service';

describe('AudioUploadService', () => {
  let service: AudioUploadService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AudioUploadService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
