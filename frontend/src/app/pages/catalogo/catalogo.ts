import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { Estado } from '../../components/estado/estado';
import { Solicitud } from '../../components/estado/solicitud';
import { TarjetaCampania } from '../../components/tarjeta-campania/tarjeta-campania';
import { Campania, categorias, FiltrosCampania } from '../../models/voluntia';
import { VoluntiaService } from '../../services/voluntia.service';

@Component({
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, Estado, TarjetaCampania],
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
      this.api.listarCampanias({ ...this.filtros.getRawValue(), categoria: this.categoria() }),
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
