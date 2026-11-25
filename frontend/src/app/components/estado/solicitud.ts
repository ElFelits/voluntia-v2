import { DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, Observable, Subscription } from 'rxjs';
import { mensajeError } from '../../services/voluntia.service';

export class Solicitud {
  readonly cargando = signal(false);
  readonly error = signal('');
  private destroyRef = inject(DestroyRef);
  private pendiente?: Subscription;

  ejecutar<T>(resultado: Observable<T>, completar: (valor: T) => void): void {
    this.pendiente?.unsubscribe();
    this.error.set('');
    this.cargando.set(true);
    this.pendiente = resultado.pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.cargando.set(false)),
    ).subscribe({
      next: completar,
      error: error => this.error.set(mensajeError(error)),
    });
  }
}
