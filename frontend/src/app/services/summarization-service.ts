import { HttpClient } from '@angular/common/http';
import { Injectable, model } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { LocalStorageService } from './local-storage-service';
@Injectable({
  providedIn: 'root'
})
export class SummarizationService {
  
  //available models for the selected type
  private readonly modelsListSubject:BehaviorSubject<string[]> = new BehaviorSubject<string[]>([]);
  public models$ = this.modelsListSubject.asObservable();

  //current selected model
  private selectedModelSub:BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);
  readonly selectedModel$ = this.selectedModelSub.asObservable();

  //like chat gpt or mistral
  private modelTypeSub:BehaviorSubject<ModelType> = new BehaviorSubject<ModelType>(ModelType.MISTRAL);
  readonly modelType$ = this.modelTypeSub.asObservable();
  
  // private urlBase = 'https://localhost:8443/api/summary';
  private urlBase = `${environment.apiUrl}/summary`;

  constructor(
  private httpClient: HttpClient,
  private localStorageService: LocalStorageService
  ) {
    const storedType =
      this.localStorageService.getItem('modelType') as ModelType | null;

    if (storedType) {
      this.setmodelType(storedType);
    } else {
      this.setmodelType(ModelType.MISTRAL);
    }
  }

  get modelsList():string[]{
    return this.modelsListSubject.value;
  }

  setModelsList(models:string[]):void {
    this.modelsListSubject.next(models);
  }

  get selectedModel():string | null{
    return this.selectedModelSub.value;
  }

  setselectedModel(model:string):void {
    this.selectedModelSub.next(model);
    this.localStorageService.setItem('selectedModel',model);
  }

  get modelType():ModelType{
    return this.modelTypeSub.value;
  }

  setmodelType(modelType:ModelType):void {
    this.modelTypeSub.next(modelType);
    this.localStorageService.setItem('modelType',modelType);
    this.loadSupportedModels(modelType);
    
  }

  create(transcriptionId: string,transcriptFile: string):Observable<SummaryResponse>{
    console.log("transcriptionId",transcriptionId); 
    console.log("transcriptFile",transcriptFile);
    return this.httpClient.post<SummaryResponse>(
      this.urlBase+"/create", 
      { transcriptionId,transcriptFile,model:this.selectedModel },
      {withCredentials: true }
    );
  }

  getFromName(fileName: string): Observable<string> {
    return this.httpClient.post(
      this.urlBase + "/get-based-on-name",
      { fileName },
      {
        withCredentials: true,
        responseType: 'text'   
      }
    );
  }

  getTemplate(): Observable<GetTemplateResponse> {
    return this.httpClient.post<GetTemplateResponse>(
      this.urlBase + "/template/get",{},
      { withCredentials: true }
    );
  } 

  saveTemplate(text:string): Observable<SaveTemplateResponse> {
    return this.httpClient.post<SaveTemplateResponse>(this.urlBase+"/template/save",{ instructions:text }, { withCredentials: true });
  }

  private getSupportedModels(modelType:ModelType): Observable<SupportedModelsResponse> {
    return this.httpClient.post<SupportedModelsResponse>(
      this.urlBase + "/supported-models",
      {
        modelType
      },
      { withCredentials: true }
    );
  }
  // private loadSupportedModelsWithNewSelectedModel(modelType:ModelType):void {
  //     //if model type has changed, update it and fetch new models
  //   this.getSupportedModels(modelType).subscribe(res => {
  //     this.modelsListSubject.next(res.models);
  //     if (res.models.length > 0) {
  //       this.selectedModelSub.next(res.models[0]);
  //       this.setselectedModel(this.selectedModelSub.value!);
  //     }
      
  //   });
  // }

  private loadSupportedModels(modelType: ModelType): void {
    this.getSupportedModels(modelType).subscribe(res => {
      this.modelsListSubject.next(res.models);

      const storedModel =
        this.localStorageService.getItem('selectedModel');

      if (storedModel && res.models.includes(storedModel)) {
        this.selectedModelSub.next(storedModel);
      } else if (res.models.length > 0) {
        this.selectedModelSub.next(res.models[0]);
        this.localStorageService.setItem('selectedModel', res.models[0]);
      }
    });
  }
}

export interface SummaryResponse {
  jobId: string;
}

export interface GetTemplateResponse{
  text:string;
  error:string;
}

export interface SaveTemplateResponse{
  text:string;
} 

export interface SupportedModelsResponse{
  models:string[];
}

export enum ModelType{
  CHATGPT = "chatgpt",
  MISTRAL = "mistral"
}


//get the summary text based on the audio file name

  // get(jobId: string):Observable<string>{
  //   console.log("Fetching summary for jobId:", jobId);
  //   return this.httpClient.post<string>(
  //     this.urlBase+"/get",
  //     {jobId},
  //     { withCredentials: true }
  //   );
  // }

