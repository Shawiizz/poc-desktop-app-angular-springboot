import {Injectable} from '@angular/core';
import {environment} from '../environments/environment';

@Injectable({providedIn: 'root'})
export class ApiService {
  private readonly baseUrl = this.resolveBaseUrl();

  private resolveBaseUrl(): string {
    const configured = environment.apiUrl?.trim();
    if (configured) {
      return configured.replace(/\/$/, '');
    }
    // Fallback: use current origin (works when frontend is served by Spring Boot)
    return window.location.origin;
  }

  getBaseUrl(): string {
    return this.baseUrl;
  }
}
