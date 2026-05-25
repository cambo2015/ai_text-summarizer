import { Component } from '@angular/core';
import { AudioService } from '../../services/audioService';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-custom-pagination',
  standalone: true,                 // 👈 REQUIRED
  imports: [NgbPaginationModule],   // now valid
  templateUrl: './custom-pagination.html',
  styleUrls: ['./custom-pagination.css'] // 👈 plural + array
})
export class CustomPagination {

  public page: number = 0; // backend page (0-based)
  public totalElements: number = 0;
  public size: number = 10;

  constructor(private audioService: AudioService) {}

  ngOnInit(): void {
    this.audioService.pagination$.subscribe(pagination => {
      this.page = pagination.page;
      this.totalElements = pagination.totalElements;
      this.size = pagination.size;
    });
  }

  onPageChange(newPage: number) {
    const zeroBasedPage = newPage - 1;

    this.audioService.getAudioFileList(zeroBasedPage, this.size)
      .subscribe(files => {
        this.audioService.fileListSubject.next(files);
      });
  }
}
