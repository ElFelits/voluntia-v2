package com.voluntared.voluntia;

import com.voluntared.voluntia.dto.*;
import com.voluntared.voluntia.service.*;
import com.voluntared.voluntia.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class CrudIntegrationTests {
    @Autowired CuentaService cuentas;
    @Autowired CampaniaService campanias;
    @Autowired InscripcionService inscripciones;
    @Autowired UsuarioRepository usuarios;
    @Autowired MockMvc mvc;
    void adminContext() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated("admin", "", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
    CuentaDTO cuenta(boolean org) {
        CuentaDTO d=new CuentaDTO();d.setNombre("Prueba comunidad");d.setEmail(UUID.randomUUID()+"@example.org");
        d.setContrasena("prueba123");d.setUbicacion("Lima");d.setEdad(22);d.setHabilidades("Reciclaje");
        if(org){d.setRuc("20123456789");d.setInfoContacto("contacto@example.org");} return d;
    }
    @Test @WithMockUser(roles="ADMIN")
    void crudConRelacionesCuposHistorialYVistas() throws Exception {
        CuentaDTO o=cuenta(true);Integer oid=cuentas.guardar(o,true);
        CuentaDTO v=cuenta(false);Integer vid=cuentas.guardar(v,false);
        assertNotEquals("prueba123",cuentas.voluntario(vid).getUsuario().getContrasena());
        Integer uid=cuentas.voluntario(vid).getUsuario().getId();
        CuentaDTO edit=cuentas.buscar(vid,false);edit.setNombre("Nombre actualizado");cuentas.guardar(edit,false);
        assertEquals("Nombre actualizado",cuentas.buscar(vid,false).getNombre());
        CuentaDTO duplicate=cuenta(false);duplicate.setEmail(v.getEmail());assertThrows(IllegalArgumentException.class,()->cuentas.guardar(duplicate,false));
        CampaniaDTO c=new CampaniaDTO();c.setTitulo("Limpieza comunitaria");c.setDescripcion("Cuidemos el entorno");c.setUbicacion("Lima");c.setFecha(LocalDateTime.now().plusDays(3));c.setCupos(1);c.setOrganizacionId(oid);
        Integer cid=campanias.guardar(c);assertEquals(1,campanias.listar("Limpieza comunitaria","Lima").size());
        inscripciones.inscribir(cid,vid);assertEquals(1,inscripciones.historial(vid).size());
        assertThrows(IllegalStateException.class,()->inscripciones.inscribir(cid,vid));
        Integer other=cuentas.guardar(cuenta(false),false);
        assertThrows(IllegalStateException.class,()->inscripciones.inscribir(cid,other));
        Integer iid=inscripciones.historial(vid).get(0).getId();inscripciones.asistencia(iid);
        assertEquals("ASISTIO",inscripciones.historial(vid).get(0).getEstado());

        mvc.perform(get("/api/campanias/" + cid)).andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Limpieza comunitaria"));
        mvc.perform(get("/api/voluntarios/" + vid)).andExpect(status().isOk())
                .andExpect(jsonPath("$.contrasena").doesNotExist());
        adminContext();
        CampaniaDTO updated=campanias.editar(cid);updated.setTitulo("Campaña actualizada");campanias.guardar(updated);assertEquals("Campaña actualizada",campanias.buscar(cid).getTitulo());
        assertThrows(IllegalArgumentException.class,()->inscripciones.cancelar(iid));
        cuentas.eliminar(oid,true);
        assertEquals("CANCELADA",campanias.buscar(cid).getEstado());
        assertEquals("ASISTIO",inscripciones.historial(vid).get(0).getEstado());
        cuentas.eliminar(vid,false);
        assertFalse(usuarios.findById(uid).orElseThrow().isActivo());
        campanias.eliminar(cid);cuentas.eliminar(other,false);
    }
    @Test @WithMockUser(roles="ADMIN")
    void apiCrudYProteccionCsrf() throws Exception {
        String email=UUID.randomUUID()+"@example.org";
        String body="{\"nombre\":\"API Prueba\",\"email\":\""+email+"\",\"contrasena\":\"prueba123\",\"ubicacion\":\"Lima\",\"edad\":21}";
        mvc.perform(post("/api/voluntarios").contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/voluntarios").with(csrf()).contentType("application/json").content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.contrasena").doesNotExist());
        adminContext();
        Integer id=cuentas.listarVoluntarios().stream().filter(v->v.getUsuario().getEmail().equals(email)).findFirst().orElseThrow().getId();
        mvc.perform(put("/api/voluntarios/"+id).with(csrf()).contentType("application/json").content(body.replace("API Prueba","API Editado"))).andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("API Editado"));
        mvc.perform(delete("/api/voluntarios/"+id).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/voluntarios/"+id)).andExpect(status().isNotFound());
        mvc.perform(post("/api/voluntarios").with(csrf()).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
    }

    @Test void backendSoloExponeLaApi() throws Exception {
        for (String ruta : new String[]{"/", "/login", "/registro", "/campanias", "/css/styles.css", "/js/app.js"})
            mvc.perform(get(ruta)).andExpect(status().isNotFound());
        mvc.perform(get("/api/campanias")).andExpect(status().isOk());
        mvc.perform(get("/api/impacto")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.mensaje").exists());
    }
    @Test @WithMockUser(roles="ADMIN")
    void gestionGeneralDeCuentasSigueRestringidaAlAdministrador() throws Exception {
        mvc.perform(put("/api/mi-cuenta").with(csrf())).andExpect(status().isForbidden());
        for(String path:new String[]{"/api/voluntarios","/api/organizaciones"}) {
            mvc.perform(get(path).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("perfil").roles("VOLUNTARIO")))
                .andExpect(status().isForbidden());
        }
    }
}
