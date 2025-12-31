import { Injectable } from '@angular/core';
import { environment } from '../environments/environment';

const LOCALHOST = 'http://localhost';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private port: number | null = null;

  /** Set the backend port (called when backend-ready event is received) */
  setPort(port: number): void {
    this.port = port;
  }

  /** Get the backend port (null if not yet set) */
  getPort(): number | null {
    return this.port;
  }

  /** Check if the backend port has been set */
  isReady(): boolean {
    return this.port !== null;
  }

  /** Get the base URL for API calls */
  getBaseUrl(): string {
    if (this.port !== null) {
      return `${LOCALHOST}:${this.port}`;
    }

    const configured = environment.apiUrl?.trim();
    if (configured) {
      return configured.replace(/\/$/, '');
    }

    return window.location.origin;
  }
}
