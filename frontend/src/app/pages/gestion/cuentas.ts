import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { Confirmacion } from '../../components/confirmacion/confirmacion';
import { Estado } from '../../components/estado/estado';
import { Solicitud } from '../../components/estado/solicitud';
import { Cuenta } from '../../models/voluntia';
import { VoluntiaService } from '../../services/voluntia.service';

@Component({
  imports: [MatButtonModule, RouterLink, Estado],
  template: `
    <section class="contenedor pagina">
      <div class="pagina-cabecera"><h1>Gestionar cuentas</h1><p>Consulta, edita y da de baja cuentas de Voluntia.</p></div>
      <nav class="chips pestañas" aria-label="Tipo de cuenta">
        <a class="chip" [class.seleccionado]="tipo() === 'voluntarios'" [attr.aria-current]="tipo() === 'voluntarios' ? 'page' : null" routerLink="." [queryParams]="{ tipo: 'voluntarios' }">Voluntarios</a>
        <a class="chip" [class.seleccionado]="tipo() === 'organizaciones'" [attr.aria-current]="tipo() === 'organizaciones' ? 'page' : null" routerLink="." [queryParams]="{ tipo: 'organizaciones' }">Organizaciones</a>
      </nav>
      <app-estado [cargando]="consulta.cargando()" [error]="consulta.error()" (reintentar)="cargar()" />
      <app-estado [error]="operacion.error()" [reintentable]="false" />
      @if (!consulta.cargando() && !consulta.error()) {
        @if (items().length) {
          <div class="panel tabla-panel">
            <table class="tabla">
              <thead><tr><th scope="col">Nombre</th><th scope="col">Correo</th><th scope="col">Ubicación</th><th scope="col">Acciones</th></tr></thead>
              <tbody>
                @for (item of items(); track item.id) {
                  <tr>
                    <td>
                      @if (item.rol === 'ORGANIZACION') {
                        <a class="texto-enlace" [routerLink]="['/organizaciones', item.id]">{{ item.nombre }}</a>
                      } @else { <strong>{{ item.nombre }}</strong> }
                      @if (item.edad) { <small>{{ item.edad }} años@if (item.habilidades) { · {{ item.habilidades }}}</small> }
                      @if (item.ruc) { <small>RUC {{ item.ruc }}</small> }
                    </td>
                    <td>{{ item.email }}</td><td>{{ item.ubicacion }}</td>
                    <td><div class="acciones-fila">
                      <a mat-button [routerLink]="['/gestion/cuentas', tipo(), item.id, 'editar']">Editar</a>
                      <button mat-button type="button" [disabled]="operacion.cargando()" (click)="baja(item)">Dar de baja</button>
                    </div></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        } @else { <app-estado vacio="No hay cuentas para este perfil." /> }
      }
    </section>
  `,
})
export class GestionCuentas {
  private api = inject(VoluntiaService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);
  readonly consulta = new Solicitud();
  readonly operacion = new Solicitud();
  readonly tipo = signal<'voluntarios' | 'organizaciones'>('voluntarios');
  readonly items = signal<Cuenta[]>([]);

  constructor() {
    inject(ActivatedRoute).queryParamMap.pipe(takeUntilDestroyed()).subscribe(params => {
      this.tipo.set(params.get('tipo') === 'organizaciones' ? 'organizaciones' : 'voluntarios');
      this.cargar();
    });
  }

  cargar(): void {
    this.consulta.ejecutar(this.api.cuentas(this.tipo()), items => this.items.set(items));
  }

  baja(item: Cuenta): void {
    const tipo = this.tipo();
    this.operacion.ejecutar(this.dialog.open(Confirmacion, { data: {
      titulo: 'Dar de baja cuenta',
      mensaje: 'Se dará de baja la cuenta de «' + item.nombre + '».'
        + (tipo === 'organizaciones' ? ' Sus campañas se cancelarán.' : ' Sus inscripciones se retirarán.'),
      accion: 'Dar de baja',
    } }).afterClosed().pipe(filter(Boolean), switchMap(() => this.api.bajaCuenta(tipo, item.id))), () => {
      this.snack.open('Cuenta dada de baja.', 'Cerrar', { duration: 4000 });
      this.cargar();
    });
  }
}
