import { Component } from '@angular/core';
import { RouterLink } from '@angular/router'; 
import { CommonModule, NgClass } from "@angular/common";
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-home',
  imports: [RouterLink, NgClass],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home {
  

  public isOpenMenu:boolean = false;
  constructor() {}
  toggleOpenMenu() {
    if(this.isOpenMenu){
      this.isOpenMenu = false;
    } else{
      this.isOpenMenu = true;
    }
  }
}
