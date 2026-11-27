import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Campania } from '../../models/voluntia';
import { VoluntiaService } from '../../services/voluntia.service';

@Component({
  selector: 'app-tarjeta-campania',
  imports: [DatePipe, RouterLink],
  templateUrl: './tarjeta-campania.html',
})
export class TarjetaCampania {
  campania = input.required<Campania>();
  private api = inject(VoluntiaService);
  readonly portada = computed(() => this.api.portadaUrl(this.campania()));
  readonly portadaFallida = signal<string | null>(null);
}
