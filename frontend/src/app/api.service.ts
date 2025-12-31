import {Injectable} from '@angular/core';
import {environment} from '../environments/environment';

@Injectable({providedIn: 'root'})
export class ApiService {
  private baseUrl: string = '';

  constructor() {
    this.baseUrl = this.resolveBaseUrl();
  }

  private resolveBaseUrl(): string {
    const port = localStorage.getItem('backend_port');
    if (port) {
      return `http://localhost:${port}`;
    }
    
    const configured = environment.apiUrl?.trim();
    if (configured) {
      return configured.replace(/\/$/, '');
    }
    
    return window.location.origin;
  }

  getBaseUrl(): string {
    return this.baseUrl;
  }

  refreshBaseUrl(): void {
    this.baseUrl = this.resolveBaseUrl();
  }
}
