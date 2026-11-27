import { DatePipe } from '@angular/common';
import { afterRenderEffect, Component, computed, DestroyRef, ElementRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { FiltrosChat, RespuestaChat } from '../../models/voluntia';
import { mensajeError, SesionService, VoluntiaService } from '../../services/voluntia.service';

type Mensaje = { pregunta: string } | { respuesta: RespuestaChat };

@Component({
  imports: [DatePipe, MatButtonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './chat.html',
  styleUrl: './chat.css',
})
export class Chat {
  private api = inject(VoluntiaService);
  private sesion = inject(SesionService);
  private destroyRef = inject(DestroyRef);
  private conversacion = viewChild<ElementRef<HTMLDivElement>>('conversacion');
  private ultimaBusqueda: FiltrosChat | null = null;

  readonly pregunta = new FormControl('', { nonNullable: true });
  readonly respondiendo = signal(false);
  readonly mensajes = signal<Mensaje[]>([
    { respuesta: {
      texto: 'Hola. Puedo ayudarte a encontrar campañas, consultar tus inscripciones y resolver dudas sobre cómo participar.',
      campanias: [], destino: null, filtros: null,
    } },
  ]);
  readonly enlaces = {
    CATALOGO: { ruta: '/campanias', etiqueta: 'Ver catálogo' },
    PARTICIPACIONES: { ruta: '/mis-participaciones', etiqueta: 'Mis participaciones' },
    PERFIL: { ruta: '/mi-cuenta', etiqueta: 'Mi cuenta' },
  };
  readonly sugerencias = computed(() => [
    'Próximas campañas', 'Campañas ambientales', 'Mis inscripciones', '¿Cómo me inscribo?',
    ...(this.sesion.cuenta()?.ubicacion?.trim() ? ['Campañas cerca de mí'] : []),
  ]);

  constructor() {
    afterRenderEffect({
      earlyRead: () => {
        this.mensajes();
        this.respondiendo();
        const panel = this.conversacion()?.nativeElement;
        return panel ? { panel, altura: panel.scrollHeight } : null;
      },
      write: lectura => {
        const resultado = lectura();
        if (resultado) resultado.panel.scrollTop = resultado.altura;
      },
    });
  }

  enviar(texto = this.pregunta.value): void {
    const consulta = texto.trim().slice(0, 240);
    if (!consulta || this.respondiendo()) return;

    this.pregunta.reset();
    this.mensajes.update(mensajes => [...mensajes, { pregunta: consulta }]);
    this.respondiendo.set(true);
    this.api.preguntarChat(consulta, this.ultimaBusqueda).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.respondiendo.set(false)),
    ).subscribe({
      next: respuesta => {
        if (respuesta.filtros) this.ultimaBusqueda = respuesta.filtros;
        this.mensajes.update(mensajes => [...mensajes, { respuesta }]);
      },
      error: error => this.mensajes.update(mensajes => [...mensajes, {
        respuesta: { texto: mensajeError(error), campanias: [], destino: null, filtros: null },
      }]),
    });
  }
}
