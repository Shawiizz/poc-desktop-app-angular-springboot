import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay, map } from 'rxjs';
import { ApiService } from '../api.service';

export interface AppConfig {
  name: string;
  id: string;
  version: string;
  description: string;
}

@Injectable({
  providedIn: 'root'
})
export class ConfigService {
  private readonly http = inject(HttpClient);
  private readonly apiService = inject(ApiService);
  
  private config$: Observable<AppConfig> | null = null;

  /**
   * Get the application configuration.
   * The result is cached and shared across all subscribers.
   */
  getConfig(): Observable<AppConfig> {
    if (!this.config$) {
      this.config$ = this.http.get<AppConfig>(`${this.apiService.getBaseUrl()}/api/config`).pipe(
        shareReplay(1)
      );
    }
    return this.config$;
  }

  /**
   * Get the application name
   */
  getName(): Observable<string> {
    return this.getConfig().pipe(map(config => config.name));
  }

  /**
   * Get the application version
   */
  getVersion(): Observable<string> {
    return this.getConfig().pipe(map(config => config.version));
  }
}
