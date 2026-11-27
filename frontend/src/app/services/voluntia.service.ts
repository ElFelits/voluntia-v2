import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, map, of, shareReplay, tap } from 'rxjs';
import {
  Campania, Cuenta, Credenciales, DatosCampania, FiltrosCampania, Impacto, Inscripcion,
  FiltrosChat, OrganizacionPublica, Participacion, Perfil, Registro, RespuestaChat, Resultado,
} from '../models/voluntia';
import { configuracion } from './configuracion';

export function mensajeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return 'No pudimos conectar con el servicio. Intenta de nuevo.';
    if (typeof error.error?.mensaje === 'string') return error.error.mensaje;
  }
  return 'No pudimos completar la operación. Intenta de nuevo.';
}

@Injectable({ providedIn: 'root' })
export class VoluntiaService {
  private http = inject(HttpClient);
  private base = configuracion.apiUrl.replace(/\/$/, '');
  private opciones = { withCredentials: true };
  readonly distritos = this.http.get<readonly string[]>(this.base + '/distritos').pipe(shareReplay(1));

  listarCampanias(filtros?: FiltrosCampania, gestion = false) {
    let params = new HttpParams();
    if (gestion) params = params.set('gestion', 'true');
    for (const [campo, valor] of Object.entries(filtros ?? {})) {
      const texto = valor?.trim();
      if (texto) params = params.set(campo, texto);
    }
    return this.http.get<Campania[]>(this.base + '/campanias', { ...this.opciones, params });
  }

  campania(id: number) { return this.http.get<Campania>(this.rutaCampania(id), this.opciones); }
  portadaUrl(item: Campania): string | null {
    return item.portadaId ? this.rutaCampania(item.id) + '/portada/' + item.portadaId : null;
  }
  publicar(datos: DatosCampania) { return this.http.post<Campania>(this.base + '/campanias', datos, this.opciones); }
  editarCampania(id: number, datos: DatosCampania) { return this.http.put<Campania>(this.rutaCampania(id), datos, this.opciones); }
  cancelarCampania(id: number) { return this.http.delete<void>(this.rutaCampania(id), this.opciones); }

  organizacion(id: number) { return this.http.get<OrganizacionPublica>(this.base + '/organizaciones/' + id, this.opciones); }
  campaniasOrganizacion(id: number) { return this.http.get<Campania[]>(this.base + '/organizaciones/' + id + '/campanias', this.opciones); }
  cuentas(rol: 'voluntarios' | 'organizaciones') { return this.http.get<Cuenta[]>(this.base + '/' + rol, this.opciones); }
  editarCuenta(rol: 'voluntarios' | 'organizaciones', id: number, datos: Perfil) {
    return this.http.put<Cuenta>(this.base + '/' + rol + '/' + id, datos, this.opciones);
  }
  bajaCuenta(rol: 'voluntarios' | 'organizaciones', id: number) {
    return this.http.delete<void>(this.base + '/' + rol + '/' + id, this.opciones);
  }

  misParticipaciones() { return this.http.get<Participacion[]>(this.base + '/mis-participaciones', this.opciones); }
  preguntarChat(mensaje: string, anterior: FiltrosChat | null) {
    return this.http.post<RespuestaChat>(this.base + '/chat', { mensaje, anterior }, this.opciones);
  }
  miInscripcion(id: number) { return this.http.get<Inscripcion | null>(this.rutaCampania(id) + '/mi-inscripcion', this.opciones); }
  inscribir(id: number) { return this.http.post<Inscripcion>(this.rutaCampania(id) + '/mi-inscripcion', {}, this.opciones); }
  cancelarInscripcion(id: number) { return this.http.delete<void>(this.rutaCampania(id) + '/mi-inscripcion', this.opciones); }
  participantes(id: number) { return this.http.get<Inscripcion[]>(this.rutaCampania(id) + '/participantes', this.opciones); }
  marcarAsistencia(id: number, participante: number) {
    return this.http.put<Inscripcion>(this.rutaCampania(id) + '/participantes/' + participante, {}, this.opciones);
  }
  retirarParticipante(id: number, participante: number) {
    return this.http.delete<void>(this.rutaCampania(id) + '/participantes/' + participante, this.opciones);
  }

  impacto() { return this.http.get<Impacto[]>(this.base + '/impacto', this.opciones); }
  resultado(id: number) { return this.http.get<Resultado>(this.rutaCampania(id) + '/impacto', this.opciones); }
  guardarResultado(id: number, resultado: Resultado) { return this.http.put<Resultado>(this.rutaCampania(id) + '/impacto', resultado, this.opciones); }

  private rutaCampania(id: number) { return this.base + '/campanias/' + id; }
}

@Injectable({ providedIn: 'root' })
export class SesionService {
  private http = inject(HttpClient);
  private base = configuracion.apiUrl.replace(/\/$/, '');
  private opciones = { withCredentials: true };
  private estado = signal<Cuenta | null | undefined>(undefined);
  readonly cuenta = computed(() => this.estado() ?? null);
  private carga$ = this.http.get<Cuenta | null>(this.base + '/auth/sesion', this.opciones).pipe(
    tap(cuenta => {
      if (this.estado() === undefined) this.estado.set(cuenta);
    }),
    map(() => this.cuenta()),
    shareReplay(1),
    catchError(() => of(null)),
  );

  restaurar() { return this.estado() === undefined ? this.carga$ : of(this.cuenta()); }

  limpiar() { this.estado.set(null); }

  ingresar(credenciales: Credenciales) {
    return this.http.post<Cuenta>(this.base + '/auth/ingresar', credenciales, this.opciones)
      .pipe(tap(cuenta => this.estado.set(cuenta)));
  }

  registrar(datos: Registro) {
    return this.http.post<Cuenta>(this.base + '/auth/registro', datos, this.opciones)
      .pipe(tap(cuenta => this.estado.set(cuenta)));
  }

  salir() {
    return this.http.post(this.base + '/auth/salir', {}, this.opciones)
      .pipe(tap(() => this.estado.set(null)), map(() => undefined));
  }

  actualizar(datos: Perfil) {
    return this.http.put<Cuenta>(this.base + '/mi-cuenta', datos, this.opciones)
      .pipe(tap(cuenta => this.estado.set(cuenta)));
  }
}
