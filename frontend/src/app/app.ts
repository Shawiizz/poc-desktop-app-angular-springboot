import {Component, inject, signal} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import {ApiService} from './api.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('Application Spring Boot + Angular');
  protected readonly message = signal('');
  protected readonly status = signal('');

  private readonly http: HttpClient = inject(HttpClient);
  private readonly apiService: ApiService = inject(ApiService);

  callBackend() {
    this.http.post(`${this.apiService.getBaseUrl()}/api/hello`, 'Message from Angular!', {
      headers: { 'Content-Type': 'text/plain' },
      responseType: 'text'
    }).subscribe({
      next: (response) => {
        console.log('✅ RResponse received from back-end:', response);
        this.message.set(response);
      },
      error: (error) => {
        console.error('❌ Error occurred while calling back-end:', error);
        this.message.set('Error occurred while calling back-end');
      }
    });
  }
}
