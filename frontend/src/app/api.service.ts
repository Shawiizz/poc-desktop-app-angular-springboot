import {Injectable} from '@angular/core';
import {environment} from '../environments/environment';

@Injectable({providedIn: 'root'})
export class ApiService {
  private readonly baseUrl = this.resolveBaseUrl();

  private resolveBaseUrl(): string {
    const configured = environment.apiBaseUrl?.trim();
    if (configured) {
      return configured.replace(/\/$/, '');
    }
    // Fallback: use current origin (works when frontend est servi par Spring Boot sur port dynamique)
    return window.location.origin;
  }

  getBaseUrl(): string {
    return this.baseUrl;
  }
}
