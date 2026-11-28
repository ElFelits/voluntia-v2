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

    @GetMapping("/voluntarios")
    public List<CuentaDTO> voluntarios() {
        return cuentas.listarVoluntarios().stream().map(v -> cuentas.buscar(v.getId(), false)).toList();
    }

    @GetMapping("/organizaciones")
    public List<CuentaDTO> organizaciones() {
        return cuentas.listarOrganizaciones().stream().map(o -> cuentas.buscar(o.getId(), true)).toList();
    }

    @GetMapping("/voluntarios/{id}")
    public CuentaDTO voluntario(@PathVariable Integer id) {
        return cuentas.buscar(id, false);
    }

    @GetMapping("/organizaciones/{id}")
    public OrganizacionPublica organizacion(@PathVariable Integer id) {
        return angular.ficha(id);
    }

    @PostMapping("/voluntarios")
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaDTO crearVoluntario(@Valid @RequestBody CuentaDTO dto) {
        dto.setId(null);
        return cuentas.buscar(cuentas.guardar(dto, false), false);
    }

    @PostMapping("/organizaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaDTO crearOrganizacion(@Valid @RequestBody CuentaDTO dto) {
        dto.setId(null);
        return cuentas.buscar(cuentas.guardar(dto, true), true);
    }

    @PutMapping("/voluntarios/{id}")
    public CuentaDTO actualizarVoluntario(@PathVariable Integer id, @Valid @RequestBody CuentaDTO dto) {
        dto.setId(id);
        cuentas.guardar(dto, false);
        return cuentas.buscar(id, false);
    }

    @PutMapping("/organizaciones/{id}")
    public CuentaDTO actualizarOrganizacion(@PathVariable Integer id, @Valid @RequestBody CuentaDTO dto) {
        dto.setId(id);
        cuentas.guardar(dto, true);
        return cuentas.buscar(id, true);
    }

    @DeleteMapping("/voluntarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarVoluntario(@PathVariable Integer id) {
        cuentas.eliminar(id, false);
    }

    @DeleteMapping("/organizaciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarOrganizacion(@PathVariable Integer id) {
        cuentas.eliminar(id, true);
    }

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

    @PostMapping("/auth/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaDTO registrar(@Valid @RequestBody Registro dto, HttpServletRequest request,
                               HttpServletResponse response) {
        if (!Set.of("VOLUNTARIO", "ORGANIZACION").contains(dto.rol()))
            throw new IllegalArgumentException("Selecciona un perfil válido");
        cuentas.registrar(dto.perfil(), "ORGANIZACION".equals(dto.rol()));
        return sesion.ingresar(dto.email(), dto.contrasena(), request, response);
    }

    @PostMapping("/auth/salir")
    @ResponseStatus(HttpStatus.NO_CONTENT)
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
    public List<CampaniaVista> listarCampanias(@RequestParam(required = false) String q,
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) String distrito,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "false") boolean gestion, Authentication actor) {
        if (gestion && (actor == null || actor instanceof AnonymousAuthenticationToken))
            throw new AuthenticationCredentialsNotFoundException("Inicia sesión para continuar");
        String filtroUbicacion = (distrito != null && !distrito.isBlank()) ? distrito : ubicacion;
        return angular.listar(q, filtroUbicacion, categoria, gestion);
    }

    @GetMapping("/campanias/{id}")
    public CampaniaVista campania(@PathVariable Integer id) {
        return angular.campania(id);
    }

    @PostMapping("/campanias")
    @ResponseStatus(HttpStatus.CREATED)
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

    @DeleteMapping("/campanias/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarCampania(@PathVariable Integer id) {
        campanias.cancelar(id);
    }

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
    public List<Participacion> misParticipaciones() {
        return angular.historial(acceso.voluntarioId());
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(org.springframework.security.web.csrf.CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @GetMapping("/campanias/{id}/mi-inscripcion")
    public Object miInscripcion(@PathVariable Integer id) {
        campanias.buscar(id);
        return valorJSON(inscripciones.miInscripcion(id).map(angular::vista).orElse(null));
    }

    @PostMapping("/campanias/{id}/mi-inscripcion")
    @ResponseStatus(HttpStatus.CREATED)
    public InscripcionVista unirme(@PathVariable Integer id) {
        return angular.vista(inscripciones.unirme(id));
    }

    @DeleteMapping("/campanias/{id}/mi-inscripcion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void salir(@PathVariable Integer id) {
        inscripciones.salir(id);
    }

    @GetMapping("/campanias/{id}/participantes")
    public List<InscripcionVista> participantes(@PathVariable Integer id) {
        return inscripciones.participantes(id).stream().map(angular::vista).toList();
    }

    @PutMapping("/campanias/{id}/participantes/{inscripcionId}")
    public InscripcionVista marcarAsistencia(@PathVariable Integer id, @PathVariable Integer inscripcionId) {
        return angular.vista(inscripciones.actualizarParticipante(id, inscripcionId, true));
    }

    @DeleteMapping("/campanias/{id}/participantes/{inscripcionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirar(@PathVariable Integer id, @PathVariable Integer inscripcionId) {
        inscripciones.actualizarParticipante(id, inscripcionId, false);
    }

    @GetMapping("/impacto")
    public List<Impacto> impacto() {
        return angular.impacto();
    }

    @GetMapping("/campanias/{id}/impacto")
    public Resultado resultado(@PathVariable Integer id) {
        return angular.resultado(id);
    }

    @PutMapping("/campanias/{id}/impacto")
    public Resultado guardarResultado(@PathVariable Integer id, @Valid @RequestBody Resultado resultado) {
        return angular.registrarResultado(id, resultado);
    }

    private record ReglaChat(List<String> claves, String respuesta, String destino) {}

    private static final List<ReglaChat> REGLAS_CHAT = List.of(
        new ReglaChat(
            List.of("hola", "buenos dias", "buenas tardes", "buenas noches", "hey", "que tal"),
            "¡Hola! Puedo ayudarte a buscar campañas por tema, categoría (Ambiental o Social) o distrito, revisar tus inscripciones y resolver dudas sobre Voluntia.",
            "CATALOGO"
        ),
        new ReglaChat(
            List.of("que es voluntia", "de que trata", "para que sirve", "ayuda", "que puedes hacer", "opciones"),
            "Voluntia conecta voluntarios con organizaciones que impulsan campañas ambientales y sociales en Lima. Puedes explorar actividades, inscribirte gratis y hacer seguimiento a tus participaciones.",
            "CATALOGO"
        ),
        new ReglaChat(
            List.of("cancelar inscripcion", "desinscribirme", "salir de campana", "retirarme", "no puedo ir"),
            "Si ya no puedes asistir, entra a la ficha de la campaña o a Mis participaciones y pulsa Cancelar inscripción para liberar tu cupo.",
            "PARTICIPACIONES"
        ),
        new ReglaChat(
            List.of("como me inscribo", "como participar", "inscribirme", "unirme", "postular"),
            "Para inscribirte: 1) Inicia sesión como voluntario. 2) Abre Explorar campañas y selecciona una actividad. 3) Pulsa Inscribirme si aún hay cupos disponibles.",
            "CATALOGO"
        ),
        new ReglaChat(
            List.of("costo", "gratis", "pagar", "precio", "cobran", "gratuito"),
            "Participar en las campañas de Voluntia es 100% gratuito. Solo necesitas crear tu cuenta e inscribirte en la actividad que elijas.",
            "CATALOGO"
        ),
        new ReglaChat(
            List.of("requisito", "edad", "necesito", "menor de edad", "16 anos"),
            "Para registrarte como voluntario debes tener al menos 16 años. Además, cada campaña muestra sus requisitos específicos (materiales, horario o recomendaciones) dentro de su detalle.",
            "CATALOGO"
        ),
        new ReglaChat(
            List.of("publicar", "crear campana", "soy organizacion", "registrar organizacion", "ruc", "ong"),
            "Si representas una organización, regístrate eligiendo el perfil Organización con tu RUC y datos de contacto. Desde tu panel podrás crear campañas, gestionar voluntarios y registrar resultados.",
            null
        ),
        new ReglaChat(
            List.of("categorias", "tipos de campana", "que hacen", "ambiental y social"),
            "En Voluntia manejamos dos categorías principales: Ambiental (limpieza de playas, arborización, reciclaje) y Social (refuerzo escolar, donaciones, apoyo comunitario).",
            "CATALOGO"
        ),
        new ReglaChat(
            List.of("contacto", "soporte", "problema", "error", "correo", "ayuda tecnica"),
            "Si tienes algún inconveniente con una campaña, revisa los datos de contacto de la organización dentro de su perfil público, o actualiza los datos de tu cuenta desde Mi cuenta.",
            "PERFIL"
        ),
        new ReglaChat(
            List.of("gracias", "muchas gracias", "genial", "perfecto", "chau", "adios", "listo"),
            "¡Con gusto! Si necesitas buscar otra campaña o revisar tus inscripciones, aquí estoy.",
            null
        )
    );

    private static final Map<String, String> ALIAS_DISTRITOS = Map.ofEntries(
        Map.entry("sjl", "San Juan de Lurigancho"),
        Map.entry("sjm", "San Juan de Miraflores"),
        Map.entry("smp", "San Martín de Porres"),
        Map.entry("vmt", "Villa María del Triunfo"),
        Map.entry("ves", "Villa El Salvador"),
        Map.entry("surco", "Santiago de Surco"),
        Map.entry("magdalena", "Magdalena del Mar"),
        Map.entry("chosica", "Lurigancho"),
        Map.entry("lurigancho chosica", "Lurigancho"),
        Map.entry("cercado de lima", "Lima"),
        Map.entry("lima cercado", "Lima"),
        Map.entry("centro de lima", "Lima")
    );

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body, Authentication actor) {
        String mensaje = body.get("mensaje") == null ? "" : body.get("mensaje").toString().trim();
        String norm = normalizarChat(mensaje);
        Map<?, ?> anterior = body.get("anterior") instanceof Map<?, ?> m ? m : Map.of();
        boolean autenticado = actor != null && !(actor instanceof AnonymousAuthenticationToken);

        // 1. Buscar coincidencia en respuestas predeterminadas
        for (ReglaChat regla : REGLAS_CHAT) {
            if (regla.claves().stream().anyMatch(norm::contains)) {
                return respuestaChat(regla.respuesta(), List.of(), regla.destino(), null);
            }
        }

        // 2. Consultar inscripciones reales del voluntario
        if (norm.contains("inscripcion") || norm.contains("participacion") || norm.contains("inscrito") || norm.contains("historial")) {
            if (!autenticado) {
                return respuestaChat(
                    "Inicia sesión con tu cuenta de voluntario para revisar tus inscripciones activas y tu historial.",
                    List.of(), null, null
                );
            }
            if (!acceso.rol("VOLUNTARIO")) {
                return respuestaChat(
                    "Tu cuenta tiene perfil de organización o administrador. Puedes gestionar los inscritos desde cada campaña.",
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

        // 3. Detectar categoría, distrito y palabras clave en preguntas abiertas
        String categoria = norm.contains("ambiental") ? "Ambiental"
                : (norm.contains("social") ? "Social" : null);

        String distrito = null;
        if (norm.contains("cerca de mi") || norm.contains("mi distrito") || norm.contains("mi zona") || norm.contains("por aqui")) {
            if (!autenticado) {
                return respuestaChat(
                    "Inicia sesión y guarda tu distrito en Mi cuenta para mostrarte las campañas cerca de ti.",
                    List.of(), "PERFIL", null
                );
            }
            String miUbicacion = cuentas.miCuenta().getUbicacion();
            if (miUbicacion == null || miUbicacion.isBlank()) {
                return respuestaChat(
                    "Aún no tienes un distrito guardado en tu perfil. Actualízalo en Mi cuenta para buscar cerca de ti.",
                    List.of(), "PERFIL", null
                );
            }
            distrito = miUbicacion;
        } else {
            String padded = " " + norm + " ";
            for (var entry : ALIAS_DISTRITOS.entrySet()) {
                if (padded.contains(" " + entry.getKey() + " ")) {
                    distrito = entry.getValue();
                    break;
                }
            }
            if (distrito == null) {
                for (String d : distritos()) {
                    if (!"Lima".equals(d) && padded.contains(" " + normalizarChat(d) + " ")) {
                        distrito = d;
                        break;
                    }
                }
            }
        }

        // Mantener contexto del mensaje anterior (ej. "Campañas ambientales" -> "¿Y en Comas?")
        if (categoria == null && distrito != null && anterior.get("categoria") instanceof String catAnt && !catAnt.isBlank()) {
            categoria = catAnt;
        }
        if (distrito == null && categoria != null && anterior.get("distrito") instanceof String distAnt && !distAnt.isBlank()) {
            distrito = distAnt;
        }

        List<String> palabrasClave = extraerPalabrasClaveChat(norm, distrito);
        List<CampaniaVista> base = angular.listar(null, distrito, categoria, false);
        List<CampaniaVista> encontradas = base.stream()
                .filter(c -> palabrasClave.isEmpty() || palabrasClave.stream().anyMatch(p ->
                        normalizarChat(c.titulo() + " " + c.descripcion() + " " + c.organizacion()).contains(p)))
                .limit(5)
                .toList();

        Map<String, String> filtros = new LinkedHashMap<>();
        if (categoria != null) filtros.put("categoria", categoria);
        if (distrito != null) filtros.put("distrito", distrito);
        if (!palabrasClave.isEmpty()) filtros.put("q", String.join(" ", palabrasClave));

        if (encontradas.isEmpty()) {
            return respuestaChat(
                "No encontré campañas con ese criterio. Prueba preguntarme por campañas ambientales, sociales, por un distrito (ej. Miraflores, Comas) o temas como limpieza, plantas o refuerzo escolar.",
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

    private List<String> extraerPalabrasClaveChat(String norm, String distrito) {
        Set<String> vacias = Set.of(
            "que", "cual", "cuales", "cuantas", "cuantos", "hay", "tienen", "tiene", "busco", "buscar",
            "quiero", "quisiera", "ver", "mostrar", "dime", "dame", "campana", "campanas", "voluntariado",
            "voluntariados", "actividad", "actividades", "proxima", "proximas", "disponible", "disponibles",
            "actuales", "ambiental", "ambientales", "social", "sociales", "lima", "peru", "voluntia",
            "donde", "cuando", "como", "puedo", "ayudar", "participar", "unirme", "cupo", "cupos",
            "fecha", "fechas", "hoy", "esta", "semana", "mes", "todo", "toda", "todos", "todas",
            "algun", "alguna", "algunos", "algunas", "de", "del", "la", "las", "el", "los",
            "un", "una", "unos", "unas", "en", "para", "por", "con", "sobre", "sin", "al", "y", "o", "me", "se"
        );
        Set<String> palabrasDistrito = distrito == null ? Set.of()
                : Set.of(normalizarChat(distrito).split("\\s+"));

        return java.util.Arrays.stream(norm.split("\\s+"))
                .filter(p -> p.length() >= 3)
                .filter(p -> !vacias.contains(p))
                .filter(p -> !palabrasDistrito.contains(p))
                .filter(p -> !ALIAS_DISTRITOS.containsKey(p))
                .toList();
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
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Object valorJSON(Object valor) {
        return valor == null ? tools.jackson.databind.node.NullNode.getInstance() : valor;
    }
}
