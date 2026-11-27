import { afterRenderEffect, Component, computed, DestroyRef, ElementRef, inject, input, signal, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { buscarDistrito } from '../../models/ubicacion';
import { Campania } from '../../models/voluntia';
import { SesionService } from '../../services/voluntia.service';
import { planoLima, Punto } from './plano-lima';

type Origen = { punto: Punto; distrito?: string };
type CampaniaCercana = { campania: Campania; distrito: string; punto: Punto; km: number };

const distritos = Object.keys(planoLima);
const limites = Object.values(planoLima).flatMap(distrito => distrito?.anillos ?? []);
const kilometros = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 1 });

function distanciaKm([lon1, lat1]: Punto, [lon2, lat2]: Punto): number {
  const rad = Math.PI / 180;
  const a = Math.sin((lat2 - lat1) * rad / 2) ** 2
    + Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin((lon2 - lon1) * rad / 2) ** 2;
  return 12742 * Math.asin(Math.sqrt(Math.min(1, a)));
}

@Component({
  selector: 'app-mapa-cercania',
  imports: [MatButtonModule, RouterLink],
  templateUrl: './mapa-cercania.html',
  styleUrl: './mapa-cercania.css',
})
export class MapaCercania {
  private sesion = inject(SesionService);
  private destroyRef = inject(DestroyRef);
  private lienzo = viewChild<ElementRef<HTMLCanvasElement>>('lienzo');
  private origenElegido = signal<Origen | null>(null);
  private solicitud = 0;

  readonly campanias = input.required<readonly Campania[]>();
  readonly distritos = distritos;
  readonly localizando = signal(false);
  readonly errorUbicacion = signal('');
  readonly origen = computed(() => {
    const distrito = buscarDistrito(distritos, this.sesion.cuenta()?.ubicacion);
    const punto = planoLima[distrito]?.centro;
    return this.origenElegido() ?? (punto ? { distrito, punto } : null);
  });
  private disponibles = computed(() => this.campanias().flatMap(campania => {
    if (campania.disponibles <= 0) return [];
    const distrito = buscarDistrito(distritos, campania.ubicacion);
    const centro = planoLima[distrito]?.centro;
    return centro ? [{ campania, distrito, punto: centro }] : [];
  }));
  readonly hayCampanias = computed(() => this.disponibles().length > 0);
  readonly cercana = computed(() => {
    const origen = this.origen();
    if (!origen) return null;

    let cercana: CampaniaCercana | null = null;
    for (const candidata of this.disponibles()) {
      const km = distanciaKm(origen.punto, candidata.punto);
      if (!cercana || km < cercana.km
          || (km === cercana.km && candidata.campania.fecha < cercana.campania.fecha)) {
        cercana = { ...candidata, km };
      }
    }
    return cercana;
  });
  readonly textoDistancia = computed(() => {
    const cercana = this.cercana();
    if (!cercana) return '';
    if (this.origen()?.distrito === cercana.distrito) return 'En tu mismo distrito';
    if (cercana.km < 0.1) return 'A menos de 0,1 km aprox.';
    return 'A ' + kilometros.format(cercana.km) + ' km aprox.';
  });

  constructor() {
    afterRenderEffect({
      write: () => {
        const canvas = this.lienzo()?.nativeElement;
        const origen = this.origen()?.punto;
        const cercana = this.cercana();
        if (canvas) dibujarMapa(canvas, origen, cercana);
      },
    });
  }

  elegirDistrito(distrito: string): void {
    const punto = planoLima[distrito]?.centro;
    if (!punto) return;
    this.solicitud++;
    this.localizando.set(false);
    this.errorUbicacion.set('');
    this.origenElegido.set({ distrito, punto });
  }

  usarUbicacion(): void {
    if (!navigator.geolocation) {
      this.errorUbicacion.set('La ubicación del dispositivo no está disponible. Elige tu distrito.');
      return;
    }
    const solicitud = ++this.solicitud;
    this.localizando.set(true);
    this.errorUbicacion.set('');
    navigator.geolocation.getCurrentPosition(({ coords }) => {
      if (this.destroyRef.destroyed || solicitud !== this.solicitud) return;
      this.origenElegido.set({ punto: [coords.longitude, coords.latitude] });
      this.localizando.set(false);
    }, () => {
      if (this.destroyRef.destroyed || solicitud !== this.solicitud) return;
      this.localizando.set(false);
      this.errorUbicacion.set('No pudimos obtener tu ubicación. Puedes elegir tu distrito.');
    }, { timeout: 8000, maximumAge: 60000 });
  }
}

function dibujarMapa(lienzo: HTMLCanvasElement, origen: Punto | undefined, cercana: CampaniaCercana | null): void {
  const ctx = lienzo.getContext('2d');
  if (!ctx) return;
  const { width, height } = lienzo;
  const destino = cercana?.punto;
  const encuadre = origen && destino ? [origen, destino] : limites.flat().concat(origen ? [origen] : []);
  const longitudes = encuadre.map(([lon]) => lon);
  const latitudes = encuadre.map(([, lat]) => lat);
  const oeste = Math.min(...longitudes);
  const este = Math.max(...longitudes);
  const sur = Math.min(...latitudes);
  const norte = Math.max(...latitudes);
  const lonMedia = (este + oeste) / 2;
  const latMedia = (norte + sur) / 2;
  const ajustarLon = Math.max(0.1, Math.cos(latMedia * Math.PI / 180));
  const escala = Math.min((width - 120) / Math.max((este - oeste) * ajustarLon, 0.09),
    (height - 84) / Math.max(norte - sur, 0.09));
  const proyectar = ([lon, lat]: Punto): [number, number] =>
    [width / 2 + (lon - lonMedia) * ajustarLon * escala, height / 2 - (lat - latMedia) * escala];
  ctx.fillStyle = '#e3eff1';
  ctx.fillRect(0, 0, width, height);
  ctx.lineWidth = 1.4;
  ctx.strokeStyle = '#a1b5a5';
  for (const [nombre, distrito] of Object.entries(planoLima)) {
    if (!distrito) continue;
    ctx.beginPath();
    for (const anillo of distrito.anillos) {
      const [primero, ...resto] = anillo;
      ctx.moveTo(...proyectar(primero));
      for (const punto of resto) ctx.lineTo(...proyectar(punto));
      ctx.closePath();
    }
    ctx.fillStyle = nombre === cercana?.distrito ? '#bcd9b8' : '#f4f7ee';
    ctx.fill('evenodd');
    ctx.stroke();
  }

  const marcar = (punto: Punto, color: string, radio: number) => {
    const [x, y] = proyectar(punto);
    ctx.beginPath();
    ctx.arc(x, y, radio, 0, 2 * Math.PI);
    ctx.fillStyle = color;
    ctx.fill();
    ctx.lineWidth = 3;
    ctx.strokeStyle = '#fff';
    ctx.stroke();
  };
  if (destino) marcar(destino, '#276238', 16);
  if (origen) marcar(origen, '#216aa0', 8);
}
