import { Component, EventEmitter, Output } from '@angular/core';
import { SummarizationService } from '../../services/summarization-service';
import { FormsModule } from '@angular/forms';
import { NgIf } from "@angular/common";
import { Spinner } from "../spinner/spinner";
import { HttpErrorResponse } from '@angular/common/http';
import { LoadingInformation } from "../loading-information/loading-information";

@Component({
  selector: 'app-custom-summary-template',
  imports: [FormsModule, NgIf, Spinner, LoadingInformation],
  templateUrl: './custom-summary-template.html',
  styleUrl: './custom-summary-template.css'
})
export class CustomSummaryTemplate {

  public textBoxText: string = '';
  public text: string = '';
  public isEditing: boolean = false;
  public loading: boolean = false;
  public errorMessage: string = '';

  @Output() modalClosed = new EventEmitter<void>();

  constructor(private summarizationService: SummarizationService) { }

  ngOnInit() {
    this.getTemplate();
  }

  onHidden() {
    this.modalClosed.emit();
  }

  getTemplate(){
    this.loading = true;
    this.summarizationService.getTemplate().subscribe({
      next: (response) => {
        // console.log('Template fetched:', response);
        this.errorMessage = '';
        this.loading = false;
        this.text = response.text;
      },
      error: (error:HttpErrorResponse) => {
        // console.error('Error fetching template:', error);
        this.loading = false;
        this.errorMessage = error.error.message ?? "Error fetching template";
      }
    });
  }

  saveTemplate() {
    this.loading = true;
    this.isEditing = false;
    this.summarizationService.saveTemplate(this.textBoxText).subscribe({
      next: (response) => {
        // console.log('Template saved:', response);
        this.loading = false;
        // console.log(response.text)
        this.text = response.text;
        this.getTemplate();
      },
      error: (error) => {
        // console.error('Error saving template:', error);
        this.loading = false;
        this.text = 'Error saving template. Please try again.';
      }
    });
  }

  toggleEdit() {
    this.isEditing = !this.isEditing;
  }
}
