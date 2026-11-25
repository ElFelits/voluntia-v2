package com.voluntared.voluntia.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "reporte_impacto")
@Getter @Setter
public class ReporteImpacto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reporte_impacto_id")
    private Integer id;
    @OneToOne @JoinColumn(name = "campania_id", nullable = false, unique = true)
    private Campania campania;
    private Integer arbolesPlantados;
    @Column(columnDefinition = "numeric")
    private BigDecimal residuosColectadosKg;
    @Column(columnDefinition = "numeric")
    private BigDecimal horasVoluntariado;
    @Column(nullable = false, columnDefinition = "numeric")
    private BigDecimal materialesKg;
}
