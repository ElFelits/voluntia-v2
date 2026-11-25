import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface ConfirmacionDatos { titulo: string; mensaje: string; accion: string }

@Component({
  selector: 'app-confirmacion',
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>{{ datos.titulo }}</h2>
    <mat-dialog-content>{{ datos.mensaje }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false" cdkFocusInitial>Volver</button>
      <button mat-flat-button type="button" [mat-dialog-close]="true">{{ datos.accion }}</button>
    </mat-dialog-actions>
  `,
})
export class Confirmacion {
  readonly datos = inject<ConfirmacionDatos>(MAT_DIALOG_DATA);
}
