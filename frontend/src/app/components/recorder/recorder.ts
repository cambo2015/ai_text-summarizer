import { Component, EventEmitter, NgZone, OnDestroy, Output } from '@angular/core';
import { NgClass, NgIf } from '@angular/common';
import { AudioService, AudioUploadResponse, AudioUploadResult, GenericErrorResponse } from '../../services/audioService';
import { FormsModule } from '@angular/forms';
import { Spinner } from "../spinner/spinner";
import { HttpErrorResponse } from '@angular/common/http';
import { LoadingInformation } from "../loading-information/loading-information";
import * as bootstrap from 'bootstrap';

@Component({
  selector: 'app-recorder',
  imports: [NgClass, FormsModule, NgIf, LoadingInformation],
  templateUrl: './recorder.html',
  styleUrl: './recorder.css',
  standalone: true
})
export class Recorder implements OnDestroy {

  /* ==================== UI State ==================== */
  recording = false;
                 // ✅ moved to UI state
  uploadedFileName?: string;
  successMessage = '';
  public filename: string = '';
  public finishedNamingFile = false;
  public loading = false;  
  public errorMessage = '';

  /* ==================== Timer ==================== */
  recordingSeconds = 0;
  private recordingStartTime = 0;
  private timerId?: number;

  /* ==================== Audio ==================== */
  private stream?: MediaStream;
  private audioContext?: AudioContext;

  /* MediaRecorder */
  private mediaRecorder?: MediaRecorder;
  private recordedChunks: Blob[] = [];

  /* ==================== Visualizer ==================== */
  private analyser?: AnalyserNode;
  private animationId?: number;
  private canvas?: HTMLCanvasElement;
  private ctx?: CanvasRenderingContext2D;

  // THIS MODAL
  private modalElement!:HTMLElement;
  private isClosing = false;

  @Output() modalClosed = new EventEmitter<void>();

  constructor(
    private audioService: AudioService,
    private zone: NgZone
  ) {}

  ngAfterViewInit(): void {
    this.modalElement = document.getElementById('recordModal') as HTMLElement;
    this.modalElement.addEventListener('hidden.bs.modal', () => {
      this.zone.run(() => {
        if(this.isClosing) return; // prevent double cleanup if we already called stopRecording()
        this.stopRecording(); 
        this.cleanup();
        this.resetEverything();
      });
    });
  } 

  onHidden() {
    this.modalClosed.emit();
  }


  toggleNamingFile(): void {
    this.filename = this.filename.trim();
    this.finishedNamingFile = !this.finishedNamingFile;
  }

  async toggleRecording(): Promise<void> {
    if (this.recording) {
      this.stopRecording();
    } else {
      await this.startRecording();
    }
  }

  /* ============================================================
     Start recording
     ============================================================ */
  async startRecording(): Promise<void> {
    this.successMessage = '';

    try {
      /* 1️⃣ Get microphone */
      this.stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: false,
          noiseSuppression: false,
          autoGainControl: false,
        }
      });

      /* 2️⃣ AudioContext for visualizer ONLY */
      this.audioContext = new AudioContext();
      await this.audioContext.resume();

      const source = this.audioContext.createMediaStreamSource(this.stream);

      /* 3️⃣ Start visualizer */
      this.startBarsVisualizer(source);

      /* 4️⃣ MediaRecorder */
      this.recordedChunks = [];

      this.mediaRecorder = new MediaRecorder(this.stream, {
        mimeType: this.getBestMimeType(),
        audioBitsPerSecond: 32000
      });

      this.mediaRecorder.ondataavailable = e => {
        if (e.data.size > 0) {
          this.recordedChunks.push(e.data);
        }
      };

      this.mediaRecorder.onstop = () => {
        const blob = new Blob(this.recordedChunks, {
          type: this.mediaRecorder!.mimeType,
        });

        const name = this.filename
          ? `${this.filename}.webm`
          : `recording-${Date.now()}.webm`;

        const file = new File([blob], name, { type: blob.type });

        // 🔑 back into Angular
        this.zone.run(() => {
          this.uploadAudio(file);
        });
      };

      this.mediaRecorder.start();
      this.recording = true;

      /* 5️⃣ Start timer */
      this.recordingStartTime = performance.now();
      this.timerId = window.setInterval(() => {
        const elapsedMs = performance.now() - this.recordingStartTime;
        this.recordingSeconds = Math.floor(elapsedMs / 1000);
      }, 250);

    } catch (err) {
      console.error('Failed to start recording:', err);
      this.cleanup();
    }
  }

  /* ============================================================
     Stop recording
     ============================================================ */
  stopRecording(): void {
    if (!this.recording) return;

    this.mediaRecorder?.stop();
    this.recording = false;

    this.stopBarsVisualizer();
    this.cleanupStream();
    this.cleanup();
    this.stopTimer();
    this.closeModal();
  }

  // 🔧 FIXED VERSION - Properly removes backdrop 
  closeModal(): void { 
    if(localStorage.getItem("hasSeenAudioTour") === "true") {
      return;
    }
    const modalElement = document.getElementById('recordModal'); 
    if (!modalElement || this.isClosing) return;
    this.isClosing = true; // Set guard first 
    const modalInstance = bootstrap.Modal.getOrCreateInstance(modalElement); modalInstance.hide(); // 🎯 KEY FIX: Remove backdrop after modal hides 
    
    modalElement.addEventListener('hidden.bs.modal', () => { // Remove any stuck backdrops 
    document.querySelectorAll('.modal-backdrop').forEach(backdrop => { backdrop.remove(); }); // Remove modal-open class from body (allows scrolling) 
    document.body.classList.remove('modal-open'); document.body.style.removeProperty('overflow'); 
    document.body.style.removeProperty('padding-right'); // Reset guard 
    setTimeout(() => { this.isClosing = false; console.log("closing modal")}, 100); }, { once: true }); // 👈 Use 'once' to auto-cleanup listener 
  
  }

  /* ============================================================
     Upload
     ============================================================ */
  private uploadAudio(file: File): void {
    this.loading = true;                        // ✅ START spinner

    this.audioService.uploadAudio(file).subscribe({
      next: (res: AudioUploadResponse) => {
        this.uploadedFileName = res.fileName;
        this.successMessage = 'Audio uploaded successfully!';
        this.loading = false;                  // ✅ STOP spinner
        this.audioService.repopulateFileList();
        console.log('✅ Upload success:', res);
        this.resetEverythingWithTimeout();          // ✅ reset UI after 5 seconds
        
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false; 
        const errorData = err.error as GenericErrorResponse;
        this.handleResponse(errorData);
        this.resetEverythingWithTimeout();          // ✅ reset UI after 5 seconds
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

  private stopTimer() {
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = undefined;
    }
  }

  /* ============================================================
     Visualizer
     ============================================================ */
  private startBarsVisualizer(source: MediaStreamAudioSourceNode): void {
    this.canvas = document.getElementById('barsCanvas') as HTMLCanvasElement;
    this.ctx = this.canvas.getContext('2d')!;

    this.canvas.width = this.canvas.clientWidth;
    this.canvas.height = this.canvas.clientHeight;

    this.analyser = this.audioContext!.createAnalyser();
    this.analyser.fftSize = 1024;
    this.analyser.smoothingTimeConstant = 0.85;

    source.connect(this.analyser);

    const bufferLength = this.analyser.frequencyBinCount;
    const dataArray = new Uint8Array(bufferLength);

    const draw = () => {
      this.animationId = requestAnimationFrame(draw);
      this.analyser!.getByteFrequencyData(dataArray);

      const ctx = this.ctx!;
      const width = this.canvas!.width;
      const height = this.canvas!.height;

      ctx.clearRect(0, 0, width, height);

      const BAR_WIDTH = 2;
      const BAR_GAP = 2;

      let x = 0;
      for (let i = 0; i < bufferLength; i++) {
        const barHeight = (dataArray[i] / 255) * height;
        ctx.fillStyle = '#9ca3af';
        ctx.fillRect(x, height - barHeight, BAR_WIDTH, barHeight);
        x += BAR_WIDTH + BAR_GAP;
      }
    };

    draw();
  }

  private stopBarsVisualizer(): void {
    if (this.animationId) {
      cancelAnimationFrame(this.animationId);
      this.animationId = undefined;
    }
  }

  /* ============================================================
     Helpers
     ============================================================ */
  private getBestMimeType(): string {
    if (MediaRecorder.isTypeSupported('audio/mp4')) return 'audio/mp4';
    if (MediaRecorder.isTypeSupported('audio/webm;codecs=opus')) return 'audio/webm;codecs=opus';
    return 'audio/webm';
  }

  private cleanupStream(): void {
    this.stream?.getTracks().forEach(t => t.stop());
    this.stream = undefined;
    this.audioContext?.close();
    this.audioContext = undefined;
  }

  private cleanup(): void {
    this.loading = false;              // ✅ safety
    this.stopBarsVisualizer();
    this.cleanupStream();
  }

  private resetEverything(){
    this.mediaRecorder = undefined;
    this.recording = false;
    this.filename = '';
    this.finishedNamingFile = false;
    this.successMessage = '';
    this.errorMessage = '';
  }

  private resetEverythingWithTimeout(){
    setTimeout(() => {
        this.resetEverything();
    }, 5000);
  }

  ngOnDestroy(): void {
    this.cleanup();
  }

  get formattedDuration(): string {
    if (!this.recording) return '0:00';
    const minutes = Math.floor(this.recordingSeconds / 60);
    const seconds = this.recordingSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }
}
