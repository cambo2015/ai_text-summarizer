import { Component } from '@angular/core';
import { ModelType, SummarizationService } from '../../services/summarization-service';
// import { ModelSelection } from "../model-selection/model-selection";
import { NgFor, NgIf } from "@angular/common";
import { Spinner } from "../spinner/spinner";
import { StpCheckoutbutton } from "../stp-checkoutbutton/stp-checkoutbutton";
import { StripeService } from '../../services/stripe-service';
import { StpEditSubscriptionButton } from "../stp-edit-subscription-button/stp-edit-subscription-button";
import { Router } from '@angular/router';

@Component({
  selector: 'app-settings',
  imports: [NgFor, NgIf, Spinner, StpCheckoutbutton, StpEditSubscriptionButton,StpEditSubscriptionButton],
  templateUrl: './settings.html',
  styleUrl: './settings.css'
})
export class Settings {

  public modelTypes: ModelType[] = [ModelType.MISTRAL, ModelType.CHATGPT];
  public modelSelected: string | null = null;
  public modelType: ModelType | null = null;
  public loading: boolean = false;
  public subscriptionStatus: boolean = false;
  constructor(private summarizationService: SummarizationService,private stripeService: StripeService,private router: Router) { }
  
  ngOnInit() {
    // this.summarizationService.selectedModel$.subscribe(model => {
    //   console.log("Selected model in settings changed to:", model);
    //   this.modelSelected = model;
    //   this.loading = false;
    // });

    // this.summarizationService.modelType$.subscribe(type => {
    //   console.log("Selected model type in settings changed to:", type);
    //   this.modelType = type;
    //   this.loading = false;
    // });

    // this.stripeService.getSubscriptionStatus().subscribe({
    //   next: (response) => {
    //     this.subscriptionStatus = response.subscribed;
    //     console.log("subscription status:", this.subscriptionStatus);
    //   },
    //   error: (error) => {
    //     console.error('Error fetching subscription status:', error);
    //   }
    // });
  }

  // onModelTypeChange(type: ModelType) {
  //   this.loading = true;
  //   console.log("radio selected", type)
  //   this.summarizationService.setmodelType(type);
  // }

  onBackClick(){
    this.router.navigate(['/dashboard']);
  }
}
