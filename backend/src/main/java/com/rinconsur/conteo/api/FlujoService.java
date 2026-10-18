package com.rinconsur.conteo.api;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio en memoria para el MVP (el caso solo permite datos ficticios).
 * Se siembra con unos pocos registros sintéticos.
 */
@Service
public class FlujoService {

    private final Map<Long, RegistroDto> datos = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong();

    public FlujoService() {
        long ahora = System.currentTimeMillis();
        crear(new RegistroDto(null, 1, ahora, "TRANSITO", "18-29", 40));
        crear(new RegistroDto(null, 1, ahora, "COMPRA", "18-29", 8));
        crear(new RegistroDto(null, 2, ahora, "TRANSITO", "30-44", 22));
        crear(new RegistroDto(null, 3, ahora, "TRANSITO", "60+", 15));
        crear(new RegistroDto(null, 3, ahora, "COMPRA", "45-59", 6));
        crear(new RegistroDto(null, 4, ahora, "COMPRA", "30-44", 9));
    }

    public List<RegistroDto> listar(Integer acceso) {
        return datos.values().stream()
                .filter(r -> acceso == null || r.accesoId().equals(acceso))
                .sorted(Comparator.comparing(RegistroDto::id))
                .toList();
    }

    public RegistroDto crear(RegistroDto nuevo) {
        long id = secuencia.incrementAndGet();
        RegistroDto guardado = nuevo.conId(id);
        datos.put(id, guardado);
        return guardado;
    }

    public List<ResumenAccesoDto> resumen() {
        List<ResumenAccesoDto> resultado = new ArrayList<>();
        for (int acceso = 1; acceso <= 4; acceso++) {
            int transito = sumar(acceso, "TRANSITO");
            int compra = sumar(acceso, "COMPRA");
            int total = transito + compra;
            double pct = total == 0 ? 0.0 : (double) transito / total;
            resultado.add(new ResumenAccesoDto(acceso, transito, compra, pct));
        }
        return resultado;
    }

    private int sumar(int acceso, String tipo) {
        return datos.values().stream()
                .filter(r -> r.accesoId() == acceso && r.tipo().equals(tipo))
                .mapToInt(RegistroDto::cantidad)
                .sum();
    }
}
