package com.voluntared.voluntia.controller;

import com.voluntared.voluntia.dto.*;
import com.voluntared.voluntia.dto.ApiDTO.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import com.voluntared.voluntia.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** API del panel de gestión. Usa la misma sesión y protección CSRF que los formularios. */
@RestController
@RequestMapping("/api")
public class ApiController {
    private final CuentaService cuentas;
    private final CampaniaService campanias;
    private final InscripcionService inscripciones;
    private final IntegracionAngularService angular;
    private final SesionApiService sesion;
    private final AccesoService acceso;

    public ApiController(CuentaService cuentas, CampaniaService campanias, InscripcionService inscripciones,
                         IntegracionAngularService angular, SesionApiService sesion, AccesoService acceso) {
        this.cuentas = cuentas;
        this.campanias = campanias;
        this.inscripciones = inscripciones;
        this.angular = angular;
        this.sesion = sesion;
        this.acceso = acceso;
    }
    @GetMapping("/voluntarios") public List<CuentaDTO> voluntarios() {
        return cuentas.listarVoluntarios().stream().map(v->cuentas.buscar(v.getId(),false)).toList();
    }
    @GetMapping("/organizaciones") public List<CuentaDTO> organizaciones() {
        return cuentas.listarOrganizaciones().stream().map(o->cuentas.buscar(o.getId(),true)).toList();
    }
    @GetMapping("/voluntarios/{id}") public CuentaDTO voluntario(@PathVariable Integer id) {return cuentas.buscar(id,false);}
    @GetMapping("/organizaciones/{id}") public OrganizacionPublica organizacion(@PathVariable Integer id) {return angular.ficha(id);}
    @PostMapping("/voluntarios") @ResponseStatus(HttpStatus.CREATED)
    public CuentaDTO crearVoluntario(@Valid @RequestBody CuentaDTO dto) {dto.setId(null);return cuentas.buscar(cuentas.guardar(dto,false),false);}
    @PostMapping("/organizaciones") @ResponseStatus(HttpStatus.CREATED)
    public CuentaDTO crearOrganizacion(@Valid @RequestBody CuentaDTO dto) {dto.setId(null);return cuentas.buscar(cuentas.guardar(dto,true),true);}
    @PutMapping("/voluntarios/{id}") public CuentaDTO actualizarVoluntario(@PathVariable Integer id,@Valid @RequestBody CuentaDTO dto) {
        dto.setId(id);cuentas.guardar(dto,false);return cuentas.buscar(id,false);
    }
    @PutMapping("/organizaciones/{id}") public CuentaDTO actualizarOrganizacion(@PathVariable Integer id,@Valid @RequestBody CuentaDTO dto) {
        dto.setId(id);cuentas.guardar(dto,true);return cuentas.buscar(id,true);
    }
    @DeleteMapping("/voluntarios/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarVoluntario(@PathVariable Integer id) {cuentas.eliminar(id,false);}
    @DeleteMapping("/organizaciones/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarOrganizacion(@PathVariable Integer id) {cuentas.eliminar(id,true);}

    @GetMapping("/auth/sesion")
    public Object sesion(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return valorJSON(sesion.sesion());
    }

    @PostMapping("/auth/ingresar")
    public CuentaDTO ingresar(@Valid @RequestBody Credenciales dto, HttpServletRequest request,
                              HttpServletResponse response) {
        return sesion.ingresar(dto.email(), dto.contrasena(), request, response);
    }

    @PostMapping("/auth/registro") @ResponseStatus(HttpStatus.CREATED)
    public CuentaDTO registrar(@Valid @RequestBody Registro dto, HttpServletRequest request,
                               HttpServletResponse response) {
        if (!Set.of("VOLUNTARIO", "ORGANIZACION").contains(dto.rol()))
            throw new IllegalArgumentException("Selecciona un perfil válido");
        cuentas.registrar(dto.perfil(), "ORGANIZACION".equals(dto.rol()));
        return sesion.ingresar(dto.email(), dto.contrasena(), request, response);
    }

    @PostMapping("/auth/salir") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void salir(HttpServletRequest request, HttpServletResponse response) {
        sesion.salir(request, response);
    }

    @PutMapping("/mi-cuenta")
    public CuentaDTO actualizarPerfil(@Valid @RequestBody CuentaDTO dto) {
        dto.setContrasena(null);
        cuentas.actualizarMiCuenta(dto);
        return cuentas.miCuenta();
    }

@GetMapping("/campanias")
    public List<CampaniaVista> listarCampanias(@RequestParam(required=false) String q,
            @RequestParam(required=false) String ubicacion,
            @RequestParam(required=false) String distrito,
            @RequestParam(required=false) String categoria,
            @RequestParam(defaultValue="false") boolean gestion, Authentication actor) {
        if (gestion && (actor == null || actor instanceof AnonymousAuthenticationToken))
            throw new AuthenticationCredentialsNotFoundException("Inicia sesión para continuar");
        String filtroUbicacion = (distrito != null && !distrito.isBlank()) ? distrito : ubicacion;
        return angular.listar(q, filtroUbicacion, categoria, gestion);
    }

    @GetMapping("/campanias/{id}")
    public CampaniaVista campania(@PathVariable Integer id) { return angular.campania(id); }

    @PostMapping("/campanias") @ResponseStatus(HttpStatus.CREATED)
    public CampaniaVista crearCampania(@Valid @RequestBody CampaniaDTO dto) {
        dto.setId(null);
        dto.setOrganizacionId(null);
        return angular.campania(campanias.guardar(dto));
    }

    @PutMapping("/campanias/{id}")
    public CampaniaVista actualizarCampania(@PathVariable Integer id, @Valid @RequestBody CampaniaDTO dto) {
        dto.setId(id);
        dto.setOrganizacionId(null);
        return angular.campania(campanias.guardar(dto));
    }

    @DeleteMapping("/campanias/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarCampania(@PathVariable Integer id) { campanias.cancelar(id); }

    @GetMapping("/organizaciones/{id}/campanias")
    public List<CampaniaVista> campaniasOrganizacion(@PathVariable Integer id) {
        return angular.deOrganizacion(id);
    }

    @GetMapping("/distritos")
    public List<String> distritos() {
        return List.of(
            "Ancón", "Ate", "Barranco", "Breña", "Carabayllo", "Chaclacayo", "Chorrillos",
            "Cieneguilla", "Comas", "El Agustino", "Independencia", "Jesús María",
            "La Molina", "La Victoria", "Lima", "Lince", "Los Olivos", "Lurigancho",
            "Lurín", "Magdalena del Mar", "Miraflores", "Pachacámac", "Pucusana",
            "Pueblo Libre", "Puente Piedra", "Punta Hermosa", "Punta Negra", "Rímac",
            "San Bartolo", "San Borja", "San Isidro", "San Juan de Lurigancho",
            "San Juan de Miraflores", "San Luis", "San Martín de Porres", "San Miguel",
            "Santa Anita", "Santa María del Mar", "Santa Rosa", "Santiago de Surco",
            "Surquillo", "Villa El Salvador", "Villa María del Triunfo"
        );
    }

    @GetMapping("/voluntarios/{id}/historial")
    public List<Participacion> historial(@PathVariable Integer id) {
        cuentas.voluntario(id);
        return angular.historial(id);
    }

    @GetMapping("/mis-participaciones")
    public List<Participacion> misParticipaciones() { return angular.historial(acceso.voluntarioId()); }

    @GetMapping("/csrf")
    public Map<String,String> csrf(org.springframework.security.web.csrf.CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @GetMapping("/campanias/{id}/mi-inscripcion")
    public Object miInscripcion(@PathVariable Integer id) {
        campanias.buscar(id);
        return valorJSON(inscripciones.miInscripcion(id).map(angular::vista).orElse(null));
    }

    @PostMapping("/campanias/{id}/mi-inscripcion") @ResponseStatus(HttpStatus.CREATED)
    public InscripcionVista unirme(@PathVariable Integer id) { return angular.vista(inscripciones.unirme(id)); }

    @DeleteMapping("/campanias/{id}/mi-inscripcion") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void salir(@PathVariable Integer id) { inscripciones.salir(id); }

    @GetMapping("/campanias/{id}/participantes")
    public List<InscripcionVista> participantes(@PathVariable Integer id) {
        return inscripciones.participantes(id).stream().map(angular::vista).toList();
    }

    @PutMapping("/campanias/{id}/participantes/{inscripcionId}")
    public InscripcionVista marcarAsistencia(@PathVariable Integer id, @PathVariable Integer inscripcionId) {
        return angular.vista(inscripciones.actualizarParticipante(id, inscripcionId, true));
    }

    @DeleteMapping("/campanias/{id}/participantes/{inscripcionId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirar(@PathVariable Integer id, @PathVariable Integer inscripcionId) {
        inscripciones.actualizarParticipante(id, inscripcionId, false);
    }

    @GetMapping("/impacto")
    public List<Impacto> impacto() { return angular.impacto(); }

    @GetMapping("/campanias/{id}/impacto")
    public Resultado resultado(@PathVariable Integer id) { return angular.resultado(id); }

    @PutMapping("/campanias/{id}/impacto")
    public Resultado guardarResultado(@PathVariable Integer id, @Valid @RequestBody Resultado resultado) {
        return angular.registrarResultado(id, resultado);
    }
    
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body, Authentication actor) {
        String mensaje = body.get("mensaje") == null ? "" : body.get("mensaje").toString().trim();
        String norm = normalizarChat(mensaje);
        Map<?, ?> anterior = body.get("anterior") instanceof Map<?, ?> m ? m : Map.of();
        boolean autenticado = actor != null && !(actor instanceof AnonymousAuthenticationToken);

        if (norm.contains("como me inscribo") || norm.contains("como participar") || norm.contains("inscribirme") || norm.equals("hola")) {
            return respuestaChat(
                "Para inscribirte: 1) Inicia sesión con tu cuenta de voluntario. 2) Entra a Explorar campañas y abre la actividad que te interese. 3) Pulsa el botón Inscribirme si aún hay cupos disponibles.",
                List.of(), "CATALOGO", null
            );
        }

        if (norm.contains("inscripcion") || norm.contains("participacion") || norm.contains("inscrito")) {
            if (!autenticado) {
                return respuestaChat(
                    "Inicia sesión con tu cuenta de voluntario para revisar tus inscripciones activas y tu historial.",
                    List.of(), null, null
                );
            }
            if (!acceso.rol("VOLUNTARIO")) {
                return respuestaChat(
                    "Tu cuenta tiene perfil de organización o administrador. Puedes revisar los participantes desde la gestión de cada campaña.",
                    List.of(), "CATALOGO", null
                );
            }
            var lista = angular.historial(acceso.voluntarioId());
            if (lista.isEmpty()) {
                return respuestaChat(
                    "Aún no tienes inscripciones registradas. Explora las campañas próximas para unirte a una.",
                    List.of(), "CATALOGO", null
                );
            }
            String resumen = lista.stream()
                .map(p -> "• " + p.campania().titulo() + " (" + p.estado() + ")")
                .collect(java.util.stream.Collectors.joining("\n"));
            return respuestaChat(
                "Tienes " + lista.size() + " participación(es) registrada(s):\n" + resumen,
                List.of(), "PARTICIPACIONES", null
            );
        }

        String categoria = norm.contains("ambiental") ? "Ambiental"
                : (norm.contains("social") ? "Social" : null);

        String distrito = null;
        if (norm.contains("cerca de mi") || norm.contains("mi distrito") || norm.contains("mi zona")) {
            if (!autenticado) {
                return respuestaChat(
                    "Inicia sesión y guarda tu distrito en Mi cuenta para mostrarte las campañas cerca de ti.",
                    List.of(), "PERFIL", null
                );
            }
            String miUbicacion = cuentas.miCuenta().getUbicacion();
            if (miUbicacion == null || miUbicacion.isBlank()) {
                return respuestaChat(
                    "Aún no tienes un distrito configurado en tu perfil. Actualízalo en Mi cuenta para buscar cerca de ti.",
                    List.of(), "PERFIL", null
                );
            }
            distrito = miUbicacion;
        } else {
            for (String d : distritos()) {
                if (norm.contains(normalizarChat(d))) {
                    distrito = d;
                    break;
                }
            }
        }

        if (categoria == null && distrito != null && anterior.get("categoria") instanceof String catAnt && !catAnt.isBlank()) {
            categoria = catAnt;
        }
        if (distrito == null && categoria != null && anterior.get("distrito") instanceof String distAnt && !distAnt.isBlank()) {
            distrito = distAnt;
        }

        boolean pideCampanias = categoria != null || distrito != null
                || norm.contains("campana") || norm.contains("proxima") || norm.contains("disponible");

        String q = pideCampanias ? null : mensaje;
        List<CampaniaVista> encontradas = angular.listar(q, distrito, categoria, false).stream().limit(5).toList();

        Map<String, String> filtros = new LinkedHashMap<>();
        if (categoria != null) filtros.put("categoria", categoria);
        if (distrito != null) filtros.put("distrito", distrito);
        if (q != null && !q.isBlank()) filtros.put("q", q);

        if (encontradas.isEmpty()) {
            return respuestaChat(
                "No encontré campañas próximas con ese criterio. Puedes probar con otra categoría (Ambiental o Social) o revisar todo el catálogo.",
                List.of(), "CATALOGO", filtros.isEmpty() ? null : filtros
            );
        }

        String detalle = (categoria != null ? " de categoría " + categoria : "")
                + (distrito != null ? " en " + distrito : "");
        return respuestaChat(
            "Encontré " + encontradas.size() + " campaña(s) próxima(s)" + detalle + ":",
            encontradas, "CATALOGO", filtros.isEmpty() ? null : filtros
        );
    }

    private Map<String, Object> respuestaChat(String texto, List<CampaniaVista> lista, String destino, Map<String, String> filtros) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("texto", texto);
        res.put("campanias", lista == null ? List.of() : lista);
        res.put("destino", destino);
        res.put("filtros", filtros);
        return res;
    }

    private static String normalizarChat(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private Object valorJSON(Object valor) {
        return valor == null ? tools.jackson.databind.node.NullNode.getInstance() : valor;
    }
}
