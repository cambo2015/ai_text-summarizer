import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { LoginRequest } from '../common/login-request';
import { LoginResponse } from '../common/login-response';
import { catchError, map, Observable, of, ReplaySubject } from 'rxjs';
import { LoginStatusResponse } from '../common/login-status-response';
import { environment } from '../../environments/environment';


interface Authority{
  authority:string;
};
interface AuthStatus{
  username:string; 
  roles:Authority[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  

  // private readonly domain = "https://localhost:8443";
  // private readonly baseUrl = `${this.domain}/api/auth`;
  private readonly baseUrl = `${environment.apiUrl}/auth`;
  private loggedIn = false;
  private roles$ = new ReplaySubject<string[] | null>(1);
  private loaded = false;
  
  constructor(private httpClient: HttpClient){}

  signup(username: string, password: string): Observable<string> {
    const url = `${this.baseUrl}/signup`;

    return this.httpClient.post(
      url,
      { username, password },
      {
        responseType: 'text',
        withCredentials: true
      }
    );
  }

  login(username: string, password: string) {
    const url = `${this.baseUrl}/signin`;
    const request = new LoginRequest(username, password);
    return this.httpClient.post<LoginResponse>(url, request, { withCredentials: true }); // 👈 add this
  }

  logout(): Observable<void> {
    // const url = `${this.domain}/logout`;
    const url = `${environment.baseUrl}/logout`;
    return this.httpClient.post<void>(url, {}, { withCredentials: true })
      .pipe(
        map(() => {
          this.resetAuthState();
        })
      );
  }

  ensureRoles() {
    const url = `${this.baseUrl}/status`;

    if (this.loaded) return this.roles$;

    this.loaded = true;
    this.roles$.next(null); // 👈 loading state

    this.httpClient.post<AuthStatus>(url, {}, { withCredentials: true })
      .pipe(
        map(s => (s.roles ?? []).map(r => r.authority)),
        catchError(() => of([])) // not logged in
      )
      .subscribe(roles => this.roles$.next(roles));

    return this.roles$;
  }

  get rolesOnce$() {
    return this.ensureRoles();
  }

  resetAuthState() {
    this.loaded = false;
    this.roles$.next(null);
  }
}
