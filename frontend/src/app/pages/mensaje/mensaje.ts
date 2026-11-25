import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  imports: [MatButtonModule, RouterLink],
  template: `
    <section class="contenedor pagina">
      <div class="panel mensaje-pagina">
        <span class="icon" aria-hidden="true">{{ restringido ? 'lock' : 'search_off' }}</span>
        <h1>{{ restringido ? 'Acceso restringido' : 'Página no encontrada' }}</h1>
        <p>{{ restringido ? 'Tu perfil no tiene acceso a esta sección.' : 'Revisa la dirección o vuelve al catálogo de campañas.' }}</p>
        <a mat-flat-button routerLink="/campanias">Explorar campañas</a>
      </div>
    </section>
  `,
})
export class MensajePagina {
  readonly restringido = inject(ActivatedRoute).snapshot.data['restringido'] === true;
}
