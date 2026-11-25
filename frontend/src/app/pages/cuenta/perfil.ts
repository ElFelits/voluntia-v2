import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { Estado } from '../../components/estado/estado';
import { Solicitud } from '../../components/estado/solicitud';
import { Cuenta, Perfil, Rol } from '../../models/voluntia';
import { SesionService, VoluntiaService } from '../../services/voluntia.service';

const textoObligatorio = [Validators.required, Validators.pattern(/\S/)];
const camposVoluntario = ['edad', 'habilidades'] as const;
const camposOrganizacion = ['ruc', 'infoContacto'] as const;

@Component({
  imports: [Estado, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, RouterLink],
  templateUrl: './perfil.html',
})
export class PerfilPagina {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(VoluntiaService);
  private sesion = inject(SesionService);
  private snack = inject(MatSnackBar);
  readonly registro = this.route.snapshot.data['modo'] === 'registro';
  readonly admin = this.route.snapshot.data['modo'] === 'admin';
  readonly rol = signal<Rol>('VOLUNTARIO');
  readonly consulta = new Solicitud();
  readonly operacion = new Solicitud();
  readonly listo = signal(this.registro);
  private id = 0;
  private tipo: 'voluntarios' | 'organizaciones' = 'voluntarios';
  readonly form = inject(FormBuilder).group({
    rol: inject(FormBuilder).nonNullable.control<'VOLUNTARIO' | 'ORGANIZACION'>('VOLUNTARIO'),
    nombre: ['', [...textoObligatorio, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    ubicacion: ['', [...textoObligatorio, Validators.maxLength(255)]],
    contrasena: ['', this.registro ? [Validators.required, Validators.minLength(8), Validators.maxLength(72)] : []],
    edad: [null as number | null, [Validators.required, Validators.min(16), Validators.max(120), Validators.pattern(/^\d+$/)]],
    habilidades: ['', Validators.maxLength(255)],
    ruc: ['', [Validators.required, Validators.pattern(/^\d{11}$/)]],
    infoContacto: ['', [...textoObligatorio, Validators.maxLength(200)]],
  });

  constructor() {
    this.form.controls.rol.valueChanges.pipe(takeUntilDestroyed()).subscribe(rol => this.configurar(rol));
    if (this.registro) {
      this.configurar('VOLUNTARIO');
    } else {
      this.route.paramMap.pipe(takeUntilDestroyed()).subscribe(() => this.cargar());
    }
  }

  cargar(): void {
    this.listo.set(false);
    if (this.admin) {
      const params = this.route.snapshot.paramMap;
      const tipo = params.get('tipo');
      this.id = Number(params.get('id'));
      if (tipo !== 'voluntarios' && tipo !== 'organizaciones') {
        this.consulta.error.set('Este tipo de cuenta no está disponible.');
        return;
      }
      this.tipo = tipo;
      this.consulta.ejecutar(this.api.cuentas(this.tipo).pipe(
        map(cuentas => cuentas.find(cuenta => cuenta.id === this.id)),
      ), cuenta => this.cargarCuenta(cuenta));
      return;
    }
    this.cargarCuenta(this.sesion.cuenta() ?? undefined);
  }

  private cargarCuenta(cuenta: Cuenta | undefined): void {
    if (!cuenta) {
      this.consulta.error.set('No encontramos esta cuenta.');
      return;
    }
    const { nombre, email, ubicacion, edad, habilidades, ruc, infoContacto } = cuenta;
    this.form.patchValue({ nombre, email, ubicacion, edad: edad ?? null, habilidades: habilidades ?? '', ruc: ruc ?? '', infoContacto: infoContacto ?? '' });
    this.configurar(cuenta.rol);
    this.listo.set(true);
  }

  private configurar(rol: Rol): void {
    this.rol.set(rol);
    for (const campo of camposVoluntario) {
      if (rol === 'VOLUNTARIO') this.form.controls[campo].enable();
      else this.form.controls[campo].disable();
    }
    for (const campo of camposOrganizacion) {
      if (rol === 'ORGANIZACION') this.form.controls[campo].enable();
      else this.form.controls[campo].disable();
    }
  }

  guardar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.operacion.cargando()) return;
    const { nombre, email, ubicacion, edad, habilidades, ruc, infoContacto, contrasena, rol } = this.form.getRawValue();
    const datos: Perfil = { nombre: nombre!.trim(), email: email!.trim(), ubicacion: ubicacion!.trim() };
    if (this.rol() === 'VOLUNTARIO') Object.assign(datos, { edad: edad!, habilidades: habilidades?.trim() });
    if (this.rol() === 'ORGANIZACION') Object.assign(datos, { ruc: ruc!.trim(), infoContacto: infoContacto!.trim() });
    const resultado = this.registro ? this.sesion.registrar({ ...datos, rol, contrasena: contrasena! })
      : this.admin ? this.api.editarCuenta(this.tipo, this.id, datos) : this.sesion.actualizar(datos);

    this.operacion.ejecutar(resultado, cuenta => {
      this.snack.open(this.registro ? 'Tu cuenta ya está lista.' : 'Cambios guardados.', 'Cerrar', { duration: 4000 });
      if (this.registro) {
        void this.router.navigateByUrl(cuenta.rol === 'VOLUNTARIO' ? '/campanias' : '/gestion/campanias');
      } else if (this.admin) {
        void this.router.navigate(['/gestion/cuentas'], { queryParams: { tipo: this.tipo } });
      }
    });
  }
}
