package com.rinconsur.conteo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.model.CrucePorticos
import com.rinconsur.conteo.model.ResumenAcceso
import com.rinconsur.conteo.model.ResumenGeneral
import kotlin.math.roundToInt

@Composable
fun Cargando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorTexto(mensaje: String?) {
    if (mensaje != null) {
        Text(mensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun DatoNumerico(etiqueta: String, valor: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor.toString(), style = MaterialTheme.typography.headlineMedium)
        Text(etiqueta, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun FilaTotales(entradas: Int, salidas: Int, aforoNeto: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        DatoNumerico("Entradas", entradas)
        DatoNumerico("Salidas", salidas)
        DatoNumerico("Aforo neto", aforoNeto)
    }
}

/** Tarjeta por acceso: entradas, salidas, aforo neto y conversión de compra. */
@Composable
fun TarjetaAcceso(resumen: ResumenAcceso, modifier: Modifier = Modifier) {
    val progreso by animateFloatAsState(targetValue = resumen.pctCompra, label = "pctCompra")

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Accesos.etiqueta(resumen.accesoId), style = MaterialTheme.typography.titleMedium)
            FilaTotales(resumen.personasEntradas, resumen.personasSalidas, resumen.aforoNeto)
            LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth())
            Text(
                "${(resumen.pctCompra * 100).roundToInt()}% de las entradas compró · " +
                    "${resumen.ninosEntradas} niños acompañantes",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/** Comparación entre lo que marca el pórtico y lo registrado desde las cámaras. */
@Composable
fun PanelCrucePorticos(cruce: CrucePorticos, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Pórticos vs. registro", style = MaterialTheme.typography.titleMedium)

            if (!cruce.hayDatosPortico) {
                Text(
                    "Sin conteos de pórticos en el periodo. Regístralos en la pestaña Pórticos " +
                        "para ver cuánto del flujo real queda cubierto.",
                    style = MaterialTheme.typography.bodySmall
                )
                return@Column
            }

            val cobertura = cruce.coberturaEntradas
            if (cobertura != null) {
                val progreso by animateFloatAsState(
                    targetValue = cobertura.coerceIn(0f, 1f),
                    label = "cobertura"
                )
                LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth())
                Text(
                    "Cobertura de entradas: ${(cobertura * 100).roundToInt()}% · " +
                        "${cruce.totalRegistradoEntradas} registradas de " +
                        "${cruce.totalPorticoEntradas} del pórtico",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            cruce.filas.forEach { fila ->
                HorizontalDivider()
                Text(Accesos.etiqueta(fila.accesoId), style = MaterialTheme.typography.labelLarge)
                Text(
                    "Entradas: ${fila.registradoEntradas} de ${fila.porticoEntradas} · " +
                        "diferencia ${fila.diferenciaEntradas}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Salidas: ${fila.registradoSalidas} de ${fila.porticoSalidas} · " +
                        "diferencia ${fila.diferenciaSalidas}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Perfil de las entradas: género, edad y acompañantes niños. */
@Composable
fun PanelSegmentacion(resumen: ResumenGeneral, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Perfil de las entradas", style = MaterialTheme.typography.titleMedium)
            BarrasSegmentacion("Género", resumen.generoEntradas)
            HorizontalDivider()
            BarrasSegmentacion("Rango etario", resumen.edadEntradas)
            HorizontalDivider()
            Text("Acompañantes niños", style = MaterialTheme.typography.titleSmall)
            Text(
                "${resumen.totalNinos} niños registrados · " +
                    "${(resumen.pctConNinos * 100).roundToInt()}% de las personas entró con niños",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun BarrasSegmentacion(titulo: String, datos: Map<String, Int>) {
    val maximo = (datos.values.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall)
        datos.forEach { (etiqueta, cantidad) ->
            val fraccion by animateFloatAsState(cantidad.toFloat() / maximo, label = "${titulo}_$etiqueta")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(etiqueta, Modifier.width(92.dp), style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(progress = { fraccion }, modifier = Modifier.weight(1f))
                Text(cantidad.toString(), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
