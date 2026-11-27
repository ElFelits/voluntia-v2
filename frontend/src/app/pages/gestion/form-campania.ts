import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, map } from 'rxjs';
import { Estado } from '../../components/estado/estado';
import { Solicitud } from '../../components/estado/solicitud';
import { Categoria, categorias } from '../../models/voluntia';
import { camposUbicacion } from '../../models/ubicacion';
import { SesionService, VoluntiaService } from '../../services/voluntia.service';

const requerido = [Validators.required, Validators.pattern(/\S/)];
const fechaValida = (campo: AbstractControl) => campo.value && !Number.isFinite(new Date(campo.value).getTime()) ? { fechaInvalida: true } : null;

@Component({
  imports: [Estado, RouterLink, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './form-campania.html',
})
export class FormCampania {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(VoluntiaService);
  private sesion = inject(SesionService);
  private snack = inject(MatSnackBar);
  readonly categorias = categorias;
  readonly distritos = signal<readonly string[]>([]);
  readonly consulta = new Solicitud();
  readonly operacion = new Solicitud();
  readonly id = signal<number | null>(null);
  readonly listo = signal(false);
  readonly portadaPrevia = signal<string | null>(null);
  readonly preparandoPortada = signal(false);
  readonly errorPortada = signal('');
  private cambioPortada?: string;
  private lectorPortada?: FileReader;
  readonly form = inject(FormBuilder).nonNullable.group({
    titulo: ['', [...requerido, Validators.maxLength(150)]],
    categoria: ['Ambiental' as Categoria, Validators.required],
    descripcion: ['', [...requerido, Validators.maxLength(255)]],
    ubicacion: ['', Validators.required],
    direccion: ['', Validators.maxLength(255)],
    fecha: ['', [Validators.required, fechaValida]],
    cupos: [1, [Validators.required, Validators.min(1), Validators.max(100000), Validators.pattern(/^\d+$/)]],
    requisitos: ['', Validators.maxLength(255)],
  });

  constructor() {
    inject(DestroyRef).onDestroy(() => this.lectorPortada?.abort());
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe(params => {
      const campo = params.get('id');
      this.id.set(campo === null ? null : Number(campo));
      this.cargar();
    });
  }

  cargar(): void {
    const id = this.id();
    this.lectorPortada?.abort();
    this.lectorPortada = undefined;
    this.preparandoPortada.set(false);
    this.errorPortada.set('');
    this.portadaPrevia.set(null);
    this.cambioPortada = undefined;
    this.listo.set(false);
    if (id === null) {
      this.form.reset();
      this.consulta.ejecutar(this.api.distritos, distritos => {
        this.distritos.set(distritos);
        this.listo.set(true);
      });
      return;
    }
    const campania = this.api.campania(id).pipe(map(item => {
      const cuenta = this.sesion.cuenta();
      if (cuenta?.rol !== 'ADMIN' && cuenta?.id !== item.organizacionId) {
        throw new HttpErrorResponse({ status: 403, error: { mensaje: 'Solo puedes gestionar tus campañas.' } });
      }
      if (item.estado === 'CANCELADA') {
        throw new HttpErrorResponse({ status: 409, error: { mensaje: 'La campaña está cancelada.' } });
      }
      return item;
    }));
    this.consulta.ejecutar(forkJoin({ item: campania, distritos: this.api.distritos }), ({ item, distritos }) => {
      if (this.id() !== id) return;
      this.distritos.set(distritos);
      this.form.setValue({
        titulo: item.titulo, categoria: item.categoria, descripcion: item.descripcion, ...camposUbicacion(item, distritos),
        fecha: item.fecha.slice(0, 16), cupos: item.cupos, requisitos: item.requisitos,
      });
      this.portadaPrevia.set(this.api.portadaUrl(item));
      this.listo.set(true);
    });
  }

  seleccionarPortada(input: HTMLInputElement): void {
    const archivo = input.files?.[0];
    input.value = '';
    if (!archivo) return;
    this.errorPortada.set('');
    if (!['image/jpeg', 'image/png'].includes(archivo.type) || !archivo.size || archivo.size > 3 * 1024 * 1024) {
      this.errorPortada.set('Selecciona una imagen JPG o PNG de hasta 3 MB.');
      return;
    }

    const lector = new FileReader();
    this.lectorPortada = lector;
    this.preparandoPortada.set(true);
    lector.onload = () => {
      const datos = String(lector.result);
      this.cambioPortada = datos.slice(datos.indexOf(',') + 1);
      this.portadaPrevia.set(datos);
    };
    lector.onerror = () => this.errorPortada.set('No se pudo leer la foto. Vuelve a seleccionarla.');
    lector.onloadend = () => {
      if (this.lectorPortada !== lector) return;
      this.lectorPortada = undefined;
      this.preparandoPortada.set(false);
    };
    lector.readAsDataURL(archivo);
  }

  quitarPortada(): void {
    this.cambioPortada = '';
    this.portadaPrevia.set(null);
    this.errorPortada.set('');
  }

  guardar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.operacion.cargando() || this.preparandoPortada() || this.errorPortada()) return;
    const id = this.id();
    const datos = this.form.getRawValue();
    const cambios = {
      ...datos, titulo: datos.titulo.trim(), descripcion: datos.descripcion.trim(),
      direccion: datos.direccion.trim(), requisitos: datos.requisitos.trim(), portadaBase64: this.cambioPortada,
    };
    if (id === null && new Date(datos.fecha).getTime() <= Date.now()) {
      this.form.controls.fecha.setErrors({ fechaPasada: true });
      return;
    }
    const resultado = id === null ? this.api.publicar(cambios) : this.api.editarCampania(id, cambios);
    this.operacion.ejecutar(resultado, () => {
      this.snack.open(id === null ? 'Campaña publicada.' : 'Cambios guardados.', 'Cerrar', { duration: 4000 });
      void this.router.navigate(['/gestion/campanias']);
    });
  }
}
