import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Recorder } from "../recorder/recorder";
import { YoutubeDownloader } from "../youtube-downloader/youtube-downloader";
import { CustomSummaryTemplate } from "../custom-summary-template/custom-summary-template";
import { UploadAudio } from "../upload-audio/upload-audio";
import { AuthService } from "../../services/auth-service";
import { Router } from '@angular/router';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-mobile-dash-menu',
  standalone: true,
  imports: [CommonModule, Recorder, YoutubeDownloader, CustomSummaryTemplate, UploadAudio, RouterLink], // 🔥 REQUIRED
  templateUrl: './mobile-dash-menu.html',
  styleUrls: ['./mobile-dash-menu.css']
})
export class MobileDashMenu {
  open = false;

  constructor(private authService:AuthService, private router: Router){}

  toggleMenu() {
    this.open = !this.open;
    console.log('open =', this.open);
  }

  logout() {
    // Implement logout logic here, for example:
    this.authService.logout().subscribe({
      next: () => {
        console.info("Logged out successfully");
        this.router.navigate(['/login']);
      },
      error: (err) => {
        alert('Logout failed! Server is probally down.'+ err);
      }
    });
  }
}
