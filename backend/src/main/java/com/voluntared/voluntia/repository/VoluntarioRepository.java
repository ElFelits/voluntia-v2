package com.voluntared.voluntia.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.voluntared.voluntia.entity.Voluntario;
public interface VoluntarioRepository extends JpaRepository<Voluntario,Integer> {
    long countByUsuarioActivoTrue();
    java.util.Optional<Voluntario> findByUsuarioId(Integer usuarioId);
}
