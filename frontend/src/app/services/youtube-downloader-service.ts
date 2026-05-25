import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';


@Injectable({
  providedIn: 'root'
})
export class YoutubeDownloaderService {


  private baseUrl = `${environment.apiUrl}/youtube`;
  constructor(private http: HttpClient  ) { }



  downloadAudio(youtubeUrl: string):Observable<string> {
    const apiUrl = `${this.baseUrl}/download-audio`; // Replace with your backend API URL
    return this.http.post(apiUrl, { youtubeUrl }, { withCredentials: true,responseType: 'text'  });
  }

  uploadAudioFile(file: File): Observable<string> {
    const apiUrl = `${this.baseUrl}/upload`; // Replace with your backend API URL
    const formData = new FormData();
    formData.append("file", file);
    return this.http.post(apiUrl, formData, { withCredentials: true, responseType: 'text' });
  }
}