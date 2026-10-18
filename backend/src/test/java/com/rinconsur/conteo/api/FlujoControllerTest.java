package com.rinconsur.conteo.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FlujoController.class)
class FlujoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private FlujoService servicio;

    @Test
    void getListaRegistros() throws Exception {
        when(servicio.listar(null)).thenReturn(List.of(new RegistroDto(1L, 1, 1L, "TRANSITO", "18-29", 10)));

        mvc.perform(get("/api/flujo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("TRANSITO"));
    }

    @Test
    void postValidoDevuelve201() throws Exception {
        when(servicio.crear(any())).thenReturn(new RegistroDto(7L, 2, 1L, "COMPRA", "30-44", 3));

        mvc.perform(post("/api/flujo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accesoId\":2,\"fechaHora\":1,\"tipo\":\"COMPRA\",\"rangoEtario\":\"30-44\",\"cantidad\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void postConCantidadInvalidaDevuelve400() throws Exception {
        mvc.perform(post("/api/flujo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accesoId\":2,\"fechaHora\":1,\"tipo\":\"COMPRA\",\"rangoEtario\":\"30-44\",\"cantidad\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postConAccesoFueraDeRangoDevuelve400() throws Exception {
        mvc.perform(post("/api/flujo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accesoId\":9,\"fechaHora\":1,\"tipo\":\"TRANSITO\",\"rangoEtario\":\"60+\",\"cantidad\":5}"))
                .andExpect(status().isBadRequest());
    }
}
