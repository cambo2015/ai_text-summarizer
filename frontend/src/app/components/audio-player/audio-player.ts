import {
  Component,
  Input,
  ViewChild,
  ElementRef,
  AfterViewInit,
  OnDestroy
} from '@angular/core';
import { NgIf } from '@angular/common';
import WaveSurfer from 'wavesurfer.js';
import { AudioService, AudioSpecs } from '../../services/audioService';
import { TranscriptionButton } from "../transcription-button/transcription-button";
import { SummarizationButton } from "../summarization-button/summarization-button";
import { DeleteButton } from "../delete-button/delete-button";

@Component({
  selector: 'app-audio-player',
  standalone: true,
  imports: [NgIf, TranscriptionButton, SummarizationButton, DeleteButton],
  templateUrl: './audio-player.html',
  styleUrl: './audio-player.css'
})
export class AudioPlayer implements AfterViewInit, OnDestroy {

  @Input() audio!: AudioSpecs;

  @ViewChild('waveform')
  waveformRef!: ElementRef<HTMLDivElement>;

  private wavesurfer?: WaveSurfer;

  public audioUrl?: string;
  public loading = false;
  public isPlaying = false;

  constructor(private audioService: AudioService) {}

  ngAfterViewInit(): void {
    // view is ready, but we do NOT init yet
    this.loading = true;

    this.audioService.getAudioFile(this.audio.name).subscribe({
      next: blob => {
        this.audioUrl = URL.createObjectURL(blob);
        this.initWaveSurfer();
        this.loading = false;
      },
      error: err => {
        console.error('Failed to load audio', err);
        this.loading = false;
      }
    });
  }

  togglePlay(): void {

    // If already created → toggle play/pause
    if (this.wavesurfer) {
      this.wavesurfer.playPause();
      return;
    }
    
  }

  private initWaveSurfer(): void {
    if (!this.waveformRef || !this.audioUrl) return;

    this.wavesurfer = WaveSurfer.create({
      container: this.waveformRef.nativeElement,
      waveColor: '#5b2aa8',
      progressColor: '#5b2aa8',
      height: 80,
      barWidth: 2,
      url: this.audioUrl
    });

    this.wavesurfer.on('play', () => this.isPlaying = true);
    this.wavesurfer.on('pause', () => this.isPlaying = false);
    this.wavesurfer.on('finish', () => this.isPlaying = false);
  }

  ngOnDestroy(): void {
    this.wavesurfer?.destroy();
    if (this.audioUrl) URL.revokeObjectURL(this.audioUrl);
  }
}
