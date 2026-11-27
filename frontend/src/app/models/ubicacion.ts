import type { Cuenta } from './voluntia';

export function buscarDistrito(distritos: readonly string[], ubicacion: string | null | undefined): string {
  const normalizar = (texto: string) => texto.trim().toLowerCase().normalize('NFD').replace(/\p{M}/gu, '');
  const nombre = normalizar(ubicacion ?? '');
  return distritos.find(distrito => normalizar(distrito) === (nombre === 'cercado de lima' ? 'lima' : nombre)) ?? '';
}

export function camposUbicacion(datos: Pick<Cuenta, 'ubicacion' | 'direccion'>, distritos: readonly string[]) {
  const ubicacion = buscarDistrito(distritos, datos.ubicacion);
  return {
    ubicacion,
    direccion: datos.direccion ?? (ubicacion ? '' : datos.ubicacion ?? ''),
  };
}
