import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, finalize, Observable, shareReplay, switchMap, throwError } from 'rxjs';
import { configuracion } from '../services/configuracion';
import { SesionService } from '../services/voluntia.service';

interface Csrf {
  headerName: string;
  token: string;
}

let consultaCsrf: Observable<Csrf> | undefined;

export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  const base = configuracion.apiUrl.replace(/\/$/, '');
  if (!req.url.startsWith(base + '/')) return next(req);

  const http = inject(HttpClient);
  const sesion = inject(SesionService);
  const lectura = ['GET', 'HEAD', 'OPTIONS'].includes(req.method);
  let respuesta = lectura ? next(req) : undefined;
  if (!respuesta) {
    consultaCsrf ??= http.get<Csrf>(base + '/csrf', { withCredentials: true }).pipe(
      finalize(() => consultaCsrf = undefined),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
    respuesta = consultaCsrf.pipe(
      switchMap(({ headerName, token }) => next(req.clone({ setHeaders: { [headerName]: token } }))),
    );
  }

  return respuesta.pipe(catchError(error => {
    if (error instanceof HttpErrorResponse && error.status === 401 && !req.url.startsWith(base + '/auth/')) {
      sesion.limpiar();
    }
    return throwError(() => error);
  }));
};
