import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { Confirmacion } from '../../components/confirmacion/confirmacion';
import { Estado } from '../../components/estado/estado';
import { Solicitud } from '../../components/estado/solicitud';
import { Participacion } from '../../models/voluntia';
import { VoluntiaService } from '../../services/voluntia.service';

@Component({
  imports: [DatePipe, MatButtonModule, RouterLink, Estado],
  template: `
    <section class="contenedor pagina">
      <div class="pagina-cabecera titulo-fila">
        <div><h1>Mis participaciones</h1><p>Consulta tus inscripciones y el historial de asistencia.</p></div>
        <a mat-stroked-button routerLink="/campanias">Explorar campañas</a>
      </div>
      <app-estado [cargando]="consulta.cargando()" [error]="consulta.error()" (reintentar)="cargar()" />
      <app-estado [error]="operacion.error()" [reintentable]="false" />
      @if (!consulta.cargando() && !consulta.error()) {
        @if (items().length) {
          <div class="panel tabla-panel">
            <table class="tabla">
              <thead><tr><th scope="col">Campaña</th><th scope="col">Fecha</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
              <tbody>
                @for (item of items(); track item.id) {
                  <tr>
                    <td><a class="texto-enlace" [routerLink]="['/campanias', item.campaniaId]">{{ item.campania.titulo }}</a><small>{{ item.campania.ubicacion }}</small></td>
                    <td>{{ item.campania.fecha | date:'dd/MM/y' }}</td>
                    <td><span class="etiqueta" [class.neutra]="item.estado === 'CANCELADO'">{{ etiquetas[item.estado] }}</span></td>
                    <td>
                      @if (item.estado === 'INSCRITO') {
                        <button mat-button type="button" [disabled]="operacion.cargando()" (click)="cancelar(item)">Cancelar inscripción</button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        } @else { <app-estado vacio="Todavía no tienes participaciones registradas." /> }
      }
    </section>
  `,
})
export class Participaciones {
  private api = inject(VoluntiaService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);
  readonly consulta = new Solicitud();
  readonly operacion = new Solicitud();
  readonly items = signal<Participacion[]>([]);
  readonly etiquetas = { INSCRITO: 'Inscrito', ASISTIO: 'Asistió', CANCELADO: 'Cancelada' };

  constructor() { this.cargar(); }

  cargar(): void {
    this.consulta.ejecutar(this.api.misParticipaciones(), items =>
      this.items.set([...items].sort((a, b) => b.campania.fecha.localeCompare(a.campania.fecha))));
  }

  cancelar(item: Participacion): void {
    this.operacion.ejecutar(this.dialog.open(Confirmacion, { data: {
      titulo: 'Cancelar inscripción',
      mensaje: 'Tu cupo en «' + item.campania.titulo + '» quedará disponible.',
      accion: 'Cancelar inscripción',
    } }).afterClosed().pipe(filter(Boolean), switchMap(() => this.api.cancelarInscripcion(item.campaniaId))), () => {
      this.snack.open('Inscripción cancelada.', 'Cerrar', { duration: 4000 });
      this.cargar();
    });
  }
}
