import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { ApiService } from '../api.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css', '../app.css']
})
export class HomeComponent {
  protected readonly title = signal('Application Spring Boot + Angular');
  protected readonly message = signal('');

  private readonly http = inject(HttpClient);
  private readonly apiService = inject(ApiService);

  callBackend(): void {
    this.http.post(`${this.apiService.getBaseUrl()}/api/hello`, 'Message from Angular!', {
      headers: { 'Content-Type': 'text/plain' },
      responseType: 'text'
    }).subscribe({
      next: (response) => {
        console.log('Response received from back-end:', response);
        this.message.set(response);
      },
      error: (error) => {
        console.error('Error occurred while calling back-end:', error);
        this.message.set('Error occurred while calling back-end');
      }
    });
  }
}
