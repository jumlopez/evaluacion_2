package com.rinconsur.conteo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.annotation.StringRes
import com.rinconsur.conteo.R
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.model.CrucePorticos
import com.rinconsur.conteo.model.ResumenAcceso
import com.rinconsur.conteo.model.ResumenGeneral
import kotlinx.coroutines.delay

private val ANCHO_ETIQUETA_SEGMENTACION = 92.dp
private const val DURACION_CIERRE_POPUP_MS = 140L

// ---------------------------------------------------------------- estructura

/** Indicador de carga centrado, compartido por todas las pantallas. */
@Composable
internal fun Cargando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Título y descripción que encabezan cada pantalla. */
@Composable
internal fun EncabezadoPantalla(@StringRes titulo: Int, @StringRes descripcion: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(titulo), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(descripcion), style = MaterialTheme.typography.bodySmall)
    }
}

/** Contenedor con scroll y snackbar flotante que usan las pantallas de captura. */
@Composable
internal fun PantallaConSnackbar(
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = contenido
        )
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

// ---------------------------------------------------------------- tarjetas

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

            cruce.cobertura?.let { cobertura ->
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
            Text(stringResource(R.string.perfil_titulo), style = MaterialTheme.typography.titleMedium)
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
            val fraccion by animateFloatAsState(
                cantidad.toFloat() / maximo,
                label = "${titulo}_$etiqueta"
            )
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

// ---------------------------------------------------------------- popups

/**
 * Contenedor animado común a los popups: escala + fade al entrar y al salir. El
 * cierre real (cambio de estado del padre) se avisa cuando termina la animación.
 */
@Composable
internal fun PopupAnimado(
    onDescartar: () -> Unit,
    contenido: @Composable (cerrar: () -> Unit) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var cerrando by remember { mutableStateOf(false) }
    val cerrar: () -> Unit = {
        cerrando = true
        visible = false
    }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(cerrando) {
        if (cerrando) {
            delay(DURACION_CIERRE_POPUP_MS)
            onDescartar()
        }
    }

    Dialog(onDismissRequest = cerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(140)) + scaleIn(tween(180), initialScale = 0.85f),
            exit = fadeOut(tween(110)) + scaleOut(tween(120), targetScale = 0.9f)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                contenido(cerrar)
            }
        }
    }
}

/** Encabezado compartido por los popups: icono, título, subtítulo y botón de cierre. */
@Composable
internal fun EncabezadoPopup(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    onCerrar: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icono, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onCerrar) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.nav_cerrar))
        }
    }
}

// ---------------------------------------------------------------- gráficos

/** Paleta compartida por los gráficos del modo análisis. */
internal object ColoresGrafico {
    val entradas = Color(0xFF1E88E5)
    val salidas = Color(0xFFFB8C00)
    val femenino = Color(0xFFEC407A)
    val masculino = Color(0xFF42A5F5)
    val joven = Color(0xFF66BB6A)
    val adulto = Color(0xFF7E57C2)
    val adultoMayor = Color(0xFF8D6E63)
    val compro = Color(0xFF2E7D32)
    val noCompro = Color(0xFFC62828)
    val sinDato = Color(0xFF9E9E9E)
    val neutro = Color(0xFFE0E0E0)
    private val piso1 = Color(0xFF26A69A)
    private val piso2 = Color(0xFFAB47BC)

    private val serie = listOf(femenino, masculino, joven, adulto, adultoMayor, piso2, piso1)

    fun paraIndice(indice: Int): Color = serie[indice % serie.size]
}

/** Una serie del gráfico de barras: nombre, color y un valor por categoría. */
internal data class SerieGrafico(val nombre: String, val color: Color, val valores: List<Int>)

/** Un segmento del gráfico de dona. */
internal data class SegmentoDona(val etiqueta: String, val valor: Int, val color: Color)

/**
 * Barras verticales agrupadas por categoría (por ejemplo, entradas y salidas de
 * cada acceso). Las barras crecen con animación al aparecer y al cambiar el periodo.
 */
@Composable
internal fun GraficoBarrasAgrupadas(
    categorias: List<String>,
    series: List<SerieGrafico>,
    modifier: Modifier = Modifier,
    altura: Dp = 170.dp
) {
    val maximo = series.flatMap { it.valores }.maxOrNull() ?: 0

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(altura),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            categorias.forEachIndexed { indice, categoria ->
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        series.forEach { serie ->
                            BarraVertical(
                                valor = serie.valores.getOrElse(indice) { 0 },
                                maximo = maximo,
                                color = serie.color,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        categoria,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LeyendaGrafico(series.map { it.nombre to it.color })
    }
}

@Composable
private fun BarraVertical(valor: Int, maximo: Int, color: Color, modifier: Modifier = Modifier) {
    val fraccion by animateFloatAsState(
        targetValue = if (maximo <= 0) 0f else (valor.toFloat() / maximo).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "barraVertical"
    )

    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (valor > 0) {
            Text(valor.toString(), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraccion)
                    .background(color, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            )
        }
    }
}

/** Dona con el total al centro y una leyenda con el valor de cada segmento. */
@Composable
internal fun GraficoDona(
    segmentos: List<SegmentoDona>,
    modifier: Modifier = Modifier,
    diametro: Dp = 132.dp,
    etiquetaCentro: String? = null
) {
    val textoCentro = etiquetaCentro ?: stringResource(R.string.etiqueta_personas)
    val total = segmentos.sumOf { it.valor }
    val progreso by animateFloatAsState(
        targetValue = if (total == 0) 0f else 1f,
        animationSpec = tween(durationMillis = 700),
        label = "dona"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.size(diametro), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val grosor = size.minDimension * 0.2f
                val inset = grosor / 2f
                val arco = Rect(inset, inset, size.width - inset, size.height - inset)
                if (total == 0) {
                    drawArc(
                        color = ColoresGrafico.neutro,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arco.topLeft,
                        size = arco.size,
                        style = Stroke(width = grosor)
                    )
                } else {
                    var inicio = -90f
                    segmentos.filter { it.valor > 0 }.forEach { segmento ->
                        val barrido = 360f * segmento.valor / total * progreso
                        drawArc(
                            color = segmento.color,
                            startAngle = inicio,
                            sweepAngle = barrido,
                            useCenter = false,
                            topLeft = arco.topLeft,
                            size = arco.size,
                            style = Stroke(width = grosor, cap = StrokeCap.Butt)
                        )
                        inicio += barrido
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$total", style = MaterialTheme.typography.headlineSmall)
                Text(textoCentro, style = MaterialTheme.typography.labelSmall)
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            segmentos.forEach { segmento ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.size(10.dp).background(segmento.color, CircleShape))
                    Text(
                        segmento.etiqueta,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(segmento.valor.toString(), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Barras horizontales de cobertura: personas registradas sobre las del pórtico. */
@Composable
internal fun GraficoCobertura(cruce: CrucePorticos, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cruce.filas.forEach { fila ->
            val cobertura = fila.cobertura
            val progreso by animateFloatAsState(
                targetValue = (cobertura ?: 0f).coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 650),
                label = "coberturaAcceso"
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(Accesos.etiqueta(fila.accesoId), style = MaterialTheme.typography.labelLarge)
                    Text(
                        stringResource(
                            R.string.grafico_cobertura_valores,
                            fila.registradoTotal,
                            fila.porticoPersonas
                        ),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth())
                Text(
                    if (cobertura == null) {
                        stringResource(R.string.grafico_cobertura_sin_lectura)
                    } else {
                        stringResource(
                            R.string.grafico_cobertura_fila,
                            cobertura.comoPorcentaje(),
                            fila.diferencia
                        )
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Leyenda de colores usada por los gráficos de barras. */
@Composable
private fun LeyendaGrafico(items: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { (nombre, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Text(nombre, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
