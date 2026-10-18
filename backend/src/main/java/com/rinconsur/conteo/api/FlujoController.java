package com.rinconsur.conteo.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/flujo")
public class FlujoController {

    private final FlujoService servicio;

    public FlujoController(FlujoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<RegistroDto> listar(@RequestParam(required = false) Integer acceso) {
        return servicio.listar(acceso);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroDto crear(@Valid @RequestBody RegistroDto registro) {
        return servicio.crear(registro);
    }

    @GetMapping("/resumen")
    public List<ResumenAccesoDto> resumen() {
        return servicio.resumen();
    }
}
