import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ConfigService, AppConfig } from '../services/config.service';
import { ApiService } from '../api.service';
import { firstValueFrom } from 'rxjs';
import { listen, UnlistenFn } from '@tauri-apps/api/event';

const BACKEND_TIMEOUT_MS = 30_000;
const FADE_OUT_DELAY_MS = 400;

const DEFAULT_CONFIG: AppConfig = {
  name: 'Desktop App',
  id: 'desktop-app',
  version: '1.0.0',
  description: ''
};

interface BackendReadyPayload {
  port: number;
}

@Component({
  selector: 'app-splash',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './splash.component.html',
  styleUrl: './splash.component.css'
})
export class SplashComponent implements OnInit, OnDestroy {
  protected status = 'Starting...';
  protected fadeOut = false;
  protected config: AppConfig | null = null;

  private readonly router = inject(Router);
  private readonly configService = inject(ConfigService);
  private readonly apiService = inject(ApiService);
  private readonly unlistenFns: UnlistenFn[] = [];

  ngOnInit(): void {
    this.initializeApp();
  }

  ngOnDestroy(): void {
    this.cleanupListeners();
  }

  private async initializeApp(): Promise<void> {
    this.status = 'Waiting for backend...';

    const apiReady = await this.waitForBackendPort();
    if (!apiReady) {
      this.status = 'Backend failed to start';
      return;
    }

    this.status = 'Loading configuration...';
    await this.loadConfiguration();

    this.status = 'Ready!';
    this.fadeOut = true;
    await this.delay(FADE_OUT_DELAY_MS);
    this.router.navigate(['/home']);
  }

  private async loadConfiguration(): Promise<void> {
    try {
      this.config = await firstValueFrom(this.configService.getConfig());
    } catch {
      this.config = DEFAULT_CONFIG;
    }
  }

  private waitForBackendPort(): Promise<boolean> {
    if (this.apiService.isReady()) {
      this.status = 'Connecting to backend...';
      return Promise.resolve(true);
    }

    return new Promise<boolean>((resolve) => {
      const timeout = setTimeout(() => {
        this.cleanupListeners();
        resolve(false);
      }, BACKEND_TIMEOUT_MS);

      this.registerListener('backend-ready', (event: { payload: BackendReadyPayload }) => {
        clearTimeout(timeout);
        this.apiService.setPort(event.payload.port);
        this.status = 'Connecting to backend...';
        resolve(true);
      });

      this.registerListener('backend-error', () => {
        clearTimeout(timeout);
        resolve(false);
      });
    });
  }

  private registerListener<T>(event: string, handler: (event: { payload: T }) => void): void {
    listen<T>(event, handler).then((unlisten) => this.unlistenFns.push(unlisten));
  }

  private cleanupListeners(): void {
    this.unlistenFns.forEach((unlisten) => unlisten());
    this.unlistenFns.length = 0;
  }

  private delay(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }
}
