import { Component } from '@angular/core';
import { StripeService } from '../../services/stripe-service';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-stp-edit-subscription-button',
  imports: [NgIf],
  templateUrl: './stp-edit-subscription-button.html',
  styleUrl: './stp-edit-subscription-button.css'
})
export class StpEditSubscriptionButton {
 loading: boolean = false;
  errormessage: string | null = null;

  constructor(private stripeService: StripeService){}

  checkout(){
    this.loading = true;
    this.stripeService.openBillingPortal().subscribe({
      next: async (response: { url: any; }) => {
        this.loading = false;
        const portalUrl = response.url;
        window.location.href = portalUrl;
      }
      ,
      error: (error) => {
        this.loading = false;
        console.error('Error opening billing portal:', error);
        this.errormessage = 'An error occurred while opening the billing portal. Please try again.';
      }
    });
  }
}
