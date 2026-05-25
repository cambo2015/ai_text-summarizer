import { Component, EventEmitter, Output } from '@angular/core';
import { YoutubeDownloaderService } from '../../services/youtube-downloader-service';
import { NgIf } from "@angular/common";
import { Spinner } from "../spinner/spinner";
import { AudioService, AudioUploadResult, GenericErrorResponse } from '../../services/audioService';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-upload-audio',
  imports: [NgIf, Spinner],
  templateUrl: './upload-audio.html',
  styleUrl: './upload-audio.css'
})
export class UploadAudio {
  
  public selectedFile: File | null = null;
  public text : string = '';
  public loading: boolean = false;
  public errorMessage: string = '';
  
  @Output() modalClosed = new EventEmitter<void>();

  // constructor(private youtubeService:YoutubeDownloaderService,private audioService:AudioService) {}
  constructor(private audioService:AudioService) {}

  onHidden() {
    this.modalClosed.emit();
  }

  onFileSelected(event: any) {
      this.selectedFile = event.target.files[0];
  }

  upload(){
    this.loading = true;
      if(!this.selectedFile){
        return ;
      }
      
      const formData = new FormData();
      formData.append("file", this.selectedFile);
      // You can now send formData to your backend using HttpClient
      this.audioService.uploadAudio(this.selectedFile).subscribe({
        next: (response) => {
          console.log('Upload successful:', response);
         this.text = response.message ?? "Upload successful.";
         this.loading = false;
         this.audioService.repopulateFileList();
        },
        error: (err: HttpErrorResponse  ) => {
          console.log('Upload failed:', err);
          const errorData = err.error
          console.log(errorData)
          this.loading = false;
          console.log("Message: "+errorData.message)
          this.errorMessage = errorData.message ?? "";
        }
      });
  }

  handleResponse(result: AudioUploadResult) {
      if ('fileName' in result) {
          console.log("Success! File is: " + result.fileName);
      } else if ('message' in result) {
          // TypeScript now knows this must be the GenericErrorResponse
          this.errorMessage = result.message ??""; 
          console.log("Error from backend: " + result.message);
      }
    }
}
