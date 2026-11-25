import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { Rol } from '../models/voluntia';
import { SesionService } from '../services/voluntia.service';

export const acceso = (...roles: Rol[]): CanActivateFn => (_route, state) => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  return sesion.restaurar().pipe(map(cuenta => {
    if (!cuenta) return router.createUrlTree(['/ingresar'], { queryParams: { volver: state.url } });
    return !roles.length || roles.includes(cuenta.rol) || router.createUrlTree(['/sin-acceso']);
  }));
};
