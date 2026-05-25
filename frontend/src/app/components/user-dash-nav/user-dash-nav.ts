import { Component } from '@angular/core';
import { AuthService } from '../../services/auth-service';
import { Router, RouterLink } from '@angular/router';
import { Recorder } from "../recorder/recorder";
import { CustomSummaryTemplate } from "../custom-summary-template/custom-summary-template";
import { UploadAudio } from "../upload-audio/upload-audio";
import { Logo } from "../logo/logo";
import { LogoWithName } from "../logo-with-name/logo-with-name";
import { CustomPagination } from "../custom-pagination/custom-pagination";

@Component({
  selector: 'app-user-dash-nav',
  imports: [Recorder, CustomSummaryTemplate, UploadAudio, RouterLink, Logo, LogoWithName, CustomPagination],
  templateUrl: './user-dash-nav.html',
  styleUrl: './user-dash-nav.css'
})
export class UserDashNav {

  selectedButton:string | null = 'home';
  constructor(private authService:AuthService, private router: Router){}

  ngAfterViewInit() {
    const tooltipTriggerList =
    document.querySelectorAll('[title]');

  [...tooltipTriggerList].forEach(el => {
    new (window as any).bootstrap.Tooltip(el);
  });
  }

  select(id:string){
    this.selectedButton = id;
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