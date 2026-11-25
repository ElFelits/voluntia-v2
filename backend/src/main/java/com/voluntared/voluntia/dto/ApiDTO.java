package com.voluntared.voluntia.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiDTO {
    private ApiDTO() {}

    public record Credenciales(@NotBlank @Email @Size(max = 100) String email,
                               @NotBlank @Size(max = 72) String contrasena) {}
    public record Registro(@NotBlank String rol, @NotBlank @Size(max=100) String nombre,
                           @NotBlank @Email @Size(max=100) String email,
                           @NotBlank @Size(min=8,max=72) String contrasena,
                           @NotBlank @Size(max=255) String ubicacion, @Min(16) @Max(120) Integer edad,
                           @Size(max=255) String habilidades, @Size(max=100) String ruc,
                           @Size(max=200) String infoContacto) {
        public CuentaDTO perfil() {
            var cuenta = new CuentaDTO();
            cuenta.setNombre(nombre);
            cuenta.setEmail(email);
            cuenta.setContrasena(contrasena);
            cuenta.setUbicacion(ubicacion);
            cuenta.setEdad(edad);
            cuenta.setHabilidades(habilidades);
            cuenta.setRuc(ruc);
            cuenta.setInfoContacto(infoContacto);
            return cuenta;
        }
    }
    public record CampaniaVista(Integer id, Integer organizacionId, String organizacion, String titulo,
                                String descripcion, String ubicacion, LocalDateTime fecha, String requisitos,
                                int cupos, long disponibles, String categoria, String estado) {}
    public record OrganizacionPublica(Integer id, String nombre, String ubicacion, String ruc, String infoContacto) {}
    public record InscripcionVista(Integer id, Integer voluntarioId, String voluntario, Integer campaniaId, String estado) {}
    public record Participacion(Integer id, Integer voluntarioId, String voluntario, Integer campaniaId,
                                String estado, CampaniaVista campania) {}
    public record Resultado(@NotNull @DecimalMin("0") @DecimalMax("100000000") BigDecimal horas,
                            @NotNull @Min(0) @Max(100000000) Integer arboles,
                            @NotNull @DecimalMin("0") @DecimalMax("100000000") BigDecimal residuosKg,
                            @NotNull @DecimalMin("0") @DecimalMax("100000000") BigDecimal materialesKg) {
        public static final Resultado CERO = new Resultado(BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO);
    }
    public record Impacto(Integer campaniaId, String titulo, Integer organizacionId, String organizacion,
                          LocalDateTime fecha, long asistentes, BigDecimal horas, Integer arboles,
                          BigDecimal residuosKg, BigDecimal materialesKg) {}
}
