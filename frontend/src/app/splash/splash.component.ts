import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, AsyncPipe } from '@angular/common';
import { ConfigService, AppConfig } from '../services/config.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-splash',
  standalone: true,
  imports: [CommonModule, AsyncPipe],
  templateUrl: './splash.component.html',
  styleUrl: './splash.component.css'
})
export class SplashComponent implements OnInit {
  status = 'Loading...';
  fadeOut = false;
  config: AppConfig | null = null;
  
  private readonly router = inject(Router);
  private readonly configService = inject(ConfigService);

  ngOnInit(): void {
    this.initializeApp();
  }

  private async initializeApp(): Promise<void> {
    this.status = 'Initializing...';
    
    // Load config from backend
    try {
      this.config = await firstValueFrom(this.configService.getConfig());
      this.status = 'Loading resources...';
    } catch (error) {
      console.error('Failed to load config:', error);
      this.config = { name: 'Desktop App', id: 'desktop-app', version: '1.0.0', description: '' };
    }
    
    await this.delay(1500);

    this.status = 'Almost ready...';
    await this.delay(1500);

    this.status = 'Ready!';
    await this.delay(300);

    this.fadeOut = true;
    await this.delay(500);
    
    this.router.navigate(['/home']);
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
