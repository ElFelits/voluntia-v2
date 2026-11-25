import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Encabezado } from './components/encabezado/encabezado';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, Encabezado],
  templateUrl: './app.html',
})
export class App {
  readonly anio = new Date().getFullYear();
}
