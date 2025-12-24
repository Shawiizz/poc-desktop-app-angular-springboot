import {Injectable} from '@angular/core';
import {environment} from '../environments/environment';

@Injectable({providedIn: 'root'})
export class ApiService {
  private baseUrl: string = '';

  constructor() {
    this.baseUrl = this.resolveBaseUrl();
  }

  private resolveBaseUrl(): string {
    // Priority 1: Check localStorage for port (set by Tauri launcher)
    const port = localStorage.getItem('backend_port');
    if (port) {
      return `http://localhost:${port}`;
    }
    
    // Priority 2: Check environment config
    const configured = environment.apiUrl?.trim();
    if (configured) {
      return configured.replace(/\/$/, '');
    }
    
    // Priority 3: Fallback to current origin (dev mode with Spring Boot)
    return window.location.origin;
  }

  getBaseUrl(): string {
    return this.baseUrl;
  }

  /**
   * Refresh the base URL from localStorage
   */
  refreshBaseUrl(): void {
    this.baseUrl = this.resolveBaseUrl();
  }

  /**
   * Check if the backend API is ready
   */
  async isApiReady(): Promise<boolean> {
    // Refresh URL in case port was just set
    this.refreshBaseUrl();
    
    try {
      const response = await fetch(`${this.baseUrl}/actuator/health`, {
        method: 'GET',
        signal: AbortSignal.timeout(2000)
      });
      return response.ok;
    } catch {
      return false;
    }
  }

  /**
   * Wait for the backend port to be available in localStorage,
   * then wait for the API to be healthy
   */
  async waitForApi(maxWaitMs: number = 30000, intervalMs: number = 300): Promise<boolean> {
    const startTime = Date.now();
    
    while (Date.now() - startTime < maxWaitMs) {
      const port = localStorage.getItem('backend_port');
      
      if (port) {
        this.baseUrl = `http://localhost:${port}`;
        
        try {
          const response = await fetch(`${this.baseUrl}/actuator/health`, {
            method: 'GET',
            signal: AbortSignal.timeout(2000)
          });
          if (response.ok) {
            return true;
          }
        } catch {
          // Keep waiting
        }
      }
      
      if (localStorage.getItem('backend_error') === 'true') {
        return false;
      }
      
      await new Promise(resolve => setTimeout(resolve, intervalMs));
    }
    
    return false;
  }
}
