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
type VistaGrafico = 'campanias' | 'mensual' | 'distribucion';
const vistas: { clave: VistaGrafico; etiqueta: string }[] = [
  { clave: 'campanias', etiqueta: 'Barras por campaña' },
  { clave: 'mensual', etiqueta: 'Barras por mes' },
  { clave: 'distribucion', etiqueta: 'Distribución porcentual' },
];
const colores = ['#316345', '#2c617b', '#a36225', '#79548b', '#65813b', '#586561'];
const formatoMes = new Intl.DateTimeFormat('es', { month: 'short', year: 'numeric', timeZone: 'UTC' });
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
  readonly vistas = vistas;
  readonly form = inject(FormBuilder).nonNullable.group({
    periodo: [0], organizacion: [0], campania: [0], metrica: ['horas' as Metrica],
    vista: ['campanias' as VistaGrafico],
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
      const fecha = item.fecha ? new Date(item.fecha).getTime() : NaN;
      return (!campania || item.campaniaId === campania)
        && (!periodo || (fecha >= inicio.getTime() && fecha <= Date.now()));
    });
  });
  readonly resumen = computed(() => this.metricas.map(metrica => ({
    ...metrica, valor: this.filtradas().reduce((suma, item) => suma + item[metrica.clave], 0),
  })));
  readonly vista = computed(() => this.filtros().vista);
  private filasCampanias = computed(() => {
    const { metrica } = this.filtros();
    return this.filtradas().map(item => ({ id: item.campaniaId, titulo: item.titulo, valor: item[metrica] }))
      .sort((a, b) => b.valor - a.valor);
  });
  readonly barras = computed(() => {
    const { vista, metrica } = this.filtros();
    let filas: { id: string | number; titulo: string; valor: number }[] = this.filasCampanias();
    if (vista === 'mensual') {
      const meses = new Map<string, number>();
      for (const item of this.filtradas()) {
        if (!item.fecha) continue;
        const mes = item.fecha.slice(0, 7);
        meses.set(mes, (meses.get(mes) ?? 0) + item[metrica]);
      }
      filas = Array.from(meses).sort(([a], [b]) => a.localeCompare(b))
        .map(([id, valor]) => ({ id, titulo: formatoMes.format(new Date(id + '-01T00:00:00Z')), valor }));
    }
    const maximo = filas.reduce((max, fila) => Math.max(max, fila.valor), 0);
    return filas.map(fila => ({ ...fila, porcentaje: maximo ? (fila.valor / maximo) * 100 : 0 }));
  });
  readonly distribucion = computed(() => {
    const positivas = this.filasCampanias().filter(fila => fila.valor > 0);
    const total = positivas.reduce((suma, fila) => suma + fila.valor, 0);
    const filas = positivas.slice(0, 5);
    if (positivas.length > 5) {
      const valor = positivas.slice(5).reduce((suma, fila) => suma + fila.valor, 0);
      filas.push({ id: -1, titulo: 'Otras campañas', valor });
    }
    let acumulado = 0;
    const partes = filas.map((fila, i) => {
      const proporcion = fila.valor / total;
      const inicio = acumulado;
      acumulado = i === filas.length - 1 ? 100 : inicio + proporcion * 100;
      return { ...fila, proporcion, color: colores[i], tramo: `${colores[i]} ${inicio}% ${acumulado}%` };
    });
    return { total, partes, fondo: `conic-gradient(${partes.map(parte => parte.tramo).join(', ')})` };
  });
  readonly etiquetaGrafico = computed(() => this.metricas.find(item => item.clave === this.filtros().metrica)?.etiqueta);

  constructor() {
    this.form.controls.organizacion.valueChanges.pipe(takeUntilDestroyed())
      .subscribe(() => this.form.controls.campania.setValue(0));
    this.cargar();
  }

  cargar(): void { this.consulta.ejecutar(this.api.impacto(), items => this.items.set(items)); }
}
