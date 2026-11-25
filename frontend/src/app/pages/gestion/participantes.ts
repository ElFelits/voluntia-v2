import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { filter, forkJoin, switchMap } from 'rxjs';
import { Confirmacion } from '../../components/confirmacion/confirmacion';
import { Estado } from '../../components/estado/estado';
import { Solicitud } from '../../components/estado/solicitud';
import { Campania, Inscripcion } from '../../models/voluntia';
import { VoluntiaService } from '../../services/voluntia.service';

@Component({
  imports: [MatButtonModule, RouterLink, Estado],
  template: `
    <section class="contenedor pagina">
      <a class="volver" routerLink="/gestion/campanias"><span class="icon" aria-hidden="true">arrow_back</span> Gestión de campañas</a>
      <div class="pagina-cabecera"><h1>Participantes</h1><p>{{ campania()?.titulo }}</p></div>
      <app-estado [cargando]="consulta.cargando()" [error]="consulta.error()" (reintentar)="cargar()" />
      <app-estado [error]="operacion.error()" [reintentable]="false" />
      @if (!consulta.cargando() && !consulta.error()) {
        @if (items().length) {
          <div class="panel tabla-panel">
            <table class="tabla">
              <thead><tr><th scope="col">Voluntario</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
              <tbody>
                @for (item of items(); track item.id) {
                  <tr>
                    <td>{{ item.voluntario }}</td>
                    <td><span class="etiqueta">{{ item.estado === 'ASISTIO' ? 'Asistió' : 'Inscrito' }}</span></td>
                    <td>
                      @if (item.estado === 'INSCRITO' && campania()?.estado === 'ACTIVA') {
                        <div class="acciones-fila">
                          <button mat-button type="button" [disabled]="operacion.cargando()" (click)="asistencia(item)">Marcar asistencia</button>
                          <button mat-button type="button" [disabled]="operacion.cargando()" (click)="retirar(item)">Retirar participante</button>
                        </div>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        } @else { <app-estado vacio="Esta campaña aún no tiene participantes." /> }
      }
    </section>
  `,
})
export class Participantes {
  private api = inject(VoluntiaService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);
  readonly consulta = new Solicitud();
  readonly operacion = new Solicitud();
  readonly campania = signal<Campania | null>(null);
  readonly items = signal<Inscripcion[]>([]);
  private id = 0;

  constructor() {
    inject(ActivatedRoute).paramMap.pipe(takeUntilDestroyed()).subscribe(params => {
      this.id = Number(params.get('id'));
      this.campania.set(null);
      this.cargar();
    });
  }

  cargar(): void {
    this.consulta.ejecutar(forkJoin({ campania: this.api.campania(this.id), items: this.api.participantes(this.id) }),
      ({ campania, items }) => {
        this.campania.set(campania);
        this.items.set(items);
      });
  }

  asistencia(item: Inscripcion): void {
    this.operacion.ejecutar(this.api.marcarAsistencia(this.id, item.id), () => {
      this.snack.open('Asistencia registrada.', 'Cerrar', { duration: 4000 });
      this.cargar();
    });
  }

  retirar(item: Inscripcion): void {
    this.operacion.ejecutar(this.dialog.open(Confirmacion, { data: {
      titulo: 'Retirar participante',
      mensaje: 'Se cancelará la inscripción de ' + item.voluntario + ' y su cupo quedará disponible.',
      accion: 'Retirar participante',
    } }).afterClosed().pipe(filter(Boolean), switchMap(() => this.api.retirarParticipante(this.id, item.id))), () => {
      this.snack.open('Participante retirado.', 'Cerrar', { duration: 4000 });
      this.cargar();
    });
  }
}
