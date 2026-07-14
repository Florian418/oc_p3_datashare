import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Composant racine — héberge le `<router-outlet>`.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {}
