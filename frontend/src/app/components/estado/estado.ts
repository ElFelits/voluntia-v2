import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-estado',
  imports: [MatButtonModule, MatProgressSpinnerModule],
  template: `
    @if (cargando()) {
      <div class="estado" role="status"><mat-spinner diameter="24" /> Cargando…</div>
    } @else if (error()) {
      <div class="estado estado-error" role="alert">
        <span>{{ error() }}</span>
        @if (reintentable()) { <button mat-stroked-button type="button" (click)="reintentar.emit()">Reintentar</button> }
      </div>
    } @else if (vacio()) {
      <div class="estado" role="status">{{ vacio() }}</div>
    }
  `,
})
export class Estado {
  cargando = input(false);
  error = input('');
  vacio = input('');
  reintentable = input(true);
  reintentar = output<void>();
}
