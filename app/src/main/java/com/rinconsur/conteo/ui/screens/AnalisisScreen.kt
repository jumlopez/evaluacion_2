package com.rinconsur.conteo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinconsur.conteo.R
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.model.CrucePorticos
import com.rinconsur.conteo.model.Genero
import com.rinconsur.conteo.model.Periodo
import com.rinconsur.conteo.model.RangosEtarios
import com.rinconsur.conteo.model.ResumenGeneral
import com.rinconsur.conteo.ui.components.ColoresGrafico
import com.rinconsur.conteo.ui.components.DatoNumerico
import com.rinconsur.conteo.ui.components.GraficoBarrasAgrupadas
import com.rinconsur.conteo.ui.components.GraficoCobertura
import com.rinconsur.conteo.ui.components.GraficoDona
import com.rinconsur.conteo.ui.components.SegmentoDona
import com.rinconsur.conteo.ui.components.SerieGrafico
import com.rinconsur.conteo.ui.utils.comoPorcentaje
import com.rinconsur.conteo.viewmodel.DashboardViewModel

private const val DURACION_ENTRADA_MS = 260
private const val DURACION_DESPLAZAMIENTO_MS = 300
private const val RETARDO_ENTRE_TARJETAS_MS = 70

/**
 * Pantalla de gráficos del modo análisis. Se abre desde el botón "Ver gráficos" y
 * muestra con transiciones la misma información del dashboard, pero en formato de
 * gráficos: flujo por acceso, género, característica etaria, compra en las salidas,
 * acompañantes niños y cobertura de los pórticos.
 */
@Composable
fun AnalisisScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val periodo by viewModel.periodo.collectAsStateWithLifecycle()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    if (estado.cargando) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val resumen = estado.resumen
    val cruce = estado.cruce

    val tarjetas: List<@Composable () -> Unit> = listOf(
        { TarjetaFlujoPorAcceso(resumen) },
        { TarjetaGenero(resumen) },
        { TarjetaEdad(resumen) },
        { TarjetaCompra(resumen) },
        { TarjetaNinos(resumen) },
        { TarjetaPorticos(cruce) }
    )

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.pantalla_analisis),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    stringResource(R.string.analisis_descripcion),
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Periodo.entries.forEach { p ->
                        FilterChip(
                            selected = p == periodo,
                            onClick = { viewModel.cambiarPeriodo(p) },
                            label = { Text(p.etiqueta) }
                        )
                    }
                }
            }
        }

        if (resumen.sinDatos) {
            item {
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(DURACION_DESPLAZAMIENTO_MS))) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                stringResource(R.string.analisis_sin_datos_titulo),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.analisis_sin_datos_descripcion),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        } else {
            items(tarjetas.size) { indice ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(
                        tween(
                            durationMillis = DURACION_ENTRADA_MS,
                            delayMillis = indice * RETARDO_ENTRE_TARJETAS_MS
                        )
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = DURACION_DESPLAZAMIENTO_MS,
                            delayMillis = indice * RETARDO_ENTRE_TARJETAS_MS
                        )
                    ) { alto -> alto / 8 }
                ) {
                    tarjetas[indice]()
                }
            }
        }
    }
}

@Composable
private fun TarjetaGrafico(
    titulo: String,
    subtitulo: String,
    contenido: @Composable () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(subtitulo, style = MaterialTheme.typography.bodySmall)
            }
            contenido()
        }
    }
}

@Composable
private fun TarjetaFlujoPorAcceso(resumen: ResumenGeneral) {
    TarjetaGrafico(
        titulo = stringResource(R.string.grafico_personas_por_acceso),
        subtitulo = stringResource(R.string.grafico_personas_por_acceso_sub)
    ) {
        GraficoBarrasAgrupadas(
            categorias = resumen.porAcceso.map { stringResource(R.string.grafico_acceso_corto, it.accesoId) },
            series = listOf(
                SerieGrafico(
                    nombre = stringResource(R.string.etiqueta_entradas),
                    color = ColoresGrafico.entradas,
                    valores = resumen.porAcceso.map { it.personasEntradas }
                ),
                SerieGrafico(
                    nombre = stringResource(R.string.etiqueta_salidas),
                    color = ColoresGrafico.salidas,
                    valores = resumen.porAcceso.map { it.personasSalidas }
                )
            )
        )
    }
}

@Composable
private fun TarjetaGenero(resumen: ResumenGeneral) {
    val segmentos = resumen.generoEntradas
        .filterValues { it > 0 }
        .entries
        .mapIndexed { indice, (etiqueta, valor) ->
            SegmentoDona(
                etiqueta = etiqueta,
                valor = valor,
                color = when (etiqueta) {
                    Genero.FEMENINO.etiqueta -> ColoresGrafico.femenino
                    Genero.MASCULINO.etiqueta -> ColoresGrafico.masculino
                    else -> ColoresGrafico.paraIndice(indice)
                }
            )
        }

    TarjetaGrafico(
        titulo = stringResource(R.string.grafico_genero),
        subtitulo = stringResource(R.string.grafico_genero_sub)
    ) {
        GraficoDona(segmentos, etiquetaCentro = stringResource(R.string.grafico_centro_entradas))
    }
}

@Composable
private fun TarjetaEdad(resumen: ResumenGeneral) {
    val segmentos = resumen.edadEntradas
        .filterValues { it > 0 }
        .entries
        .mapIndexed { indice, (etiqueta, valor) ->
            SegmentoDona(
                etiqueta = etiqueta,
                valor = valor,
                color = when (etiqueta) {
                    RangosEtarios.JOVEN -> ColoresGrafico.joven
                    RangosEtarios.ADULTO -> ColoresGrafico.adulto
                    RangosEtarios.ADULTO_MAYOR -> ColoresGrafico.adultoMayor
                    else -> ColoresGrafico.paraIndice(indice)
                }
            )
        }

    TarjetaGrafico(
        titulo = stringResource(R.string.etiqueta_caracteristica_etaria),
        subtitulo = stringResource(R.string.grafico_etaria_sub)
    ) {
        GraficoDona(segmentos, etiquetaCentro = stringResource(R.string.grafico_centro_entradas))
    }
}

@Composable
private fun TarjetaCompra(resumen: ResumenGeneral) {
    val segmentos = listOf(
        SegmentoDona(
            stringResource(R.string.etiqueta_compro),
            resumen.totalCompradores,
            ColoresGrafico.compro
        ),
        SegmentoDona(
            stringResource(R.string.etiqueta_no_compro),
            resumen.totalNoCompradores,
            ColoresGrafico.noCompro
        ),
        SegmentoDona(
            stringResource(R.string.etiqueta_sin_dato),
            resumen.totalSinDatoCompra,
            ColoresGrafico.sinDato
        )
    )

    TarjetaGrafico(
        titulo = stringResource(R.string.etiqueta_compra_salidas),
        subtitulo = stringResource(R.string.grafico_compra_sub, resumen.pctCompra.comoPorcentaje())
    ) {
        GraficoDona(segmentos, etiquetaCentro = stringResource(R.string.grafico_centro_salidas))
    }
}

@Composable
private fun TarjetaNinos(resumen: ResumenGeneral) {
    TarjetaGrafico(
        titulo = stringResource(R.string.etiqueta_ninos_acompanantes),
        subtitulo = stringResource(R.string.grafico_ninos_sub)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            DatoNumerico(stringResource(R.string.grafico_ninos_entraron), resumen.totalNinos)
            DatoNumerico(stringResource(R.string.grafico_ninos_salieron), resumen.totalNinosSalidas)
            DatoNumerico(stringResource(R.string.grafico_entraron_con_ninos), resumen.personasConNinos)
        }
        Text(
            stringResource(R.string.grafico_ninos_pct, resumen.pctConNinos.comoPorcentaje()),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun TarjetaPorticos(cruce: CrucePorticos) {
    TarjetaGrafico(
        titulo = stringResource(R.string.etiqueta_porticos_vs_registro),
        subtitulo = stringResource(R.string.grafico_porticos_sub)
    ) {
        if (!cruce.hayDatosPortico) {
            Text(
                stringResource(R.string.grafico_porticos_sin_datos),
                style = MaterialTheme.typography.bodySmall
            )
            return@TarjetaGrafico
        }

        GraficoCobertura(cruce)
        val cobertura = cruce.cobertura
        Text(
            if (cobertura == null) {
                stringResource(
                    R.string.grafico_porticos_sin_cobertura,
                    cruce.totalRegistrado,
                    cruce.totalPorticoPersonas
                )
            } else {
                stringResource(
                    R.string.grafico_porticos_resumen,
                    cruce.totalRegistrado,
                    cruce.totalPorticoPersonas,
                    cobertura.comoPorcentaje(),
                    cruce.diferencia
                )
            },
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            stringResource(
                R.string.grafico_porticos_accesos,
                cruce.filas.count { it.porticoPersonas > 0 },
                Accesos.ids.size
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
