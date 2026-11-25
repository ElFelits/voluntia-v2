package com.voluntared.voluntia.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.*;
import java.util.*;
import com.voluntared.voluntia.entity.*;
import com.voluntared.voluntia.repository.*;
@Service @Transactional
public class InscripcionService {
 private final InscripcionRepository repository; private final CampaniaService campanias;
 private final VoluntarioRepository voluntarios; private final EntityManager em; private final AccesoService acceso;
 public InscripcionService(InscripcionRepository r,CampaniaService c,VoluntarioRepository v,EntityManager e,AccesoService a){repository=r;campanias=c;voluntarios=v;em=e;acceso=a;}
 public List<Inscripcion> historial(Integer id){acceso.exigirCuenta(id,false);return repository.findByVoluntarioId(id);}
 public List<Inscripcion> participantes(Integer id){acceso.exigirGestion(campanias.buscar(id));return repository.findByCampaniaId(id).stream().filter(i -> !"CANCELADO".equals(i.getEstado())).toList();}
 public long cantidad(Integer id){return campanias.ocupados(id);}
 public Optional<Inscripcion> miInscripcion(Integer id){return repository.findByVoluntarioIdAndCampaniaId(acceso.voluntarioId(),id);}
 private Campania bloquear(Integer id){return campanias.bloquear(id);}
 public void inscribir(Integer campaniaId,Integer voluntarioId){acceso.exigirAdmin();inscribirInterno(campaniaId,voluntarioId);}
 public Inscripcion unirme(Integer campaniaId){return inscribirInterno(campaniaId,acceso.voluntarioId());}

 private Inscripcion inscribirInterno(Integer campaniaId,Integer voluntarioId) {
  Voluntario voluntario = voluntarios.findById(voluntarioId)
          .orElseThrow(() -> new IllegalArgumentException("Voluntario no encontrado"));
  em.refresh(voluntario.getUsuario(), LockModeType.PESSIMISTIC_WRITE);
  if (!voluntario.getUsuario().isActivo()) throw new org.springframework.security.access.AccessDeniedException("Cuenta dada de baja");
  Campania c = bloquear(campaniaId);
  if ("CANCELADA".equals(c.getEstado()) || c.getFecha()==null
          || !c.getFecha().isAfter(java.time.LocalDateTime.now())
          || c.getOrganizacion()==null || !c.getOrganizacion().getUsuario().isActivo())
      throw new ConflictoVoluntiaException("La campaña ya no admite inscripciones");
  Inscripcion i = repository.findByVoluntarioIdAndCampaniaId(voluntarioId,campaniaId).orElseGet(Inscripcion::new);
  if (i.getId()!=null && !"CANCELADO".equals(i.getEstado()))
      throw new ConflictoVoluntiaException("Ya tienes una inscripción en esta campaña");
  if (c.getCupos()==null || cantidad(campaniaId)>=c.getCupos()) throw new ConflictoVoluntiaException("No quedan cupos disponibles");
  i.setCampania(c);
  i.setVoluntario(voluntario);
  i.setEstado("INSCRITO");
  return repository.saveAndFlush(i);
 }
 public void salir(Integer campaniaId) {
  Integer vid=acceso.voluntarioId();
  bloquear(campaniaId);
  Inscripcion i = repository.findByVoluntarioIdAndCampaniaId(vid,campaniaId)
          .orElseThrow(() -> new ConflictoVoluntiaException("No hay una inscripción activa que cancelar"));
  cambiarPendiente(i, "CANCELADO");
 }
 public Inscripcion actualizarParticipante(Integer campaniaId, Integer inscripcionId, boolean asistencia) {
  Campania c = bloquear(campaniaId);
  acceso.exigirGestion(c);
  if ("CANCELADA".equals(c.getEstado())) throw new ConflictoVoluntiaException("La campaña está cancelada");
  Inscripcion i = repository.findByIdAndCampaniaId(inscripcionId, campaniaId)
          .filter(inscripcion -> !"CANCELADO".equals(inscripcion.getEstado()))
          .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));
  cambiarPendiente(i, asistencia ? "ASISTIO" : "CANCELADO");
  return i;
 }
 private void cambiarPendiente(Inscripcion i, String estado) {
  if (!"INSCRITO".equals(i.getEstado())) throw new ConflictoVoluntiaException("No hay una inscripción pendiente");
  i.setEstado(estado);
 }
 public void retirar(Integer campaniaId,Integer voluntarioId){
  Campania c=bloquear(campaniaId);acceso.exigirGestion(c);
  Inscripcion i=repository.findByVoluntarioIdAndCampaniaId(voluntarioId,campaniaId).orElseThrow(()->new IllegalArgumentException("Inscripción no encontrada"));
  repository.delete(i);repository.flush();
 }
 private void eliminarInscripcion(Inscripcion i){
  if("ASISTIO".equals(i.getEstado()))throw new IllegalArgumentException("La asistencia registrada se conserva en el historial");
  repository.delete(i);repository.flush();
 }
 private Inscripcion buscar(Integer id){return repository.findById(id).orElseThrow(()->new IllegalArgumentException("Inscripción no encontrada"));}
 public void cancelar(Integer id){Inscripcion i=buscar(id);acceso.exigirGestion(i.getCampania());Campania c=bloquear(i.getCampania().getId());acceso.exigirGestion(c);eliminarInscripcion(i);}
 public void asistencia(Integer id){Inscripcion i=buscar(id);acceso.exigirGestion(i.getCampania());Campania c=bloquear(i.getCampania().getId());acceso.exigirGestion(c);if (!"CANCELADA".equals(c.getEstado())) cambiarPendiente(i, "ASISTIO");}
}
