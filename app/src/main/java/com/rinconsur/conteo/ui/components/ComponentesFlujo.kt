package com.rinconsur.conteo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rinconsur.conteo.R
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.model.CrucePorticos
import com.rinconsur.conteo.model.ResumenAcceso
import com.rinconsur.conteo.model.ResumenGeneral
import com.rinconsur.conteo.ui.utils.comoPorcentaje

private val ANCHO_ETIQUETA_SEGMENTACION = 92.dp

@Composable
internal fun DatoNumerico(etiqueta: String, valor: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor.toString(), style = MaterialTheme.typography.headlineMedium)
        Text(etiqueta, style = MaterialTheme.typography.labelMedium)
    }
}

/** Tarjeta por acceso: entradas, salidas, aforo neto y conversión de compra. */
@Composable
internal fun TarjetaAcceso(resumen: ResumenAcceso, modifier: Modifier = Modifier) {
    val progreso by animateFloatAsState(targetValue = resumen.pctCompra, label = "pctCompra")

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Accesos.etiqueta(resumen.accesoId), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                DatoNumerico(stringResource(R.string.etiqueta_entradas), resumen.personasEntradas)
                DatoNumerico(stringResource(R.string.etiqueta_salidas), resumen.personasSalidas)
                DatoNumerico(stringResource(R.string.etiqueta_aforo_neto), resumen.aforoNeto)
            }
            LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth())
            Text(
                stringResource(
                    R.string.acceso_compra_resumen,
                    resumen.pctCompra.comoPorcentaje(),
                    resumen.ninosEntradas
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Comparación entre lo que marca el pórtico y lo registrado desde las cámaras. El
 * pórtico solo cuenta tránsitos, sin separar entrada de salida, así que la
 * comparación se hace sobre el total de cada acceso.
 */
@Composable
internal fun PanelCrucePorticos(cruce: CrucePorticos, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.etiqueta_porticos_vs_registro),
                style = MaterialTheme.typography.titleMedium
            )

            if (!cruce.hayDatosPortico) {
                Text(
                    stringResource(R.string.panel_porticos_sin_datos),
                    style = MaterialTheme.typography.bodySmall
                )
                return@Column
            }

            val cobertura = cruce.cobertura
            if (cobertura != null) {
                val progreso by animateFloatAsState(
                    targetValue = cobertura.coerceIn(0f, 1f),
                    label = "cobertura"
                )
                LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(
                        R.string.panel_porticos_cobertura,
                        cobertura.comoPorcentaje(),
                        cruce.totalRegistrado,
                        cruce.totalPorticoPersonas,
                        cruce.diferencia
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            cruce.filas.forEach { fila ->
                HorizontalDivider()
                Text(Accesos.etiqueta(fila.accesoId), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(
                        R.string.panel_porticos_fila,
                        fila.porticoPersonas,
                        fila.registradoTotal,
                        fila.diferencia
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    stringResource(
                        R.string.panel_porticos_composicion,
                        fila.registradoEntradas,
                        fila.registradoSalidas
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Perfil de las entradas (género, característica etaria y acompañantes niños) y
 * resultado de compra declarado en las salidas.
 */
@Composable
internal fun PanelSegmentacion(resumen: ResumenGeneral, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.perfil_titulo),
                style = MaterialTheme.typography.titleMedium
            )
            BarrasSegmentacion(stringResource(R.string.etiqueta_genero), resumen.generoEntradas)
            HorizontalDivider()
            BarrasSegmentacion(
                stringResource(R.string.etiqueta_caracteristica_etaria),
                resumen.edadEntradas
            )
            HorizontalDivider()
            Text(
                stringResource(R.string.etiqueta_ninos_acompanantes),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                stringResource(
                    R.string.perfil_ninos_resumen,
                    resumen.totalNinos,
                    resumen.pctConNinos.comoPorcentaje()
                ),
                style = MaterialTheme.typography.bodySmall
            )
            HorizontalDivider()
            BarrasSegmentacion(
                stringResource(R.string.etiqueta_compra_salidas),
                linkedMapOf(
                    stringResource(R.string.etiqueta_compro) to resumen.totalCompradores,
                    stringResource(R.string.etiqueta_no_compro) to resumen.totalNoCompradores,
                    stringResource(R.string.etiqueta_sin_dato) to resumen.totalSinDatoCompra
                )
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
                Text(
                    etiqueta,
                    Modifier.width(ANCHO_ETIQUETA_SEGMENTACION),
                    style = MaterialTheme.typography.labelLarge
                )
                LinearProgressIndicator(progress = { fraccion }, modifier = Modifier.weight(1f))
                Text(cantidad.toString(), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
