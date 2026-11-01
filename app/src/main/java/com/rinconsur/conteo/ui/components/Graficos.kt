package com.rinconsur.conteo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rinconsur.conteo.R
import com.rinconsur.conteo.model.Accesos
import com.rinconsur.conteo.model.CrucePorticos
import com.rinconsur.conteo.ui.utils.comoPorcentaje

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
private fun BarraVertical(
    valor: Int,
    maximo: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
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
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
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

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                LinearProgressIndicator(
                    progress = { progreso },
                    modifier = Modifier.fillMaxWidth()
                )
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
