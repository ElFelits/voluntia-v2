import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { Confirmacion } from '../../components/confirmacion/confirmacion';
import { Estado } from '../../components/estado/estado';
import { Solicitud } from '../../components/estado/solicitud';
import { Campania } from '../../models/voluntia';
import { SesionService, VoluntiaService } from '../../services/voluntia.service';

@Component({
  imports: [DatePipe, RouterLink, MatButtonModule, MatTableModule, Estado],
  templateUrl: './campanias.html',
})
export class GestionCampanias {
  private api = inject(VoluntiaService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);
  readonly rol = inject(SesionService).cuenta()?.rol;
  readonly consulta = new Solicitud();
  readonly operacion = new Solicitud();
  readonly items = signal<Campania[]>([]);
  readonly columnas = ['campania', 'fecha', 'cupos', 'estado', 'acciones'];

  constructor() { this.cargar(); }

  cargar(): void {
    this.consulta.ejecutar(this.api.listarCampanias(undefined, true), items => this.items.set(items));
  }

  estado(item: Campania): string {
    if (item.estado === 'CANCELADA') return 'Cancelada';
    return new Date(item.fecha).getTime() > Date.now() ? 'Activa' : 'Finalizada';
  }

  cancelar(item: Campania): void {
    this.operacion.ejecutar(this.dialog.open(Confirmacion, { data: {
      titulo: 'Cancelar campaña',
      mensaje: 'Las inscripciones activas de «' + item.titulo + '» se cancelarán con la campaña.',
      accion: 'Cancelar campaña',
    } }).afterClosed().pipe(filter(Boolean), switchMap(() => this.api.cancelarCampania(item.id))), () => {
      this.snack.open('Campaña cancelada.', 'Cerrar', { duration: 4000 });
      this.cargar();
    });
  }
}
