package com.voluntared.voluntia;

import com.voluntared.voluntia.dto.*;
import com.voluntared.voluntia.service.*;
import com.voluntared.voluntia.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.json.JsonMapper;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@SpringBootTest @AutoConfigureMockMvc
class PerfilesIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired CuentaService cuentas;
    @Autowired CampaniaService campanias;
    @Autowired InscripcionService inscripciones;
    @Autowired UsuarioRepository usuarios;
    @Autowired VoluntarioRepository voluntarios;
    @Autowired InscripcionRepository inscripcionRepo;
    @Autowired CampaniaRepository campaniaRepo;

    void contexto(String nombre,String rol){
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(nombre,"",List.of(new SimpleGrantedAuthority("ROLE_"+rol))));
    }
    CuentaDTO cuenta(boolean org){
        CuentaDTO d=new CuentaDTO();d.setNombre(org?"Organización de prueba":"Voluntario de prueba");d.setEmail(UUID.randomUUID()+"@example.org");
        d.setContrasena("prueba123");d.setUbicacion("Lima");d.setEdad(23);d.setHabilidades("Reciclaje");
        if(org){d.setRuc("20123456789");d.setInfoContacto("Contacto de prueba");}return d;
    }
    CampaniaDTO campania(Integer organizacionId,int cupos){
        CampaniaDTO d=new CampaniaDTO();d.setTitulo("Iniciativa "+UUID.randomUUID());d.setDescripcion("Plantaremos árboles para recuperar los espacios públicos.");
        d.setUbicacion("Lima");d.setFecha(LocalDateTime.now().plusDays(3));d.setCupos(cupos);d.setOrganizacionId(organizacionId);return d;
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    MockHttpServletRequestBuilder perfil(CuentaDTO d) {
        return put("/api/mi-cuenta").with(csrf()).contentType("application/json").content(JSON.writeValueAsString(d));
    }

    MockHttpServletRequestBuilder credenciales(String email, String password) {
        return post("/api/auth/ingresar").with(csrf()).contentType("application/json")
                .content(JSON.writeValueAsString(Map.of("email", email, "contrasena", password)));
    }

    MockHttpSession login(String email,String password) throws Exception {
        return (MockHttpSession) mvc.perform(credenciales(email, password)).andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test @Transactional
    void crearCuentaDeAmbosPerfilesEIngresarConCorreo() throws Exception {
        for (String rol : List.of("VOLUNTARIO", "ORGANIZACION")) {
            String email = UUID.randomUUID() + "@example.org";
            var datos = new HashMap<String, Object>(Map.of("rol", rol, "nombre", "Cuenta nueva", "email", email,
                    "contrasena", "prueba123", "ubicacion", "Lima", "ruc", "20123456789", "infoContacto", "Contacto"));
            datos.put("edad", 23);
            String cuerpo = JSON.writeValueAsString(datos);
            mvc.perform(post("/api/auth/registro").with(csrf()).contentType("application/json").content(cuerpo))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value(rol));
            var usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
            assertTrue(usuario.getContrasena().startsWith("$2"));
            MockHttpSession session = login(email.toUpperCase(Locale.ROOT), "prueba123");
            mvc.perform(get("/api/auth/sesion").session(session)).andExpect(jsonPath("$.email").value(email));
            mvc.perform(get(rol.equals("ORGANIZACION") ? "/api/campanias?gestion=true" : "/api/mis-participaciones")
                    .session(session)).andExpect(status().isOk());
            for (String ruta : List.of("/api/voluntarios", "/api/organizaciones"))
                mvc.perform(get(ruta).session(session)).andExpect(status().isForbidden());
            mvc.perform(post("/api/auth/registro").with(csrf()).contentType("application/json").content(cuerpo))
                    .andExpect(status().isBadRequest());
            datos.put("rol", "ADMIN");
            mvc.perform(post("/api/auth/registro").with(csrf()).contentType("application/json")
                    .content(JSON.writeValueAsString(datos))).andExpect(status().isBadRequest());
        }
    }

    @Test @Transactional
    void miCuentaSoloModificaLaCuentaPropiaSinCambiarSuRol() throws Exception {
        for(boolean org:List.of(false,true)){
            CuentaDTO d=cuenta(org),ajena=cuenta(org);
            Integer id=cuentas.registrar(d,org),otroId=cuentas.registrar(ajena,org);
            Integer uid=usuarios.findByEmailIgnoreCase(d.getEmail()).orElseThrow().getId();
            String hash=usuarios.findById(uid).orElseThrow().getContrasena();
            MockHttpSession session=login(d.getEmail(),"prueba123");d.setNombre("Mi nombre actualizado");
            d.setId(otroId);
            d.setRol("ADMIN");
            mvc.perform(perfil(d).session(session)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id));
            var actualizado=usuarios.findById(uid).orElseThrow();assertEquals("Mi nombre actualizado",actualizado.getNombre());
            assertEquals(org?"ORGANIZACION":"VOLUNTARIO",actualizado.getRol());assertEquals(hash,actualizado.getContrasena());
            assertEquals(ajena.getNombre(),usuarios.findByEmailIgnoreCase(ajena.getEmail()).orElseThrow().getNombre());
            mvc.perform(get("/api/auth/sesion").session(session)).andExpect(status().isOk());
            String propio=d.getEmail();d.setEmail(ajena.getEmail());
            mvc.perform(perfil(d).session(session)).andExpect(status().isBadRequest());
            d.setEmail(propio);
            mvc.perform(put("/api/"+(org?"organizaciones":"voluntarios")+"/"+otroId).with(csrf()).session(session)).andExpect(status().isForbidden());
            mvc.perform(delete("/api/"+(org?"organizaciones":"voluntarios")+"/"+id).session(session).with(csrf())).andExpect(status().isForbidden());
        }
    }


    @Test @Transactional
    void cambiarCorreoMantieneLaSesionYPermiteIngresarConElNuevoCorreo() throws Exception {
        for (boolean org : List.of(false, true)) {
            CuentaDTO d = cuenta(org);
            cuentas.registrar(d, org);
            MockHttpSession session = login(d.getEmail(), "prueba123");
            String anterior = d.getEmail();
            d.setEmail(UUID.randomUUID() + "@example.org");
            mvc.perform(perfil(d).session(session)).andExpect(status().isOk());
            mvc.perform(get("/api/auth/sesion").session(session)).andExpect(jsonPath("$.email").value(d.getEmail()));
            mvc.perform(credenciales(anterior, "prueba123")).andExpect(status().isUnauthorized());
            login(d.getEmail(), "prueba123");
        }
    }

    @Test @Transactional @WithMockUser(roles="ADMIN")
    void organizacionPublicaDescripcionYGestionaUnicamenteSusCampanias() throws Exception {
        CuentaDTO o=cuenta(true),otra=cuenta(true),v=cuenta(false);
        Integer oid=cuentas.guardar(o,true),otroId=cuentas.guardar(otra,true);cuentas.guardar(v,false);
        String titulo="Publicación propia "+UUID.randomUUID();

        var datos = JSON.createObjectNode().put("titulo", titulo).put("descripcion", "Recogeremos residuos de la playa.")
                .put("ubicacion", "Lima").put("fecha", LocalDateTime.now().plusDays(3).toString())
                .put("cupos", 2).put("organizacionId", otroId);
        var publicacion = mvc.perform(post("/api/campanias").with(user(o.getEmail()).roles("ORGANIZACION"))
                .with(csrf()).contentType("application/json").content(JSON.writeValueAsString(datos)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.organizacionId").value(oid)).andReturn();
        int cid = JSON.readTree(publicacion.getResponse().getContentAsString()).get("id").asInt();
        assertEquals("Recogeremos residuos de la playa.",campanias.buscar(cid).getDescripcion());
        mvc.perform(get("/api/campanias?gestion=true").with(user(o.getEmail()).roles("ORGANIZACION")))
                .andExpect(jsonPath("$[0].titulo").value(titulo));
        mvc.perform(get("/api/campanias?gestion=true").with(user(otra.getEmail()).roles("ORGANIZACION")))
                .andExpect(content().json("[]"));
        String json="{\"titulo\":\"Edición API\",\"descripcion\":\"Cuidamos la comunidad\",\"ubicacion\":\"Lima\",\"fecha\":\"2030-01-01T10:00:00\",\"cupos\":2}";
        mvc.perform(put("/api/campanias/"+cid).with(user(otra.getEmail()).roles("ORGANIZACION")).with(csrf()).contentType("application/json").content(json)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/campanias/"+cid).with(user(otra.getEmail()).roles("ORGANIZACION")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/campanias").with(user(v.getEmail()).roles("VOLUNTARIO")).with(csrf()).contentType("application/json").content(json)).andExpect(status().isForbidden());
        mvc.perform(put("/api/campanias/"+cid).with(user(o.getEmail()).roles("ORGANIZACION")).with(csrf()).contentType("application/json").content(json)).andExpect(status().isOk()).andExpect(jsonPath("$.organizacionId").value(oid));
        mvc.perform(post("/api/campanias").with(user(o.getEmail()).roles("ORGANIZACION")).with(csrf()).contentType("application/json").content(json)).andExpect(status().isCreated());
        mvc.perform(delete("/api/campanias/"+cid).with(user(o.getEmail()).roles("ORGANIZACION")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/campanias/"+cid).with(anonymous())).andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test @Transactional @WithMockUser(roles="ADMIN")
    void voluntarioSeUneSaleYLaOrganizacionRetiraSinEliminarLaCuenta() throws Exception {
        CuentaDTO o=cuenta(true),otra=cuenta(true),v=cuenta(false),otro=cuenta(false);
        Integer oid=cuentas.guardar(o,true);cuentas.guardar(otra,true);Integer vid=cuentas.guardar(v,false),otroVid=cuentas.guardar(otro,false);
        Integer cid=campanias.guardar(campania(oid,1)),otroCid=campanias.guardar(campania(oid,2));

        mvc.perform(post("/api/campanias/"+cid+"/mi-inscripcion").with(user(v.getEmail()).roles("VOLUNTARIO"))
                .with(csrf()).contentType("application/json").content("{\"voluntarioId\":"+otroVid+"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.voluntarioId").value(vid));
        int iid = inscripcionRepo.findByVoluntarioIdAndCampaniaId(vid,cid).orElseThrow().getId();
        mvc.perform(get("/api/mis-participaciones").with(user(v.getEmail()).roles("VOLUNTARIO")))
                .andExpect(jsonPath("$[0].campaniaId").value(cid));
        mvc.perform(get("/api/campanias/"+cid+"/participantes").with(user(o.getEmail()).roles("ORGANIZACION")))
                .andExpect(jsonPath("$[0].id").value(iid));
        mvc.perform(get("/api/campanias/"+cid+"/participantes").with(user(otra.getEmail()).roles("ORGANIZACION")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/campanias/"+cid+"/mi-inscripcion").with(user(v.getEmail()).roles("VOLUNTARIO")).with(csrf())).andExpect(status().isConflict());
        mvc.perform(post("/api/campanias/"+cid+"/mi-inscripcion").with(user(otro.getEmail()).roles("VOLUNTARIO")).with(csrf())).andExpect(status().isConflict());
        mvc.perform(post("/api/campanias/"+otroCid+"/mi-inscripcion").with(user(v.getEmail()).roles("VOLUNTARIO")).with(csrf())).andExpect(status().isCreated());
        mvc.perform(delete("/api/campanias/"+cid+"/participantes/"+iid).with(user(otra.getEmail()).roles("ORGANIZACION")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/campanias/"+cid+"/participantes/"+iid).with(user(o.getEmail()).roles("ORGANIZACION")).with(csrf())).andExpect(status().isNoContent());
        assertTrue(voluntarios.existsById(vid));
        assertTrue(inscripcionRepo.existsByVoluntarioIdAndCampaniaId(vid,otroCid));
        assertEquals("CANCELADO", inscripcionRepo.findById(iid).orElseThrow().getEstado());
        mvc.perform(post("/api/campanias/"+cid+"/mi-inscripcion").with(user(v.getEmail()).roles("VOLUNTARIO")).with(csrf())).andExpect(status().isCreated());
        mvc.perform(delete("/api/campanias/"+cid+"/mi-inscripcion").with(user(v.getEmail()).roles("VOLUNTARIO")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(post("/api/campanias/"+cid+"/mi-inscripcion").with(user(otro.getEmail()).roles("VOLUNTARIO")).with(csrf())).andExpect(status().isCreated());
        mvc.perform(delete("/api/campanias/"+cid+"/mi-inscripcion").with(user(otro.getEmail()).roles("VOLUNTARIO")).with(csrf())).andExpect(status().isNoContent());
        Integer otroIid = inscripcionRepo.findByVoluntarioIdAndCampaniaId(vid,otroCid).orElseThrow().getId();
        mvc.perform(delete("/api/campanias/"+otroCid+"/participantes/"+otroIid).with(user(o.getEmail()).roles("ORGANIZACION")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/voluntarios/"+otroVid).with(user(v.getEmail()).roles("VOLUNTARIO"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/campanias/"+cid+"/mi-inscripcion").with(anonymous())).andExpect(status().isForbidden());
        mvc.perform(post("/api/campanias/"+cid+"/mi-inscripcion").with(anonymous()).with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test @Transactional @WithMockUser(roles="ADMIN")
    void asistenciaSeConservaYCampaniaIniciadaNoAdmiteInscripciones() throws Exception {
        CuentaDTO o=cuenta(true),v=cuenta(false);Integer oid=cuentas.guardar(o,true),vid=cuentas.guardar(v,false),cid=campanias.guardar(campania(oid,2));
        inscripciones.inscribir(cid,vid);Integer iid=inscripcionRepo.findByVoluntarioIdAndCampaniaId(vid,cid).orElseThrow().getId();
        mvc.perform(put("/api/campanias/"+cid+"/participantes/"+iid).with(user(o.getEmail()).roles("ORGANIZACION")).with(csrf())).andExpect(jsonPath("$.estado").value("ASISTIO"));
        mvc.perform(delete("/api/campanias/"+cid+"/mi-inscripcion").with(user(v.getEmail()).roles("VOLUNTARIO")).with(csrf())).andExpect(status().isConflict());

        mvc.perform(get("/api/mis-participaciones").with(user(v.getEmail()).roles("VOLUNTARIO")))
                .andExpect(jsonPath("$[0].estado").value("ASISTIO"));
        mvc.perform(delete("/api/campanias/"+cid+"/participantes/"+iid).with(user(o.getEmail()).roles("ORGANIZACION")).with(csrf())).andExpect(status().isConflict());
        assertEquals("ASISTIO",inscripcionRepo.findById(iid).orElseThrow().getEstado());
        campanias.buscar(cid).setFecha(LocalDateTime.now().minusDays(1));campaniaRepo.flush();
        contexto("admin","ADMIN");CuentaDTO otro=cuenta(false);cuentas.guardar(otro,false);
        mvc.perform(post("/api/campanias/"+cid+"/mi-inscripcion").with(user(otro.getEmail()).roles("VOLUNTARIO")).with(csrf())).andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles="ADMIN")
    void solicitudesSimultaneasNoSobrepasanCuposNiDuplicanInscripciones() throws Exception {
        CuentaDTO o=cuenta(true),v1=cuenta(false),v2=cuenta(false);
        Integer oid=cuentas.guardar(o,true),vid1=cuentas.guardar(v1,false),vid2=cuentas.guardar(v2,false);
        List<Integer> ids=new ArrayList<>();ExecutorService pool=Executors.newFixedThreadPool(2);
        try{
            for(boolean duplicado:List.of(false,true)){
                Integer cid=campanias.guardar(campania(oid,duplicado?2:1));ids.add(cid);
                CountDownLatch preparados=new CountDownLatch(2),inicio=new CountDownLatch(1);List<Future<Boolean>> resultados=new ArrayList<>();
                for(String email:List.of(v1.getEmail(),duplicado?v1.getEmail():v2.getEmail()))resultados.add(pool.submit(()->{
                    contexto(email,"VOLUNTARIO");preparados.countDown();inicio.await(10,TimeUnit.SECONDS);
                    try{inscripciones.unirme(cid);return true;}catch(IllegalStateException e){return false;}finally{SecurityContextHolder.clearContext();}
                }));
                assertTrue(preparados.await(10,TimeUnit.SECONDS));inicio.countDown();int exitos=0;
                for(var resultado:resultados)if(resultado.get(20,TimeUnit.SECONDS))exitos++;
                assertEquals(1,exitos);assertEquals(1,inscripcionRepo.countByCampaniaId(cid));
            }
        }finally{
            pool.shutdownNow();contexto("admin","ADMIN");for(Integer id:ids)campanias.eliminar(id);
            cuentas.eliminar(vid1,false);cuentas.eliminar(vid2,false);cuentas.eliminar(oid,true);
        }
    }
}
