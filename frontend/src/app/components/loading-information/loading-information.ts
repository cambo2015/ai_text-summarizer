import { Component, Input, input } from '@angular/core';
import { Spinner } from "../spinner/spinner";

@Component({
  selector: 'app-loading-information',
  imports: [Spinner],
  templateUrl: './loading-information.html',
  styleUrl: './loading-information.css'
})
export class LoadingInformation {

  @Input() text: string = 'Loading. Please wait...';
  @Input() color: 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'info' | 'light' | 'dark' = 'primary';
}
