import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, map, Observable, ObservedValueOf, tap,  } from 'rxjs';
import { environment } from '../../environments/environment';


@Injectable({
  providedIn: 'root',
})
export class AudioService {

  //private readonly API_URL = "https://localhost:8443/api/audio"
  private readonly API_URL = environment.apiUrl + "/audio";
  public fileListSubject: BehaviorSubject<AudioSpecs[]> = new BehaviorSubject<AudioSpecs[]>([]);
  readonly fileList$ = this.fileListSubject.asObservable();

  private paginationSubject = new BehaviorSubject<PageInfo>({ page: 0, size: 10, totalPages: 0, totalElements: 0 });
  readonly pagination$ = this.paginationSubject.asObservable();

  

  constructor(private http: HttpClient) { }

  // ✅ CHANGE #3: load initial data once
  loadFileList(): void {
    this.getAudioFileList(this.paginationSubject.value.page, this.paginationSubject.value.size).subscribe(files => {
      this.fileListSubject.next(files);
    });
  }

  audioFileListSubscription(): Observable<ObservedValueOf<Observable<AudioSpecs[]>>> {
    return this.getAudioFileList(this.paginationSubject.value.page,this.paginationSubject.value.size);
  }

  uploadAudio(file: File):Observable<AudioUploadResponse> {
     
      const formData = new FormData();
      formData.append('file', file);

      return this.http.post<AudioUploadResponse>(this.API_URL+"/upload", formData, { withCredentials: true });
  }

  getAudioFileList(page:number,size:number):Observable<AudioSpecs[]>{
    return this.http.post<Page<AudioSpecs>>(this.API_URL+"/list",{page,size},{withCredentials:true}).pipe(
      tap(page => {
        this.paginationSubject.next({
          page: page.number,
          size: page.size,
          totalPages: page.totalPages,
          totalElements: page.totalElements
        });
      }),
      map(page => page.content)
    );
  }

  getSingleAudioFileSpecs(fileName:string):Observable<AudioSpecs>{
    return this.http.post<AudioSpecs>(this.API_URL+"/get-one",{fileName},{withCredentials:true});
  }

  getAudioFile(fileName: string): Observable<Blob> {
    return this.http.post(
      this.API_URL + '/getfile',
      { fileName }, // 👈 JSON body
      {
        withCredentials: true,
        responseType: 'blob' // 👈 THIS IS CRITICAL
      }
    );
  }
  
  blobToFile(blob: Blob, fileName: string,options:any): File {
    return new File([blob],fileName,options);
  }

  repopulateFileList():void{
    const {page,size} = this.paginationSubject.value;
    this.getAudioFileList(page,size).subscribe(files => {
      this.fileListSubject.next(files);
    });
  }
}


export interface GenericErrorResponse{
    timestamp?:Date,
    status?: Number,
    error?: string,
    message?: string,
    path?: string
}

export interface AudioUploadResponse {
  fileName:string;
  message?: string;
}

export type AudioUploadResult = AudioUploadResponse | GenericErrorResponse;

export interface AudioSpecs {
  id: string;
  name:string;
  size: number;
  summaryFileName: string;
  transcriptionFileName: string;
  originalFileName?:string;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export interface PageInfo{
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}


