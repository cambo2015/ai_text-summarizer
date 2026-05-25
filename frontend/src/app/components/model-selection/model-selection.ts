import { Component, OnInit } from '@angular/core';
import { SummarizationService } from '../../services/summarization-service';
import { NgFor } from '@angular/common';
import { LocalStorageService } from '../../services/local-storage-service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-model-selection',
  imports: [NgFor,FormsModule],
  templateUrl: './model-selection.html',
  styleUrl: './model-selection.css'
})
export class ModelSelection implements OnInit {


  public selectedModel: string | null = null;
  public models: string[] = [];

  
  constructor(private summarizationService: SummarizationService,private localStorageService:LocalStorageService) { }

  ngOnInit() {
    // modelList subscription and schedual all changes
    this.summarizationService.models$.subscribe(models => {
      console.log("Available models updated:", models);
      this.models = models;
    });

    
    this.summarizationService.selectedModel$.subscribe(model => {
      console.log("Selected model changed to:", model);
      this.selectedModel = model;
    });
  }

  onModelChange(newModel:string) {
    this.summarizationService.setselectedModel(newModel);
    
  }
}
