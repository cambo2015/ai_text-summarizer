import { Component } from '@angular/core';
import { YoutubeDownloaderService } from '../../services/youtube-downloader-service';
import { FormsModule } from '@angular/forms';
import { AudioService } from '../../services/audioService';
import { Spinner } from "../spinner/spinner";
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-youtube-downloader',
  imports: [FormsModule, Spinner,NgIf],
  templateUrl: './youtube-downloader.html',
  styleUrl: './youtube-downloader.css'
})
export class YoutubeDownloader {

  public youtubeUrl: string = '';
  public msg = '';
  public loading: boolean = false;
  constructor(private youtubeService:YoutubeDownloaderService,private audioService:AudioService) { }

  downloadAudio(){
    this.loading = true;
    this.youtubeService.downloadAudio(this.youtubeUrl).subscribe({next:(response)=>{
      // console.log('Audio download link:', response);
      console.log(response)
      this.msg = response;
      this.audioService.repopulateFileList();
      this.loading = false;
      // You can implement further logic to handle the downloaded audio link
    },error:(error)=>{
      this.msg = 'Could not download audio. Try a different link or try again.';
      this.loading = false;
      // console.error('Error downloading audio:', error);
    }});
  }
}
