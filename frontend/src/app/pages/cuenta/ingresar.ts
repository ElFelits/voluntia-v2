import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Estado } from '../../components/estado/estado';
import { Solicitud } from '../../components/estado/solicitud';
import { SesionService } from '../../services/voluntia.service';

@Component({
  imports: [Estado, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, RouterLink],
  template: `
    <section class="contenedor pagina pagina-angosta">
      <div class="pagina-cabecera">
        <h1>Iniciar sesión</h1>
        <p>Ingresa a tu cuenta de Voluntia.</p>
      </div>
      <div class="panel">
        <form class="form-stack" [formGroup]="form" (ngSubmit)="ingresar()">
          <mat-form-field appearance="outline">
            <mat-label>Correo electrónico</mat-label>
            <input matInput type="email" autocomplete="username" formControlName="email">
            <mat-error>Ingresa un correo válido.</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Contraseña</mat-label>
            <input matInput type="password" autocomplete="current-password" formControlName="contrasena">
            <mat-error>Ingresa tu contraseña.</mat-error>
          </mat-form-field>
          <app-estado [error]="solicitud.error()" [reintentable]="false" />
          <button mat-flat-button type="submit" [disabled]="solicitud.cargando()">Iniciar sesión</button>
        </form>
      </div>
      <p class="form-nota">¿Todavía no tienes cuenta? <a class="texto-enlace" routerLink="/registro">Crear cuenta</a></p>
    </section>
  `,
})
export class Ingresar {
  private sesion = inject(SesionService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  readonly solicitud = new Solicitud();
  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]], contrasena: ['', Validators.required],
  });

  ingresar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.solicitud.cargando()) return;
    this.solicitud.ejecutar(this.sesion.ingresar(this.form.getRawValue()), cuenta => {
      const volver = this.route.snapshot.queryParamMap.get('volver');
      const destino = volver && /^\/(?![\/\\])/.test(volver)
        ? volver
        : cuenta.rol === 'VOLUNTARIO' ? '/campanias' : '/gestion/campanias';
      void this.router.navigateByUrl(destino);
    });
  }
}
