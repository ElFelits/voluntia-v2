import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Campania } from '../../models/voluntia';

@Component({
  selector: 'app-tarjeta-campania',
  imports: [DatePipe, RouterLink],
  templateUrl: './tarjeta-campania.html',
})
export class TarjetaCampania {
  campania = input.required<Campania>();
}
