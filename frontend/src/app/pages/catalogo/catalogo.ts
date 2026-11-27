import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { switchMap } from 'rxjs';
import { Estado } from '../../components/estado/estado';
import { Solicitud } from '../../components/estado/solicitud';
import { TarjetaCampania } from '../../components/tarjeta-campania/tarjeta-campania';
import { Campania, categorias, FiltrosCampania } from '../../models/voluntia';
import { buscarDistrito } from '../../models/ubicacion';
import { VoluntiaService } from '../../services/voluntia.service';
import { MapaCercania } from './mapa-cercania';

@Component({
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, Estado, TarjetaCampania, MapaCercania],
  templateUrl: './catalogo.html',
  styleUrl: './catalogo.css',
})
export class Catalogo {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(VoluntiaService);
  private fb = inject(FormBuilder);
  readonly inicio = this.route.snapshot.routeConfig?.path === '';
  readonly categorias = categorias;
  readonly distritos = signal<readonly string[]>([]);
  readonly ubicacionAnterior = signal('');
  readonly categoria = signal<FiltrosCampania['categoria']>('');
  readonly vista = signal<'lista' | 'mapa'>('lista');
  readonly campanias = signal<Campania[]>([]);
  readonly visibles = computed(() => this.inicio ? this.campanias().slice(0, 3) : this.campanias());
  readonly solicitud = new Solicitud();
  readonly filtros = this.fb.nonNullable.group({ q: '', ubicacion: '' });

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(params => {
      const categoria = params.get('categoria');
      this.filtros.setValue({ q: params.get('q') ?? '', ubicacion: params.get('ubicacion') ?? '' });
      this.categoria.set(categoria === 'Ambiental' || categoria === 'Social' ? categoria : '');
      this.cargar();
    });
  }

  cargar(): void {
    this.solicitud.ejecutar(
      this.api.distritos.pipe(switchMap(distritos => {
        this.distritos.set(distritos);
        const ubicacion = this.route.snapshot.queryParamMap.get('ubicacion') ?? '';
        const distrito = buscarDistrito(distritos, ubicacion);
        this.ubicacionAnterior.set(distrito ? '' : ubicacion);
        this.filtros.controls.ubicacion.setValue(distrito || ubicacion);
        const { q } = this.filtros.getRawValue();
        return this.api.listarCampanias({ q, distrito, ubicacion: distrito ? '' : ubicacion, categoria: this.categoria() });
      })),
      campanias => this.campanias.set(campanias),
    );
  }

  buscar(categoria = this.categoria()): void {
    const { q, ubicacion } = this.filtros.getRawValue();
    void this.router.navigate(['/campanias'], {
      queryParams: { q: q.trim() || null, ubicacion: ubicacion.trim() || null, categoria: categoria || null },
    });
  }

  limpiar(): void {
    this.filtros.reset();
    this.buscar('');
  }
}
