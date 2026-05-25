import { Component, OnDestroy, OnInit } from '@angular/core';
import { AudioService, AudioSpecs } from '../../services/audioService';
import { AudioPlayer } from "../audio-player/audio-player";
import { NgFor, NgIf } from '@angular/common';
import { Subscription } from 'rxjs';
import { StripeService } from '../../services/stripe-service';
import { StpCheckoutbutton } from "../stp-checkoutbutton/stp-checkoutbutton";
import { TutorialServiceTs } from '../../services/tutorial-service.ts';
import { CustomPagination } from "../custom-pagination/custom-pagination";

@Component({
  selector: 'app-audio-files',
  imports: [AudioPlayer, NgIf, NgFor, StpCheckoutbutton, CustomPagination],
  templateUrl: './audio-files.html',
  styleUrl: './audio-files.css'
})
export class AudioFiles implements OnInit, OnDestroy {

  audioFiles: AudioSpecs[] = [];
  private audioServiceSubscription?: Subscription;
  public  subscribedToStripe: boolean = false;
  constructor(private audioService: AudioService,private stripeService:StripeService,private tutorialService:TutorialServiceTs) {}

 
  ngOnInit(): void {
    // ✅ CHANGE #5: subscribe once
    this.audioServiceSubscription = this.audioService.fileList$.subscribe(files => {
      this.audioFiles = files;
      if ((!localStorage.getItem('hasSeenAudioTour') && localStorage.getItem('hasSeenNavTour')==='true')) {
        this.tutorialService.startAudioTourTour();
      }
    });

    this.stripeService.getSubscriptionStatus().subscribe(status=>{
      this.subscribedToStripe = status.subscribed;
    });

    // ✅ load initial list
    this.audioService.loadFileList();
  }

  ngOnDestroy(): void {
    this.audioServiceSubscription?.unsubscribe();
  }
}
