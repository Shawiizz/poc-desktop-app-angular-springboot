import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-splash',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './splash.component.html',
  styleUrl: './splash.component.css'
})
export class SplashComponent implements OnInit {
  status = 'Loading...';
  fadeOut = false;
  
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.initializeApp();
  }

  private async initializeApp(): Promise<void> {
    this.status = 'Initializing...';
    await this.delay(1000);

    this.status = 'Loading resources...';
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
