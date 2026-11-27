import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { filter, forkJoin, of, switchMap } from 'rxjs';
import { Confirmacion } from '../../components/confirmacion/confirmacion';
import { Estado } from '../../components/estado/estado';
import { Solicitud } from '../../components/estado/solicitud';
import { Campania, Inscripcion } from '../../models/voluntia';
import { SesionService, VoluntiaService } from '../../services/voluntia.service';

@Component({
  imports: [DatePipe, RouterLink, MatButtonModule, Estado],
  templateUrl: './detalle.html',
})
export class Detalle {
  private api = inject(VoluntiaService);
  private route = inject(ActivatedRoute);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);
  readonly sesion = inject(SesionService);
  readonly consulta = new Solicitud();
  readonly operacion = new Solicitud();
  readonly campania = signal<Campania | null>(null);
  readonly portada = computed(() => {
    const item = this.campania();
    return item ? this.api.portadaUrl(item) : null;
  });
  readonly portadaFallida = signal<string | null>(null);
  readonly inscripcion = signal<Inscripcion | null>(null);
  readonly hoy = Date.now();
  private id = 0;

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe(params => {
      this.id = Number(params.get('id'));
      this.campania.set(null);
      this.cargar();
    });
  }

  cargar(): void {
    this.consulta.ejecutar(this.sesion.restaurar().pipe(switchMap(cuenta => forkJoin({
      campania: this.api.campania(this.id),
      inscripcion: cuenta?.rol === 'VOLUNTARIO' ? this.api.miInscripcion(this.id) : of(null),
    }))), ({ campania, inscripcion }) => {
      this.campania.set(campania);
      this.inscripcion.set(inscripcion);
    });
  }

  finalizada(campania: Campania): boolean { return new Date(campania.fecha).getTime() <= this.hoy; }

  inscribir(): void {
    this.operacion.ejecutar(this.api.inscribir(this.id), () => {
      this.snack.open('Inscripción confirmada.', 'Cerrar', { duration: 4000 });
      this.cargar();
    });
  }

  cancelar(): void {
    this.operacion.ejecutar(this.dialog.open(Confirmacion, { data: {
      titulo: 'Cancelar inscripción',
      mensaje: 'Tu cupo quedará disponible para otra persona.',
      accion: 'Cancelar inscripción',
    } }).afterClosed().pipe(filter(Boolean), switchMap(() => this.api.cancelarInscripcion(this.id))), () => {
      this.snack.open('Inscripción cancelada.', 'Cerrar', { duration: 4000 });
      this.cargar();
    });
  }
}
