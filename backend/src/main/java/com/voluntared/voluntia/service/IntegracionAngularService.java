package com.voluntared.voluntia.service;

import com.voluntared.voluntia.dto.ApiDTO.*;
import com.voluntared.voluntia.entity.*;
import com.voluntared.voluntia.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class IntegracionAngularService {
    private final CampaniaService campanias;
    private final CampaniaRepository catalogo;
    private final OrganizacionRepository organizaciones;
    private final InscripcionRepository inscripciones;
    private final ReporteImpactoRepository reportes;
    private final AccesoService acceso;

    public IntegracionAngularService(CampaniaService campanias, CampaniaRepository catalogo,
            OrganizacionRepository organizaciones, InscripcionRepository inscripciones,
            ReporteImpactoRepository reportes, AccesoService acceso) {
        this.campanias = campanias;
        this.catalogo = catalogo;
        this.organizaciones = organizaciones;
        this.inscripciones = inscripciones;
        this.reportes = reportes;
        this.acceso = acceso;
    }

    public List<CampaniaVista> listar(String q, String ubicacion, String categoria, boolean gestion) {
        if (categoria != null && !Set.of("Ambiental", "Social").contains(categoria))
            throw new IllegalArgumentException("Selecciona una categoría válida");
        var lista = gestion ? gestionables() : catalogo.findAll();
        var ahora = LocalDateTime.now();
        return lista.stream().filter(c -> gestion || proxima(c, ahora))
                .filter(c -> categoria == null || categoria.equals(c.getCategoria()))
                .filter(c -> texto(c.getUbicacion()).contains(texto(ubicacion)))
                .filter(c -> texto(c.getTitulo() + " " + limpio(c.getDescripcion()) + " " + organizacion(c)).contains(texto(q)))
                .sorted(Comparator.comparing(Campania::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Campania::getId))
                .map(this::vista).toList();
    }

    private List<Campania> gestionables() {
        if (acceso.admin()) return catalogo.findAll();
        if (acceso.rol("ORGANIZACION")) return catalogo.findByOrganizacionId(acceso.organizacionId());
        throw new AccessDeniedException("Tu perfil no puede gestionar campañas");
    }

    public CampaniaVista campania(Integer id) { return vista(campanias.buscar(id)); }

    public OrganizacionPublica ficha(Integer id) {
        var org = organizaciones.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organización no encontrada"));
        var usuario = org.getUsuario();
        return new OrganizacionPublica(id, usuario.getNombre(), limpio(usuario.getUbicacion()),
                usuario.isActivo() ? org.getRuc() : null, usuario.isActivo() ? org.getInfoContacto() : null);
    }

    public List<CampaniaVista> deOrganizacion(Integer id) {
        ficha(id);
        var ahora = LocalDateTime.now();
        return catalogo.findByOrganizacionId(id).stream().filter(c -> proxima(c, ahora))
                .sorted(Comparator.comparing(Campania::getFecha).thenComparing(Campania::getId))
                .map(this::vista).toList();
    }

    private boolean proxima(Campania c, LocalDateTime ahora) {
        return "ACTIVA".equals(c.getEstado()) && c.getFecha() != null && c.getFecha().isAfter(ahora)
                && c.getOrganizacion() != null && c.getOrganizacion().getUsuario().isActivo();
    }

    public CampaniaVista vista(Campania c) {
        int cupos = c.getCupos() == null ? 0 : c.getCupos();
        return new CampaniaVista(c.getId(), c.getOrganizacion() == null ? null : c.getOrganizacion().getId(),
                organizacion(c), c.getTitulo(), limpio(c.getDescripcion()), limpio(c.getUbicacion()),
                c.getFecha(), limpio(c.getRequisitos()), cupos, Math.max(0, cupos - campanias.ocupados(c.getId())),
                c.getCategoria(), c.getEstado());
    }

    private String organizacion(Campania c) {
        return c.getOrganizacion() == null ? "Sin organización" : c.getOrganizacion().getUsuario().getNombre();
    }

    public InscripcionVista vista(Inscripcion i) {
        var voluntario = i.getVoluntario();
        return new InscripcionVista(i.getId(), voluntario.getId(),
                voluntario.getUsuario().isActivo() ? voluntario.getUsuario().getNombre() : "Voluntario dado de baja",
                i.getCampania().getId(), i.getEstado());
    }

    public List<Participacion> historial(Integer voluntarioId) {
        acceso.exigirCuenta(voluntarioId, false);
        return inscripciones.findByVoluntarioId(voluntarioId).stream().map(i -> {
            var datos = vista(i);
            return new Participacion(datos.id(), datos.voluntarioId(), datos.voluntario(), datos.campaniaId(),
                    datos.estado(), vista(i.getCampania()));
        }).toList();
    }

    public Resultado resultado(Integer id) {
        acceso.exigirGestion(campanias.buscar(id));
        return resultadoGuardado(id);
    }

    private Resultado resultadoGuardado(Integer id) {
        return reportes.findByCampaniaId(id).map(r -> new Resultado(cero(r.getHorasVoluntariado()),
                r.getArbolesPlantados() == null ? 0 : r.getArbolesPlantados(),
                cero(r.getResiduosColectadosKg()), cero(r.getMaterialesKg()))).orElse(Resultado.CERO);
    }

    public Resultado registrarResultado(Integer id, Resultado resultado) {
        var campania = campanias.bloquear(id);
        acceso.exigirGestion(campania);
        if ("CANCELADA".equals(campania.getEstado())) throw new ConflictoVoluntiaException("La campaña está cancelada");
        var reporte = reportes.findByCampaniaId(id).orElseGet(ReporteImpacto::new);
        reporte.setCampania(campania);
        reporte.setHorasVoluntariado(resultado.horas());
        reporte.setArbolesPlantados(resultado.arboles());
        reporte.setResiduosColectadosKg(resultado.residuosKg());
        reporte.setMaterialesKg(resultado.materialesKg());
        reportes.save(reporte);
        return resultado;
    }

    public List<Impacto> impacto() {
        return gestionables().stream().map(c -> {
            var r = resultadoGuardado(c.getId());
            return new Impacto(c.getId(), c.getTitulo(), c.getOrganizacion() == null ? null : c.getOrganizacion().getId(),
                    organizacion(c), c.getFecha(), inscripciones.countByCampaniaIdAndEstado(c.getId(), "ASISTIO"),
                    r.horas(), r.arboles(), r.residuosKg(), r.materialesKg());
        }).toList();
    }

    private static String texto(String valor) {
        return Normalizer.normalize(limpio(valor).strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }
    private static String limpio(String valor) { return valor == null ? "" : valor; }
    private static BigDecimal cero(BigDecimal valor) { return valor == null ? BigDecimal.ZERO : valor; }
}
