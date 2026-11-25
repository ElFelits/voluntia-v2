package com.voluntared.voluntia.repository;

import com.voluntared.voluntia.entity.ReporteImpacto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReporteImpactoRepository extends JpaRepository<ReporteImpacto, Integer> {
    Optional<ReporteImpacto> findByCampaniaId(Integer id);
}
