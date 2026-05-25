import { TestBed } from '@angular/core/testing';

import { YoutubeDownloaderService } from './youtube-downloader-service';

describe('YoutubeDownloaderService', () => {
  let service: YoutubeDownloaderService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(YoutubeDownloaderService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
