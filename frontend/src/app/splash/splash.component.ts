import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ConfigService, AppConfig } from '../services/config.service';
import { ApiService } from '../api.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-splash',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './splash.component.html',
  styleUrl: './splash.component.css'
})
export class SplashComponent implements OnInit {
  status = 'Starting...';
  fadeOut = false;
  config: AppConfig | null = null;
  
  private readonly router = inject(Router);
  private readonly configService = inject(ConfigService);
  private readonly apiService = inject(ApiService);

  ngOnInit(): void {
    this.initializeApp();
  }

  private async initializeApp(): Promise<void> {
    this.status = 'Waiting for backend...';
    
    // Wait for backend API to be ready (port is set by Tauri via localStorage)
    const apiReady = await this.waitForApiWithStatus(30000, 300);
    
    if (!apiReady) {
      this.status = 'Backend failed to start';
      return;
    }
    
    this.status = 'Loading configuration...';
    
    // Load config from backend
    try {
      this.config = await firstValueFrom(this.configService.getConfig());
    } catch (error) {
      this.config = { name: 'Desktop App', id: 'desktop-app', version: '1.0.0', description: '' };
    }
    
    await this.delay(500);
    this.status = 'Ready!';
    await this.delay(300);

    // Fade out and navigate to home
    this.fadeOut = true;
    await this.delay(500);
    
    this.router.navigate(['/home']);
  }

  private async waitForApiWithStatus(maxWaitMs: number, intervalMs: number): Promise<boolean> {
    const startTime = Date.now();
    
    while (Date.now() - startTime < maxWaitMs) {
      const port = localStorage.getItem('backend_port');
      
      if (port) {
        this.status = 'Connecting to backend...';
        const baseUrl = `http://localhost:${port}`;
        
        try {
          const response = await fetch(`${baseUrl}/actuator/health`, {
            method: 'GET',
            signal: AbortSignal.timeout(2000)
          });
          if (response.ok) {
            this.apiService.refreshBaseUrl();
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

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
