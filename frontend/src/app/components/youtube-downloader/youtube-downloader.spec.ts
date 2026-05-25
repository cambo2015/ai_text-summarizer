import { ComponentFixture, TestBed } from '@angular/core/testing';

import { YoutubeDownloader } from './youtube-downloader';

describe('YoutubeDownloader', () => {
  let component: YoutubeDownloader;
  let fixture: ComponentFixture<YoutubeDownloader>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [YoutubeDownloader]
    })
    .compileComponents();

    fixture = TestBed.createComponent(YoutubeDownloader);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
