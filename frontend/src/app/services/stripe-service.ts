import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { loadStripe,type Stripe as StripeJs } from '@stripe/stripe-js';
import { environment } from '../../environments/environment';


@Injectable({
  providedIn: 'root'

})
export class StripeService {
  
  private stripePromise:Promise<StripeJs | null> = loadStripe("pk_test_51S9WhYCHC6umIYrDZ0Cwddxvb23jiDbSw87wvXDRkd1A3oHOu6TboijCeys4e3r8n6a1PewEqtRerLJb2fUSDsYJ00blFxsDh9")

  // private baseUrl = "https://localhost:8443/api/financials"
  private baseUrl = environment.apiUrl + "/stripe"

  constructor(private http:HttpClient){}


  getSubscriptionCheckoutSession(plan:string):Observable<CheckoutSessionResponse>{
    const url = `${this.baseUrl}/checkout`
    return this.http.post<CheckoutSessionResponse>(url,{plan},{withCredentials:true})
  }

  getSubscriptionStatus(): Observable<SubscriptionStatusResponse> {
    return this.http.get<SubscriptionStatusResponse>(
      `${this.baseUrl}/subscription/status`,
        { withCredentials: true }
    );
  }

  openBillingPortal() {
    return this.http.post<BillingPortalResponse>(
      `${this.baseUrl}/subscription/portal`,
      {},
      { withCredentials: true }
    )
  }
}

export interface BillingPortalResponse{
  url:string;
}

export interface SubscriptionStatusResponse{
  subscribed:boolean;
  subscriptionId:string | null;
}

export interface lineItem{
  priceId:string;
  quantity:number;
}

export interface CheckoutSessionResponse{
  checkoutUrl:string;
}

export interface InvoiceResponse{
  invoices:Invoice[];
};

export interface Invoice{
  id:string;
  total:number;
  amountRemaining:number;
  currency:string;
  status:string;
  created:number;
  invoiceUrl:string;
};



/*

{
    "invoices": [
        {
            "id": "in_1S9WjzCHC6umIYrDOk8OBAYX",
            "total": 100,
            "amountRemaining": 100,
            "currency": "usd",
            "status": "open",
            "created": 1758397599,
            "invoiceUrl": "https://invoice.stripe.com/i/acct_1S9WhYCHC6umIYrD/test_YWNjdF8xUzlXaFlDSEM2dW1JWXJELF9UNWlBTW5EYkxmVWNienZGbHp3QjNBanpOZFRZenA1LDE1MTI3OTgyMA0200GM5a1a40?s=ap"
        }
    ],
    "message": "success"
}

*/ 