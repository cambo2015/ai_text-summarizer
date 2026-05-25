import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';


@Injectable({
  providedIn: 'root'
})
export class CleanupService {
  
  // cleanup service 
  //when to cleanup the service 
  // private baseUrl = "https://localhost:8443/api/cleanup";
  private baseUrl = environment.apiUrl + "/cleanup";

  constructor(private http:HttpClient) { }

  deleteAssociatedFiles(fileName:string):Observable<CleanupResponse>{
    return this.http.post<CleanupResponse>(this.baseUrl+"/delete/associated-files", {
      fileName
    }, { withCredentials: true });
  }
}

interface CleanupResponse{
  success:string;
  error:string;
}
