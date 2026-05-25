import { Component } from '@angular/core';

import { UserDashNav } from "../user-dash-nav/user-dash-nav";
import { AudioFiles } from "../audio-files/audio-files";
import { MobileDashMenu } from "../mobile-dash-menu/mobile-dash-menu";
import { ModelSelection } from "../model-selection/model-selection";
import { CustomPagination } from "../custom-pagination/custom-pagination";
import { TutorialServiceTs } from '../../services/tutorial-service.ts';
import { Recorder } from "../recorder/recorder";
import { CustomSummaryTemplate } from "../custom-summary-template/custom-summary-template";
import { UploadAudio } from "../upload-audio/upload-audio";
import { LogoWithName } from "../logo-with-name/logo-with-name";
import { RouterLink } from "@angular/router";

@Component({
  selector: 'app-user-dashboard',
  standalone: true,
  imports: [UserDashNav, AudioFiles, MobileDashMenu, CustomPagination, Recorder, CustomSummaryTemplate, UploadAudio, LogoWithName, RouterLink],
  templateUrl: './user-dashboard.html',
  styleUrl: './user-dashboard.css'
})
export class UserDashboard  {


  constructor(private tutorialService: TutorialServiceTs) { }

  ngOnInit(): void {
    this.tutorialService.startNavBarTour();
  } 
    
}
