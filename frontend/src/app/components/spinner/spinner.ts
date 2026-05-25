import { Component, Input } from '@angular/core';
import { NgClass } from "@angular/common";


type SpinnerColor = 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'info' | 'light' | 'dark';
type SpinnerSize = "sm" | "md" | "lg";
@Component({
  selector: 'app-spinner',
  imports: [NgClass],
  templateUrl: './spinner.html',
  styleUrl: './spinner.css'
})
export class Spinner {

  @Input() size: SpinnerSize = 'md';
  @Input() color: SpinnerColor = 'primary';

  get sizeClass(): string | null {
    switch (this.size) {
      case 'sm':
        return 'spinner-border-sm';
      case 'lg':
        return 'spinner-lg'; // custom
      default:
        return null; // md
    }
  }
}
