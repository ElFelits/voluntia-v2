import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { etiquetasRol } from '../../models/voluntia';
import { mensajeError, SesionService } from '../../services/voluntia.service';

@Component({
  selector: 'app-encabezado',
  imports: [RouterLink, RouterLinkActive, MatButtonModule, MatMenuModule],
  templateUrl: './encabezado.html',
})
export class Encabezado {
  readonly sesion = inject(SesionService);
  readonly etiquetas = etiquetasRol;
  readonly saliendo = signal(false);
  readonly menuAbierto = signal(false);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  constructor() { this.sesion.restaurar().subscribe(); }

  salir(): void {
    if (this.saliendo()) return;
    this.saliendo.set(true);
    this.sesion.salir().subscribe({
      next: () => {
        this.saliendo.set(false);
        this.menuAbierto.set(false);
        void this.router.navigate(['/']);
      },
      error: error => {
        this.saliendo.set(false);
        this.snackBar.open(mensajeError(error), 'Cerrar', { duration: 5000 });
      },
    });
  }
}
