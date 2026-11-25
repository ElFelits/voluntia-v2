import { Routes } from '@angular/router';
import { acceso } from './guards/acceso';

export const routes: Routes = [
  { path: '', pathMatch: 'full', title: 'Voluntia | Haz la diferencia cerca de ti', loadComponent: () => import('./pages/catalogo/catalogo').then(m => m.Catalogo) },
  { path: 'campanias', pathMatch: 'full', title: 'Campañas | Voluntia', loadComponent: () => import('./pages/catalogo/catalogo').then(m => m.Catalogo) },
  { path: 'campanias/:id', title: 'Detalle de campaña | Voluntia', loadComponent: () => import('./pages/detalle/detalle').then(m => m.Detalle) },
  { path: 'organizaciones/:id', title: 'Organización | Voluntia', loadComponent: () => import('./pages/detalle/organizacion').then(m => m.Organizacion) },
  { path: 'ingresar', title: 'Iniciar sesión | Voluntia', loadComponent: () => import('./pages/cuenta/ingresar').then(m => m.Ingresar) },
  { path: 'registro', title: 'Crear cuenta | Voluntia', loadComponent: () => import('./pages/cuenta/perfil').then(m => m.PerfilPagina), data: { modo: 'registro' } },
  { path: 'mi-cuenta', title: 'Mi cuenta | Voluntia', canActivate: [acceso('VOLUNTARIO', 'ORGANIZACION')], loadComponent: () => import('./pages/cuenta/perfil').then(m => m.PerfilPagina) },
  { path: 'mis-participaciones', title: 'Mis participaciones | Voluntia', canActivate: [acceso('VOLUNTARIO')], loadComponent: () => import('./pages/voluntario/participaciones').then(m => m.Participaciones) },
  { path: 'chat', title: 'Asistente Voluntia', canActivate: [acceso('VOLUNTARIO')], loadComponent: () => import('./pages/voluntario/chat').then(m => m.Chat) },
  {
    path: 'gestion', canActivate: [acceso('ORGANIZACION', 'ADMIN')], children: [
      { path: '', pathMatch: 'full', redirectTo: 'campanias' },
      { path: 'campanias', title: 'Gestionar campañas | Voluntia', loadComponent: () => import('./pages/gestion/campanias').then(m => m.GestionCampanias) },
      { path: 'campanias/nueva', title: 'Publicar campaña | Voluntia', canActivate: [acceso('ORGANIZACION')], loadComponent: () => import('./pages/gestion/form-campania').then(m => m.FormCampania) },
      { path: 'campanias/:id/editar', title: 'Editar campaña | Voluntia', loadComponent: () => import('./pages/gestion/form-campania').then(m => m.FormCampania) },
      { path: 'campanias/:id/participantes', title: 'Participantes | Voluntia', loadComponent: () => import('./pages/gestion/participantes').then(m => m.Participantes) },
      { path: 'campanias/:id/impacto', title: 'Registrar impacto | Voluntia', loadComponent: () => import('./pages/gestion/form-impacto').then(m => m.FormImpacto) },
      { path: 'impacto', title: 'Impacto | Voluntia', loadComponent: () => import('./pages/gestion/impacto').then(m => m.ImpactoPagina) },
      { path: 'cuentas', title: 'Gestionar cuentas | Voluntia', canActivate: [acceso('ADMIN')], loadComponent: () => import('./pages/gestion/cuentas').then(m => m.GestionCuentas) },
      { path: 'cuentas/:tipo/:id/editar', title: 'Editar cuenta | Voluntia', canActivate: [acceso('ADMIN')], loadComponent: () => import('./pages/cuenta/perfil').then(m => m.PerfilPagina), data: { modo: 'admin' } },
    ],
  },
  { path: 'sin-acceso', title: 'Acceso restringido | Voluntia', loadComponent: () => import('./pages/mensaje/mensaje').then(m => m.MensajePagina), data: { restringido: true } },
  { path: '**', title: 'Página desconocida | Voluntia', loadComponent: () => import('./pages/mensaje/mensaje').then(m => m.MensajePagina) },
];
