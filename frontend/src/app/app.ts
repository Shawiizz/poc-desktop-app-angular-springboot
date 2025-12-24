import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TitlebarComponent } from './titlebar/titlebar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, TitlebarComponent],
  template: `
    <app-titlebar></app-titlebar>
    <div class="app-content">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: [`
    .app-content {
      margin-top: 32px;
      height: calc(100vh - 32px);
      overflow: auto;
      scrollbar-width: none;
      -ms-overflow-style: none;
    }
    .app-content::-webkit-scrollbar {
      display: none;
    }
  `]
})
export class App {}

