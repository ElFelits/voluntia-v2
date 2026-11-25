import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { map, startWith } from 'rxjs';
import { Estado } from '../../components/estado/estado';
import { Solicitud } from '../../components/estado/solicitud';
import { Impacto } from '../../models/voluntia';
import { SesionService, VoluntiaService } from '../../services/voluntia.service';

type Metrica = 'asistentes' | 'horas' | 'arboles' | 'residuosKg' | 'materialesKg';
const metricas: { clave: Metrica; etiqueta: string; icono: string }[] = [
  { clave: 'asistentes', etiqueta: 'Asistentes', icono: 'group' },
  { clave: 'horas', etiqueta: 'Horas de voluntariado', icono: 'schedule' },
  { clave: 'arboles', etiqueta: 'Árboles plantados', icono: 'park' },
  { clave: 'residuosKg', etiqueta: 'Residuos recolectados (kg)', icono: 'delete_outline' },
  { clave: 'materialesKg', etiqueta: 'Materiales recuperados (kg)', icono: 'recycling' },
];

@Component({
  imports: [DecimalPipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatSelectModule, RouterLink, Estado],
  templateUrl: './impacto.html',
})
export class ImpactoPagina {
  private api = inject(VoluntiaService);
  readonly admin = inject(SesionService).cuenta()?.rol === 'ADMIN';
  readonly consulta = new Solicitud();
  readonly items = signal<Impacto[]>([]);
  readonly metricas = metricas;
  readonly form = inject(FormBuilder).nonNullable.group({
    periodo: [0], organizacion: [0], campania: [0], metrica: ['horas' as Metrica],
  });
  private filtros = toSignal(this.form.valueChanges.pipe(
    map(() => this.form.getRawValue()), startWith(this.form.getRawValue()),
  ), { requireSync: true });
  readonly organizaciones = computed(() => Array.from(new Map(this.items().map(item =>
    [item.organizacionId, item.organizacion] as const)).entries()));
  readonly campanias = computed(() => {
    const id = this.filtros().organizacion;
    return this.items().filter(item => !id || item.organizacionId === id);
  });
  readonly filtradas = computed(() => {
    const { periodo, campania } = this.filtros();
    const inicio = new Date();
    inicio.setHours(0, 0, 0, 0);
    inicio.setDate(inicio.getDate() - periodo);
    return this.campanias().filter(item => {
      const fecha = new Date(item.fecha).getTime();
      return (!campania || item.campaniaId === campania)
        && (!periodo || (fecha >= inicio.getTime() && fecha <= Date.now()));
    });
  });
  readonly resumen = computed(() => this.metricas.map(metrica => ({
    ...metrica, valor: this.filtradas().reduce((suma, item) => suma + item[metrica.clave], 0),
  })));
  readonly grafico = computed(() => {
    const { metrica } = this.filtros();
    const filas = this.filtradas().map(item => ({ id: item.campaniaId, titulo: item.titulo, valor: item[metrica] }))
      .sort((a, b) => b.valor - a.valor);
    const maximo = Math.max(1, filas[0]?.valor ?? 0);
    return filas.map(fila => ({ ...fila, porcentaje: (fila.valor / maximo) * 100 }));
  });
  readonly etiquetaGrafico = computed(() => this.metricas.find(item => item.clave === this.filtros().metrica)?.etiqueta);

  constructor() {
    this.form.controls.organizacion.valueChanges.pipe(takeUntilDestroyed())
      .subscribe(() => this.form.controls.campania.setValue(0));
    this.cargar();
  }

  cargar(): void { this.consulta.ejecutar(this.api.impacto(), items => this.items.set(items)); }
}
