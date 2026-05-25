import { HttpClient } from '@angular/common/http';
import { Injectable, Input } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment.development';

@Injectable({
  providedIn: 'root'
})
export class TranscriptionService {
  
  
  // private readonly urlBase="https://localhost:8443/api/text-extractor";
  private readonly urlBase=`${environment.apiUrl}/text-extractor`;
  constructor(private httpClient: HttpClient) { }

  getTranscript(jobId: string):Observable<string> {
    return this.httpClient.post(
      this.urlBase+"/get",
      { jobId },
      {
        withCredentials: true,
        responseType: 'text'
      }
    );
  }

  create(fileName:string):Observable<TranscriptionResponse> {
    return this.httpClient.post<TranscriptionResponse>(this.urlBase+"/create", { fileName }, { withCredentials: true });
  }

  getCommonWords(transcriptionFileName:string,top:number):Observable<string[]> {
    return this.httpClient.post<string[]>(this.urlBase+"/common-words",
      {transcriptionFileName,top}, 
      { withCredentials: true });
  }
}


interface TranscriptionResponse {
  jobId: string;
}



