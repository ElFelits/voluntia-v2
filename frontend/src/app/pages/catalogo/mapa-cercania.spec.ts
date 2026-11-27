import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Campania } from '../../models/voluntia';
import { SesionService } from '../../services/voluntia.service';
import { MapaCercania } from './mapa-cercania';

describe('MapaCercania', () => {
  let fixture: ComponentFixture<MapaCercania>;

  const campania = (id: number, ubicacion: string, disponibles = 1): Campania => ({
    id, ubicacion, disponibles, organizacionId: 1, organizacion: 'Voluntia',
    titulo: 'Campaña ' + id, descripcion: '', requisitos: '', fecha: '2099-10-01T10:00:00',
    cupos: 10, categoria: 'Social', estado: 'ACTIVA',
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MapaCercania],
      providers: [
        provideRouter([]),
        { provide: SesionService, useValue: { cuenta: signal({ ubicacion: 'Miraflores' }) } },
      ],
    });
    fixture = TestBed.createComponent(MapaCercania);
    fixture.componentRef.setInput('campanias', [
      campania(1, 'Ancón'),
      campania(2, 'Miraflores', 0),
      campania(3, 'Barranco'),
      campania(4, 'Ubicación sin distrito'),
    ]);
    fixture.detectChanges();
  });

  it('usa el distrito del perfil y elige la más cercana entre las que tienen cupos', () => {
    const pagina: HTMLElement = fixture.nativeElement;
    expect(pagina.querySelector('select')?.value).toBe('Miraflores');
    expect(pagina.querySelector('.mapa-resultado a')?.getAttribute('href')).toBe('/campanias/3');
    expect(fixture.componentInstance.cercana()?.km).toBeGreaterThan(0);
  });

  it('recalcula al cambiar el distrito y al actualizar las campañas filtradas', () => {
    fixture.componentInstance.elegirDistrito('Ancón');
    expect(fixture.componentInstance.cercana()?.campania.id).toBe(1);
    expect(fixture.componentInstance.textoDistancia()).toBe('En tu mismo distrito');
    fixture.componentRef.setInput('campanias', [campania(3, 'Barranco')]);
    fixture.detectChanges();
    expect(fixture.componentInstance.cercana()?.campania.id).toBe(3);
  });

  it('usa las coordenadas del navegador para actualizar el resultado y el mapa', () => {
    spyOn(navigator.geolocation, 'getCurrentPosition').and.callFake(exito => {
      exito({
        timestamp: 0,
        coords: {
          longitude: -77.175, latitude: -11.771, accuracy: 12,
          altitude: null, altitudeAccuracy: null, heading: null, speed: null,
          toJSON: () => ({}),
        },
        toJSON: () => ({}),
      });
    });
    fixture.componentInstance.usarUbicacion();
    fixture.detectChanges();

    const canvas = (fixture.nativeElement as HTMLElement).querySelector('canvas')!;
    expect(fixture.componentInstance.origen()?.distrito).toBeUndefined();
    expect(fixture.componentInstance.cercana()?.campania.id).toBe(1);
    expect(canvas.getContext('2d')?.getImageData(450, 235, 1, 1).data[3]).toBe(255);
  });

  it('permite elegir manualmente el distrito si falla la ubicación del dispositivo', () => {
    spyOn(navigator.geolocation, 'getCurrentPosition').and.callFake((_exito, error) => {
      error?.({ code: 1, message: '', PERMISSION_DENIED: 1, POSITION_UNAVAILABLE: 2, TIMEOUT: 3 });
    });
    fixture.componentInstance.usarUbicacion();
    expect(fixture.componentInstance.errorUbicacion()).toContain('elegir');
    fixture.componentInstance.elegirDistrito('Barranco');
    expect(fixture.componentInstance.errorUbicacion()).toBe('');
    expect(fixture.componentInstance.textoDistancia()).toBe('En tu mismo distrito');
  });
});
