package com.voluntared.voluntia.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.voluntared.voluntia.entity.Organizacion;
public interface OrganizacionRepository extends JpaRepository<Organizacion,Integer> {
    long countByUsuarioActivoTrue();
    java.util.Optional<Organizacion> findByUsuarioId(Integer usuarioId);
}
