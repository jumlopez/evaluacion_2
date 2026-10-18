package com.rinconsur.conteo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinconsur.conteo.R
import com.rinconsur.conteo.model.CrucePorticos
import com.rinconsur.conteo.model.Periodo
import com.rinconsur.conteo.model.ResumenGeneral
import com.rinconsur.conteo.ui.components.DatoNumerico
import com.rinconsur.conteo.ui.components.PanelCrucePorticos
import com.rinconsur.conteo.ui.components.PanelSegmentacion
import com.rinconsur.conteo.ui.components.TarjetaAcceso
import com.rinconsur.conteo.ui.utils.obtenerWindowSizeClass
import com.rinconsur.conteo.viewmodel.DashboardViewModel
import com.rinconsur.conteo.viewmodel.ModoViewModel
import kotlin.math.roundToInt

/**
 * Guías 8 y 9: pantalla principal adaptativa.
 * Compact -> 1 columna | Medium -> 2 columnas | Expanded -> 2 columnas + panel lateral.
 */
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(),
    modoViewModel: ModoViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val periodo by viewModel.periodo.collectAsState()
    val modoActivo by modoViewModel.activo.collectAsState()
    val ancho = obtenerWindowSizeClass().widthSizeClass

    if (estado.cargando) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val resumen = estado.resumen
    val cruce = estado.cruce
    when (ancho) {
        WindowWidthSizeClass.Compact ->
            DashboardCompacto(
                modifier, resumen, cruce, periodo, viewModel::cambiarPeriodo, modoActivo == true
            )

        WindowWidthSizeClass.Medium ->
            DashboardMediano(
                modifier, resumen, cruce, periodo, viewModel::cambiarPeriodo, modoActivo == true
            )

        else ->
            DashboardExpandido(modifier, resumen, cruce, periodo, viewModel::cambiarPeriodo)
    }
}

@Composable
private fun Encabezado(resumen: ResumenGeneral, periodo: Periodo, onPeriodo: (Periodo) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(id = R.drawable.logo_rincon),
                contentDescription = "Logo Tienda Departamental Rincón Sur",
                modifier = Modifier.size(48.dp)
            )
            Column {
                Text("Tienda Departamental Rincón Sur", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Registro real de entradas, salidas y compra por acceso",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Periodo.entries.forEach { p ->
                FilterChip(selected = p == periodo, onClick = { onPeriodo(p) }, label = { Text(p.etiqueta) })
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    DatoNumerico("Entradas", resumen.totalPersonasEntradas)
                    DatoNumerico("Salidas", resumen.totalPersonasSalidas)
                    DatoNumerico("Aforo neto", resumen.aforoNeto)
                }
                Text(
                    "${(resumen.pctCompra * 100).roundToInt()}% de las salidas compró · " +
                        "${resumen.totalNinos} niños acompañantes",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (resumen.sinDatos) {
            Text(
                "Aún no hay registros en este periodo. Usa Registro rápido para capturar " +
                    "las personas que ves en las cámaras.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ---------- Compact: una columna ----------
@Composable
private fun DashboardCompacto(
    modifier: Modifier,
    resumen: ResumenGeneral,
    cruce: CrucePorticos,
    periodo: Periodo,
    onPeriodo: (Periodo) -> Unit,
    mostrarSegmentacion: Boolean
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Encabezado(resumen, periodo, onPeriodo) }
        items(resumen.porAcceso) { TarjetaAcceso(it) }
        item { PanelCrucePorticos(cruce) }
        if (mostrarSegmentacion) item {
            PanelSegmentacion(
                resumen.generoEntradas,
                resumen.edadEntradas,
                resumen.totalNinos,
                resumen.pctConNinos
            )
        }
    }
}

// ---------- Medium: grilla de dos columnas ----------
@Composable
private fun DashboardMediano(
    modifier: Modifier,
    resumen: ResumenGeneral,
    cruce: CrucePorticos,
    periodo: Periodo,
    onPeriodo: (Periodo) -> Unit,
    mostrarSegmentacion: Boolean
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { Encabezado(resumen, periodo, onPeriodo) }
        items(resumen.porAcceso) { TarjetaAcceso(it) }
        item(span = { GridItemSpan(maxLineSpan) }) { PanelCrucePorticos(cruce) }
        if (mostrarSegmentacion) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                PanelSegmentacion(
                    resumen.generoEntradas,
                    resumen.edadEntradas,
                    resumen.totalNinos,
                    resumen.pctConNinos
                )
            }
        }
    }
}

// ---------- Expanded: grilla + panel lateral ----------
@Composable
private fun DashboardExpandido(
    modifier: Modifier,
    resumen: ResumenGeneral,
    cruce: CrucePorticos,
    periodo: Periodo,
    onPeriodo: (Periodo) -> Unit
) {
    Row(modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(2f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { Encabezado(resumen, periodo, onPeriodo) }
            items(resumen.porAcceso) { TarjetaAcceso(it) }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PanelSegmentacion(
                resumen.generoEntradas,
                resumen.edadEntradas,
                resumen.totalNinos,
                resumen.pctConNinos
            )
            PanelCrucePorticos(cruce)
        }
    }
}
