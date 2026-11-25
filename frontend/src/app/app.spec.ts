import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { configuracion } from './services/configuracion';

describe('Voluntia', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('muestra la navegación pública y el acceso', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    TestBed.inject(HttpTestingController).expectOne(configuracion.apiUrl + '/auth/sesion').flush(null);
    const pagina = fixture.nativeElement as HTMLElement;
    expect(pagina.querySelector('header .marca')?.textContent).toContain('Voluntia');
    expect(pagina.querySelector('a[href="/ingresar"]')?.textContent).toContain('Iniciar sesión');
  });
});
