package com.voluntared.voluntia.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.voluntared.voluntia.entity.Inscripcion;
public interface InscripcionRepository extends JpaRepository<Inscripcion,Integer> {
    java.util.List<Inscripcion> findByVoluntarioId(Integer id);
    java.util.List<Inscripcion> findByCampaniaId(Integer id);
    long countByCampaniaId(Integer id);
    long countByCampaniaIdAndEstadoIn(Integer id, java.util.Collection<String> estados);
    long countByCampaniaIdAndEstado(Integer id, String estado);
    java.util.Optional<Inscripcion> findByIdAndCampaniaId(Integer id, Integer campaniaId);
    boolean existsByVoluntarioIdAndCampaniaId(Integer voluntarioId,Integer campaniaId);
    java.util.Optional<Inscripcion> findByVoluntarioIdAndCampaniaId(Integer voluntarioId,Integer campaniaId);
}
