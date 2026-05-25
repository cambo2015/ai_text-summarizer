import { Component } from '@angular/core';
import { StripeService } from '../../services/stripe-service';
import { NgIf } from "@angular/common";
import { FormsModule } from "@angular/forms";

@Component({
  selector: 'app-stp-checkoutbutton',
  standalone: true,
  imports: [NgIf, FormsModule],
  templateUrl: './stp-checkoutbutton.html',
  styleUrl: './stp-checkoutbutton.css'
})
export class StpCheckoutbutton {

  loading: boolean = false;
  errormessage: string | null = null;

  // default selection
  plan: string = "starter";

  constructor(private stripeService: StripeService){}

  checkout(){
    this.loading = true;

    this.stripeService.getSubscriptionCheckoutSession(this.plan)
      .subscribe({
        next: (response) => {
          this.loading = false;
          window.location.href = response.checkoutUrl;
        },
        error: (error) => {
          this.loading = false;
          console.error('Error creating checkout session:', error);
          this.errormessage = 'An error occurred while initiating checkout. Please try again.';
        }
      });
  }
}
