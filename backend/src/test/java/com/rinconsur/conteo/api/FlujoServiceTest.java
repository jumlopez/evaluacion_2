package com.rinconsur.conteo.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlujoServiceTest {

    private FlujoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new FlujoService();
    }

    @Test
    void crearAsignaIdYLoGuarda() {
        int antes = servicio.listar(null).size();
        RegistroDto guardado = servicio.crear(new RegistroDto(null, 2, 1L, "COMPRA", "60+", 5));

        assertNotNull(guardado.id());
        assertEquals(antes + 1, servicio.listar(null).size());
    }

    @Test
    void listarFiltraPorAcceso() {
        assertTrue(servicio.listar(1).stream().allMatch(r -> r.accesoId() == 1));
    }

    @Test
    void resumenIncluyeLosCuatroAccesosYCalculaPorcentaje() {
        var resumen = servicio.resumen();
        assertEquals(4, resumen.size());

        // Acceso 1 sembrado: 40 tránsito + 8 compra => 40/48
        var acceso1 = resumen.get(0);
        assertEquals(40, acceso1.transito());
        assertEquals(8, acceso1.compra());
        assertEquals(40.0 / 48.0, acceso1.pctTransito(), 0.0001);
    }
}
