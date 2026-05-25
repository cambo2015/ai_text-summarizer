import { Injectable } from '@angular/core';
import {driver,Driver} from 'driver.js'

@Injectable({
  providedIn: 'root',
})
export class TutorialServiceTs {
  private driverObj!: Driver;
 
  private isTourActive: boolean = false;
  constructor() { 
  }

  startNavBarTour(): void {

    if(localStorage.getItem('hasSeenNavTour') === 'true') {
      return;
    }
    this.driverObj = driver({
      showProgress: true,
      animate: true,
      overlayOpacity: 0.6,
      stagePadding: 6,
      nextBtnText: 'Next',
      prevBtnText: 'Back',
      doneBtnText: 'Finish',
      onDestroyed: () => {
        localStorage.setItem('hasSeenNavTour', 'true');
      },
      steps: [
        {
          element: '#home',
          popover: {
            title: 'Home',
            description: 'Click here to return to the home page.',
            side: 'right'
          }
        },
        {
          element: '#recordButton',
          popover: {
            title: 'Record Audio',
            description: 'Click here to record your audio.',
            side: 'right'
          }
        },
        {
          element: '#customTemplateButton',
          popover: {
            title: 'Custom Template',
            description: 'Click here to create custom instructions for the AI to summarize your audio. Not required but can be used to get better summaries.',
            side: 'right'
          }
        },
        {
          element: '#uploadButton',
          popover: {
            title: 'Upload Audio',
            description: 'Click here to upload an audio file.',
            side: 'right'
          }
        },
        {
          element: '#settings-button',
          popover: {
            title: 'Settings',
            description: 'Click here to access settings and subscribe to our service.',
            side: 'right'
          }
        },
        
      ]
    });

    this.driverObj.drive();
  }

  startAudioTourTour(): void {

    if(localStorage.getItem('hasSeenAudioTour') === 'true') {
      return;
    }
    this.driverObj = driver({
      showProgress: true,
      animate: true,
      overlayOpacity: 0.6,
      stagePadding: 6,
      nextBtnText: 'Next',
      prevBtnText: 'Back',
      doneBtnText: 'Finish',
      onDestroyed: () => {
        localStorage.setItem('hasSeenAudioTour', 'true');
      },
      steps: [
        {
          element: '.audio-button-primary',
          popover: {
            title: 'Play Button',
            description: 'Click here to play the audio.',
            side: 'right'
          }
        },
        {
          element: '.audio-button-secondary',
          popover: {
            title: 'Audio Transcription Button',
            description: 'Click here to view the audio transcription.',
            side: 'bottom'
          }
        },
        {
          element: '.audio-button-success',
          popover: {
            title: 'Audio Summary Button',
            description: 'Click here to view the audio summary.',
            side: 'bottom'
          }
        },
        {
          element: '.trash-button',
          popover: {
            title: 'Delete Audio Button',
            description: 'Click here to delete the audio file and related files.',
            side: 'bottom'
          }
        },
      ]
    });

    this.driverObj.drive();
  }

  destroy(): void {
    this.driverObj?.destroy();
  }
}
