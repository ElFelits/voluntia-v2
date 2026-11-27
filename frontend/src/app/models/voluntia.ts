export type Rol = 'VOLUNTARIO' | 'ORGANIZACION' | 'ADMIN';
export type Categoria = 'Ambiental' | 'Social';
export const categorias: Categoria[] = ['Ambiental', 'Social'];

export interface Cuenta {
  id: number;
  rol: Rol;
  nombre: string;
  email: string;
  ubicacion: string;
  direccion?: string | null;
  edad?: number;
  habilidades?: string;
  ruc?: string;
  infoContacto?: string;
}

export type Perfil = Omit<Cuenta, 'id' | 'rol'>;
export type Registro = Perfil & { rol: 'VOLUNTARIO' | 'ORGANIZACION'; contrasena: string };
export interface Credenciales { email: string; contrasena: string }
export type OrganizacionPublica = Pick<Cuenta, 'id' | 'nombre' | 'ubicacion' | 'direccion' | 'ruc' | 'infoContacto'>;

export interface Campania {
  id: number;
  organizacionId: number | null;
  organizacion: string;
  titulo: string;
  descripcion: string;
  ubicacion: string;
  direccion?: string | null;
  fecha: string;
  requisitos: string;
  cupos: number;
  disponibles: number;
  categoria: Categoria;
  estado: 'ACTIVA' | 'CANCELADA';
  portadaId?: number | null;
}

export type DatosCampania = Pick<Campania, 'titulo' | 'descripcion' | 'ubicacion' | 'direccion' | 'fecha' | 'requisitos' | 'cupos' | 'categoria'>
  & { portadaBase64?: string };
export type FiltrosCampania = { q: string; ubicacion?: string; distrito?: string; categoria: Categoria | '' };

export interface FiltrosChat {
  q: string | null;
  categoria: Categoria | null;
  distrito: string | null;
}

export interface RespuestaChat {
  texto: string;
  campanias: Campania[];
  destino: 'CATALOGO' | 'PARTICIPACIONES' | 'PERFIL' | null;
  filtros: FiltrosChat | null;
}

export interface Inscripcion {
  id: number;
  voluntarioId: number;
  voluntario: string;
  campaniaId: number;
  estado: 'INSCRITO' | 'ASISTIO' | 'CANCELADO';
}
export interface Participacion extends Inscripcion { campania: Campania }

export interface Resultado {
  horas: number;
  arboles: number;
  residuosKg: number;
  materialesKg: number;
}
export interface Impacto extends Resultado {
  campaniaId: number;
  titulo: string;
  organizacionId: number | null;
  organizacion: string;
  fecha: string | null;
  asistentes: number;
}

export const etiquetasRol: Record<Rol, string> = {
  VOLUNTARIO: 'Voluntario',
  ORGANIZACION: 'Organización',
  ADMIN: 'Administración',
};
