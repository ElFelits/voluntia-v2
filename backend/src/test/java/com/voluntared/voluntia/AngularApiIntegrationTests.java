package com.voluntared.voluntia;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "voluntia.cors.origins=http://localhost:4200")
@AutoConfigureMockMvc
@Transactional
class AngularApiIntegrationTests {
    private static final String ORIGEN = "http://localhost:4200";
    @Autowired MockMvc mvc;

    @Test
    void corsAdmiteCredencialesYRechazaOtrosOrigenes() throws Exception {
        mvc.perform(options("/api/auth/registro")
                .header("Origin", ORIGEN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,x-csrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/auth/registro")
                .header("Origin", "http://localhost:9999")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registroRestauraSesionYLaBajaRevocaElAcceso() throws Exception {
        mvc.perform(get("/api/auth/sesion")).andExpect(status().isOk()).andExpect(content().string("null"));
        String correo = UUID.randomUUID() + "@example.org";
        String registro = """
                {"nombre":"Voluntaria Angular","email":"%s","contrasena":"prueba123",
                 "ubicacion":"Lima","rol":"VOLUNTARIO","edad":24}
                """.formatted(correo);
        mvc.perform(post("/api/auth/registro").contentType("application/json").content(registro))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.mensaje").exists());
        var inicio = mvc.perform(get("/api/csrf").header("Origin", ORIGEN))
                .andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) inicio.getRequest().getSession(false);
        var json = JsonMapper.builder().build();
        var csrf = json.readTree(inicio.getResponse().getContentAsString());
        var resultado = mvc.perform(post("/api/auth/registro").session(session).header("Origin", ORIGEN)
                .header(csrf.get("headerName").asString(), csrf.get("token").asString())
                .contentType("application/json").content(registro))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("VOLUNTARIO"))
                .andExpect(jsonPath("$.contrasena").doesNotExist()).andReturn();
        var autenticada = (MockHttpSession) resultado.getRequest().getSession(false);
        var id = json.readTree(resultado.getResponse().getContentAsString()).get("id").asInt();

        mvc.perform(get("/api/auth/sesion").session(autenticada))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/impacto").session(autenticada))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.mensaje").exists());
        var tokenAdmin = mvc.perform(get("/api/csrf")).andReturn();
        var csrfAdmin = json.readTree(tokenAdmin.getResponse().getContentAsString());
        mvc.perform(delete("/api/voluntarios/{id}", id).with(user("admin").roles("ADMIN"))
                .session((MockHttpSession) tokenAdmin.getRequest().getSession(false))
                .header(csrfAdmin.get("headerName").asString(), csrfAdmin.get("token").asString()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/sesion").session(autenticada))
                .andExpect(status().isOk()).andExpect(content().string("null"));
        mvc.perform(get("/api/mis-participaciones").session(autenticada))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.mensaje").exists());
    }
}
