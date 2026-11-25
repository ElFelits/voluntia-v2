import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, map } from 'rxjs';
import { Estado } from '../../components/estado/estado';
import { Solicitud } from '../../components/estado/solicitud';
import { VoluntiaService } from '../../services/voluntia.service';

const cantidad = [Validators.required, Validators.min(0), Validators.max(100000000)];
const entero = [...cantidad, Validators.pattern(/^\d+$/)];

@Component({
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule, Estado],
  template: `
    <section class="contenedor pagina pagina-form">
      <a class="volver" routerLink="/gestion/campanias"><span class="icon" aria-hidden="true">arrow_back</span> Gestión de campañas</a>
      <div class="pagina-cabecera"><h1>Registrar impacto</h1><p>{{ titulo() }}</p></div>
      <app-estado [cargando]="consulta.cargando()" [error]="consulta.error()" (reintentar)="cargar()" />
      @if (titulo() && !consulta.cargando() && !consulta.error()) {
        <div class="panel">
          <form class="form-grid" [formGroup]="form" (ngSubmit)="guardar()">
            <mat-form-field appearance="outline">
              <mat-label>Horas de voluntariado</mat-label><input matInput type="number" min="0" formControlName="horas">
              <mat-error>Ingresa un número entre 0 y 100 000 000.</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Árboles plantados</mat-label><input matInput type="number" min="0" step="1" formControlName="arboles">
              <mat-error>Ingresa un entero entre 0 y 100 000 000.</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Residuos recolectados (kg)</mat-label><input matInput type="number" min="0" step="any" formControlName="residuosKg">
              <mat-error>Ingresa un número entre 0 y 100 000 000.</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Materiales recuperados (kg)</mat-label><input matInput type="number" min="0" step="any" formControlName="materialesKg">
              <mat-error>Ingresa un número entre 0 y 100 000 000.</mat-error>
            </mat-form-field>
            <div class="ancho">
              <app-estado [error]="operacion.error()" [reintentable]="false" />
              <div class="acciones-fila">
                <button mat-flat-button type="submit" [disabled]="operacion.cargando()">Guardar resultados</button>
                <a mat-stroked-button routerLink="/gestion/impacto">Ver dashboard</a>
              </div>
            </div>
          </form>
        </div>
      }
    </section>
  `,
})
export class FormImpacto {
  private api = inject(VoluntiaService);
  private router = inject(Router);
  private snack = inject(MatSnackBar);
  readonly consulta = new Solicitud();
  readonly operacion = new Solicitud();
  readonly titulo = signal('');
  private id = 0;
  readonly form = inject(FormBuilder).nonNullable.group({
    horas: [0, cantidad], arboles: [0, entero], residuosKg: [0, cantidad], materialesKg: [0, cantidad],
  });

  constructor() {
    inject(ActivatedRoute).paramMap.pipe(takeUntilDestroyed()).subscribe(params => {
      this.id = Number(params.get('id'));
      this.cargar();
    });
  }

  cargar(): void {
    this.titulo.set('');
    const campania = this.api.campania(this.id).pipe(map(item => {
      if (item.estado === 'CANCELADA') {
        throw new HttpErrorResponse({ status: 409, error: { mensaje: 'La campaña está cancelada.' } });
      }
      return item;
    }));
    this.consulta.ejecutar(forkJoin({ campania, resultado: this.api.resultado(this.id) }), ({ campania, resultado }) => {
      this.titulo.set(campania.titulo);
      this.form.setValue(resultado);
    });
  }

  guardar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.operacion.cargando()) return;
    this.operacion.ejecutar(this.api.guardarResultado(this.id, this.form.getRawValue()), () => {
      this.snack.open('Resultados guardados.', 'Cerrar', { duration: 4000 });
      void this.router.navigate(['/gestion/impacto']);
    });
  }
}
