import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, Input, NgZone } from '@angular/core';
import { TranscriptionService } from '../../services/transcription-service';
import { AudioSpecs } from '../../services/audioService';
import { NgIf } from "@angular/common";
import { Spinner } from "../spinner/spinner";
import { CommonWordChips } from "../common-word-chips/common-word-chips";
import { LoadingInformation } from "../loading-information/loading-information";

@Component({
  selector: 'app-transcription-button',
  imports: [NgIf, Spinner, CommonWordChips, LoadingInformation],
  templateUrl: './transcription-button.html',
  styleUrl: './transcription-button.css'
})
export class TranscriptionButton {

  public readonly modalId = `transcript-modal-${crypto.randomUUID()}`;
  @Input() audioSpecs: AudioSpecs | undefined;
  public transcript: string = '';
  private source?: EventSource;
  public isLoading = false;
  public errorText = '';

  constructor(private http: HttpClient, private transcriptionService: TranscriptionService, private zone: NgZone) { }

  ngOnInit() {
    // check to see if there is a transcription
    // console.log(this.audioSpecs);
  }

  connect(
    jobId: string,
    onMessage: (msg: string) => void,
    onError?: (err: any) => void,
    onComplete?: () => void
  ): EventSource {

    this.source = new EventSource(`https://localhost:8443/api/text-extractor/jobs/${jobId}/stream`, { withCredentials: true });

    this.source.onmessage = event => {
      this.zone.run(() => {
        onMessage(event.data);

        if (event.data === 'Completed' || event.data === 'Failed') {
          // console.log("stream completed");
          if (this.source) {
            this.source.close();
          }
          onComplete?.();
        }
      });
    };

    this.source.onerror = err => {
      this.zone.run(() => {
        onError?.(err);   // ✅ PASS THE ERROR, NOT A FUNCTION
        if (this.source) {
          this.source.close();
        }
      });
    };

    return this.source; // ✅ REQUIRED
  }

  getTranscript(jobId:string) {
    if(this.audioSpecs?.id){
       this.transcriptionService.getTranscript(jobId).subscribe({
        next: (transcript) => {
          // console.log('Transcript received:', transcript);
          this.transcript = transcript;
          this.isLoading = false;
        },
        error: (error) => {
          // console.error('Error fetching transcript:', error);
          this.transcript = error.message;
          this.isLoading = false;
        }
      });
    }
  }

  clickTranscribeData() {
    //see if transcription exists if the user has a jobId
    this.isLoading = true;
    if (this.audioSpecs?.id) {
      this.getTranscript(this.audioSpecs.id);
    }
    //if not, transcribe the data by calling the transcription service transcribeAudio method
    else {
      // const path = "./audio/"+this.audioSpecs?.name;
      const audioFile =this.audioSpecs?.name;
      console.log("tb 1. this is the audio path: " + audioFile);
      if (audioFile) {
        // console.log("this is the path: "+path);
        this.isLoading = true;
        this.transcriptionService.create(audioFile).subscribe({
          next: (response) => {
            // console.log('Transcription job started with ID:', response.jobId);
            //subscribe to the events
            if(this.audioSpecs){
              this.audioSpecs.id = response.jobId;
            }
            this.connect(
              response.jobId,
              msg => {
                // console.log('SSE message:', msg);
                // update UI state here if you want
                // console.log("this is the event message"+msg);
                this.transcript = msg;
              },
              err => {
                // console.error('SSE error:', err);
                console.log("2. final error transcribing audio:"+err)
                this.isLoading = false;
                this.errorText = 'Error during transcription: ' + err.message;
              },
              () => {
                // console.log('SSE completed');
                // fetch transcript or update UI here
                this.getTranscript(response.jobId);
              }
            );
          },
          error: (error:HttpErrorResponse) => {
            console.log("3. final error transcribing audio:"+error.error)
            this.isLoading = false;
            
            const errorObj = error.error;
            
            if(errorObj['message']){
              this.errorText = errorObj['message'];
            }
            console.error('Error starting transcription job:', error);
          }
        })
      }
    }
  }

  printText() {
    // Open a new window
    const newWindow = window.open('', '_blank');
    
    if (!newWindow) {
        alert("Pop-ups blocked. Please allow pop-ups for printing.");
        return;
    }

    // Write the string content to the new window's document
    newWindow.document.write('<html><head><title>Print Document</title></head><body>');
    newWindow.document.write(this.transcript); // Insert the string
    newWindow.document.write('</body></html>');
    newWindow.document.close();

   newWindow.print();
  }

  
}
