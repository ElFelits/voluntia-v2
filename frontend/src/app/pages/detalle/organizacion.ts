import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Estado } from '../../components/estado/estado';
import { Solicitud } from '../../components/estado/solicitud';
import { TarjetaCampania } from '../../components/tarjeta-campania/tarjeta-campania';
import { Campania, OrganizacionPublica } from '../../models/voluntia';
import { VoluntiaService } from '../../services/voluntia.service';

@Component({
  imports: [Estado, RouterLink, TarjetaCampania],
  template: `
    <section class="contenedor pagina">
      <a class="volver" routerLink="/campanias"><span class="icon" aria-hidden="true">arrow_back</span> Campañas</a>
      <app-estado [cargando]="consulta.cargando()" [error]="consulta.error()" (reintentar)="cargar()" />
      @if (organizacion(); as item) {
        <div class="pagina-cabecera">
          <h1>{{ item.nombre }}</h1>
          <p class="meta"><span class="icon" aria-hidden="true">location_on</span> {{ item.direccion ? item.direccion + ', ' : '' }}{{ item.ubicacion }}</p>
        </div>
        <div class="panel ficha">
          @if (item.ruc) { <p><strong>RUC</strong> {{ item.ruc }}</p> }
          @if (item.infoContacto) { <p><strong>Contacto</strong> {{ item.infoContacto }}</p> }
        </div>
        <h2>Próximas campañas</h2>
        @if (campanias().length) {
          <div class="rejilla-campanias">
            @for (campania of campanias(); track campania.id) { <app-tarjeta-campania [campania]="campania" /> }
          </div>
        } @else {
          <app-estado vacio="Esta organización aún no tiene próximas campañas." />
        }
      }
    </section>
  `,
})
export class Organizacion {
  private api = inject(VoluntiaService);
  readonly consulta = new Solicitud();
  readonly organizacion = signal<OrganizacionPublica | null>(null);
  readonly campanias = signal<Campania[]>([]);
  private id = 0;

  constructor() {
    inject(ActivatedRoute).paramMap.pipe(takeUntilDestroyed()).subscribe(params => {
      this.id = Number(params.get('id'));
      this.organizacion.set(null);
      this.cargar();
    });
  }

  cargar(): void {
    this.consulta.ejecutar(forkJoin({
      organizacion: this.api.organizacion(this.id), campanias: this.api.campaniasOrganizacion(this.id),
    }), ({ organizacion, campanias }) => {
      this.organizacion.set(organizacion);
      this.campanias.set(campanias);
    });
  }
}
