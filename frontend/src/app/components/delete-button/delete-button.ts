import { Component, Input } from '@angular/core';
import { AudioService, AudioSpecs } from '../../services/audioService';
import { CleanupService } from '../../services/cleanup-service';

@Component({
  selector: 'app-delete-button',
  imports: [],
  templateUrl: './delete-button.html',
  styleUrl: './delete-button.css'
})
export class DeleteButton {
  @Input() audioSpecs: AudioSpecs | undefined;

  constructor(private cleanupService: CleanupService, private audioService: AudioService) { }

  handleDelete() {
    if (this.audioSpecs) {
      if (confirm(`Are you sure you want to delete ${this.audioSpecs.originalFileName}? This action cannot be undone.`)) {

        this.cleanupService.deleteAssociatedFiles(this.audioSpecs.name).subscribe({
          next: response => {
            console.log('Cleanup response:', response);
            this.audioService.repopulateFileList();
          },
          error: err => {
            console.error('Error during cleanup:', err);
          }
        });
      }
    }
  }
}
