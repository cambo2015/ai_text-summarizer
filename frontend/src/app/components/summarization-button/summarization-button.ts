import { Component, Input, NgZone } from '@angular/core';
import { SummarizationService } from '../../services/summarization-service';
import { AudioService, AudioSpecs } from '../../services/audioService';
import { NgIf } from '@angular/common';
import { Spinner } from "../spinner/spinner";
import { LoadingInformation } from "../loading-information/loading-information";

@Component({
  selector: 'app-summarization-button',
  imports: [NgIf, Spinner, LoadingInformation],
  templateUrl: './summarization-button.html',
  styleUrl: './summarization-button.css'
})
export class SummarizationButton {

  @Input() audioSpecs: AudioSpecs | undefined;

  public readonly modalId = `summary-modal-${crypto.randomUUID()}`;
  public isLoading = false;
  public summarizationText: string = '';
  private source?: EventSource;

  constructor(
    private summarizationService: SummarizationService,
    private audioService: AudioService,
    private zone: NgZone
  ) {}

  ngOnInit() {
    console.log('__SUMMARIZATION BUTTON__');
    console.log(this.audioSpecs);
  }

  clickSummarizeData() {
    console.log('Summarization button clicked');
    this.isLoading = true;

    if (!this.audioSpecs) {
      this.summarizationText =
        'Audio specs are undefined. Cannot summarize.';
      this.isLoading = false;
      return;
    }

    /* ============================================================
       ✅ CASE 1: Summary already exists → just load it
       ============================================================ */
      
    if (this.audioSpecs.summaryFileName) {
      // const index = this.audioSpecs.transcriptionFileName.lastIndexOf('/');
      this.populateSummaryText(
        // this.audioSpecs.summaryFileName.substring(index -1)
        this.audioSpecs.summaryFileName
      );
      // populateSummaryText will end loading
      return;
    }

    if (!this.audioSpecs.transcriptionFileName) {

      
      // We REFRESH audioSpecs asynchronously
      // AND move all dependent logic INSIDE subscribe
      this.updateAudioSpec(this.audioSpecs.name, () => {

        
        // This check now runs AFTER audioSpecs is updated
        if (!this.audioSpecs?.transcriptionFileName) {
          this.summarizationText =
            'Transcription file does not exist. Please transcribe first.';
            this.isLoading = false;
          return;
        }

        // Transcription exists → summarize
        
        this.summarize(
          this.audioSpecs.id,
          this.audioSpecs.transcriptionFileName
        );
      });

      return;
    }

    /* ============================================================
       ✅ CASE 3: Transcription already known → summarize immediately
       ============================================================ */
       
    this.summarize(
      this.audioSpecs.id,
      this.audioSpecs.transcriptionFileName
    );
  }

  summarize(transcriptionId: string, transcriptFile: string) {

    this.summarizationService
      .create(transcriptionId, transcriptFile)
      .subscribe({
        next: (response) => {
          console.log(
            'Summarization job started with ID:',
            response.jobId
          );

          this.connect(
            response.jobId,
            (msg) => console.log('SSE message:', msg),
            (err) => console.error('SSE error:', err),
            () => {
              console.log('SSE completed');

              // ❗ summaryLocation is written server-side,
              // so we refresh before reading
              this.updateAudioSpec(this.audioSpecs!.name, () => {
                if (this.audioSpecs?.summaryFileName) {
                  this.populateSummaryText(
                    this.audioSpecs.summaryFileName
                  );
                }else{
                  this.isLoading = false;
                  this.summarizationText = 'Error: Summary file not found after summarization.';
                }
              });
            }
          );
        },
        error: (error) => {
          this.isLoading = false;
          this.summarizationText =
            'Error with summarization job: ' + error.message;
        }
      });
  }

  populateSummaryText(fileName: string) {
    this.summarizationService.getFromName(fileName).subscribe({
      next: (summary) => {
        this.isLoading = false;
        this.summarizationText = summary;
      },
      error: () => {
        this.isLoading = false;
        this.summarizationText = 'Error fetching summary.';
      }
    });
  }

  connect(
    jobId: string,
    onMessage: (msg: string) => void,
    onError?: (err: any) => void,
    onComplete?: () => void
  ): EventSource {

    this.source = new EventSource(
      `https://localhost:8443/api/summary/jobs/${jobId}/stream`,
      { withCredentials: true }
    );

    this.source.onmessage = (event) => {
      this.zone.run(() => {
        onMessage(event.data);

        if (event.data === 'Completed' || event.data === 'Failed') {
          this.source?.close();
          onComplete?.();
        }
      });
    };

    this.source.onerror = (err) => {
      this.zone.run(() => {
        onError?.(err);
        this.source?.close();
      });
    };

    return this.source;
  }

  /* ============================================================
     ❗ CHANGE #3:
     updateAudioSpec now accepts a callback
     so logic can run AFTER HTTP completes
     ============================================================ */
  updateAudioSpec(fileName: string, afterUpdate?: () => void) {
    this.audioService.getSingleAudioFileSpecs(fileName).subscribe({
      next: (audioSpecs) => {
        console.log('Audio specs received:', audioSpecs);
        this.audioSpecs = audioSpecs;
        afterUpdate?.(); // ✅ critical
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Error fetching audio specs:', error);
      }
    });
  }
}
