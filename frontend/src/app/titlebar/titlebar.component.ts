import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfigService } from '../services/config.service';
import { getCurrentWindow } from '@tauri-apps/api/window';

@Component({
  selector: 'app-titlebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './titlebar.component.html',
  styleUrls: ['./titlebar.component.css']
})
export class TitlebarComponent implements OnInit {
  appName = 'Desktop App';
  private readonly configService = inject(ConfigService);

  ngOnInit(): void {
    this.configService.getConfig().subscribe({
      next: (config) => {
        this.appName = config.name;
      },
      error: () => {
        // Keep default name
      }
    });
  }

  async minimize(): Promise<void> {
    try {
      const appWindow = getCurrentWindow();
      await appWindow.minimize();
    } catch (e) {
      console.error('Failed to minimize:', e);
    }
  }

  async toggleMaximize(): Promise<void> {
    try {
      const appWindow = getCurrentWindow();
      await appWindow.toggleMaximize();
    } catch (e) {
      console.error('Failed to toggle maximize:', e);
    }
  }

  async close(): Promise<void> {
    try {
      const appWindow = getCurrentWindow();
      await appWindow.close();
    } catch (e) {
      console.error('Failed to close:', e);
    }
  }
}
