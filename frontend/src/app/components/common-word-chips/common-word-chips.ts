import { Component, Input } from '@angular/core';
import { TranscriptionService } from '../../services/transcription-service';
import { NgClass, NgFor, NgIf } from '@angular/common';

@Component({
  selector: 'app-common-word-chips',
  imports: [NgFor,NgClass,NgIf],
  templateUrl: './common-word-chips.html',
  styleUrl: './common-word-chips.css'
})
export class CommonWordChips {


  @Input() fileName: string = "";
  public commonWords: string[] = [];
  readonly chipColors = [
  'bg-primary',
  'bg-secondary',
  'bg-danger',
  "bg-success",
  // "bg-info",
];

  constructor(private transcriptionService: TranscriptionService) { }

  ngOnInit() {
    this.getCommonWords();
  }

  getColor(index:number): string {
    
    return this.chipColors[index % this.chipColors.length];
  }

  getCommonWords() {
    
    console.log("Fetching common words for file: "+this.fileName);
    if(this.fileName===''){
      console.log("No file name provided for common words");
      return;
    } 
    // Call the transcription service to get common words
    this.transcriptionService.getCommonWords(this.fileName,15).subscribe({
      next: (words) => {
        console.log(words)
        this.commonWords = words;
      },
      error: (error) => {
        console.error('Error fetching common words:', error);
      }
    });
  }
}
