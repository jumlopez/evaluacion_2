# AppRincon_Grupo1 — Flujo por acceso, Tienda Departamental Rincón Sur

MVP para la asignatura **DSY1105 Desarrollo de Aplicaciones Móviles** (CITT Plaza Norte).
Resuelve el caso *"Segmentación del flujo de clientes por acceso en tienda de retail"* registrando,
en cada uno de los **4 accesos**, las **entradas y salidas**, el **género**, el **rango etario**,
los **niños acompañantes** y si la persona **compró o no**.

Los accesos se distribuyen así: **1, 2 y 3 en el primer piso; 4 en el segundo piso**.

## Cómo funciona en la tienda

La tienda ya cuenta con **pórticos** (torniquetes de conteo) en cada acceso, que entregan el
número de personas que pasan. Esos pórticos **no distinguen género, edad ni acompañantes**, por
lo que la app cumple dos roles complementarios:

1. **Captura rápida (operador de cámaras)**: una persona frente a las cámaras de seguridad observa
   a los clientes y registra, con botones e iconos, cuántas personas entran o salen por acceso,
   su género, el rango etario y si vienen con niños. Un toque por persona, sin formularios largos.
2. **Conteos de pórticos**: al cierre de cada bloque horario se digitan las lecturas de entradas y
   salidas que entregaron los pórticos de cada acceso.

El **Dashboard** cruza ambas fuentes: muestra el conteo del pórtico, lo registrado por el operador,
la diferencia entre ambos y el **porcentaje de cobertura** (qué proporción del flujo real quedó
caracterizada). También calcula aforo neto, conversión de compra, distribución por género/edad y
niños acompañantes.

> **Privacidad**: el operador solo registra **conteos agregados** (cuántos hombres, mujeres, otros,
> niños). La app no guarda nombres, RUT, rostros ni imágenes de las personas.

**No hay datos ficticios ni siembra automática**: la base arranca vacía y solo contiene lo que el
operador digita. Tampoco hay backend ni sincronización remota: **Room/SQLite es el único almacén**.

## Stack

| Capa | Tecnología |
|---|---|
| Frontend | Kotlin, Android Studio, Jetpack Compose, Material 3, MVVM |
| Persistencia | **Room / SQLite como único almacén de datos** (+ DataStore Preferences para el modo de análisis) |
| Pruebas | JUnit 4 (validaciones, cálculo de resúmenes y cruce con pórticos) |

> La carpeta `backend/` (Spring Boot) queda como legado del MVP anterior y no participa del flujo
> actual: la app no la usa ni la necesita.

## Modelo de datos

### `registros_flujo`

| Campo | Descripción |
|---|---|
| `accesoId` | 1–4 (el catálogo define el piso de cada acceso) |
| `fechaHora` | Momento del registro |
| `movimiento` | `ENTRADA` o `SALIDA` |
| `genero` | `FEMENINO`, `MASCULINO`, `OTRO`, `NO_DECLARA` |
| `rangoEtario` | `<18`, `18-29`, `30-44`, `45-59`, `60+`, `No declara` |
| `cantidad` | Personas del grupo con ese movimiento/género/rango |
| `ninosAcompanantes` | Niños adicionales que acompañan al grupo (0–20) |
| `compro` | Solo en entradas: `true`/`false`; en salidas queda `null` |

Un solo guardado genera **una fila por género** con cantidad mayor que cero; los niños acompañantes
se descuentan una sola vez (en la primera fila) para no duplicar el conteo.

### `conteos_portico`

| Campo | Descripción |
|---|---|
| `accesoId` | 1–4 |
| `fechaMillis` | Inicio del día (una lectura por acceso y día) |
| `entradas` / `salidas` | Personas contadas por el pórtico |
| `actualizadoEn` | Última edición del registro |

Índice único `(accesoId, fechaMillis)`: volver a guardar el día **actualiza** la lectura en lugar
de duplicarla.

### Migraciones

La base `rincon_flujo.db` está en la **versión 3**:

- **v1 → v2**: transforma el esquema anterior (`tipo`) en `movimiento` + `compro`.
- **v2 → v3**: crea `conteos_portico` con su índice único.

Las migraciones están registradas en `AppDatabase`, por lo que una instalación existente conserva
sus registros al actualizar la app.

## Flujo funcional

1. **Dashboard**: entradas, salidas, aforo neto y % de conversión general; tarjetas por acceso con
   su piso, filtro por periodo (Hoy / 7 días / Todo) y estado vacío cuando aún no hay registros.
   Incluye el panel **Cruce con pórticos** (pórtico vs. registrado, diferencia y cobertura) y, con el
   *Modo análisis* activo o en pantallas expandidas, el perfil por género/edad de las entradas.
2. **Registro rápido**: se elige acceso (chips 1–4 con su piso) y movimiento (Entrada / Salida).
   Luego se toca la tarjeta del género para sumar personas (**+1** por toque, con −/+ para corregir),
   el botón **+1** de la tarjeta de niños acompañantes y, en las entradas, si compró (Sí / No / Sin
   dato). Un botón **Guardar N** confirma cuántas personas se registraron y limpia los contadores
   manteniendo el acceso y el movimiento para seguir capturando.
3. **Pórticos**: por cada acceso se digitan las entradas y salidas del día y se guardan con un solo
   botón; dejar ambos campos vacíos elimina la lectura del día.
4. **Modo análisis**: interruptor persistente (DataStore) con animaciones.
5. **Perfil**: foto del operador desde galería o cámara.

## Qué se tomó de cada guía

| Guía | Contenido aplicado | Dónde está |
|---|---|---|
| 7 — Proyecto MVVM + colaboración | Carpetas `ui/`, `viewmodel/`, `model/`, `data/` (repository), convención `AppNombre_GrupoX`, plan GitHub/Trello | estructura del proyecto, esta sección |
| 8 — Pantalla base | Scaffold, TopAppBar, Column/Row, Text, Button, Image, Modifier, colores de MaterialTheme | `DashboardScreen.kt`, `AppNavigation.kt`, `res/drawable/logo_rincon.xml` |
| 9 — Adaptabilidad | Window Size Classes (Compact / Medium / Expanded) con `obtenerWindowSizeClass()` | `ui/utils/WindowSizeUtils.kt`, `DashboardScreen.kt` |
| 10 — Navegación | `sealed class Screen`, `NavigationEvent`, `MainViewModel`, NavHost, Drawer, NavigationBar | paquete `navigation/`, `MainViewModel.kt` |
| 11 — Formularios | `RegistroUiState` / `RegistroErrores`, validación reactiva, ViewModel con API de incremento/decremento | `model/RegistroUiState.kt`, `RegistroViewModel.kt`, `RegistroScreen.kt` |
| 12 — Animaciones y estados | DataStore, `rememberSaveable`, `derivedStateOf`, `animateColorAsState`, `AnimatedVisibility`, `AnimatedContent`, loader | `ModoViewModel.kt`, `ModoScreen.kt`, `ModoDataStore.kt` |
| 13 — Recurso nativo | Galería (`GetContent`) y cámara (`TakePicture`), `FileProvider`, `ImagenInteligente` circular | `PerfilViewModel.kt`, `PerfilScreen.kt`, `ui/components/ImagenInteligente.kt`, `res/xml/file_paths.xml` |

## Estructura del código

```
app/src/main/java/com/rinconsur/conteo/
├── data/local/          Room: AppDatabase, RegistroDao, ConteoPorticoDao
├── data/repository/     FlujoRepository (única puerta de entrada a los datos)
├── model/               RegistroFlujo, ConteoPortico, Resumen, CrucePorticos, RegistroUiState, Catalogos
├── viewmodel/           Dashboard, Registro, ConteoPorticos, Modo, Perfil, Main
├── ui/screens/          Dashboard, Registro, Porticos, Modo, Perfil
├── ui/components/       TarjetaAcceso, PanelCrucePorticos, ImagenInteligente
└── navigation/          Screen, AppNavigation
```

## Cómo ejecutar

**App**: abrir la carpeta raíz en Android Studio (Ladybug o superior), esperar la sincronización
de Gradle y ejecutar en un emulador API 33+. No se necesita backend ni conexión a Internet.
Para la cámara (Guía 13) usa un teléfono real.

```bash
./gradlew :app:testDebugUnitTest    # pruebas unitarias (Windows: gradlew.bat)
./gradlew :app:assembleDebug        # APK de depuración
```

El wrapper de Gradle ya está incluido en el repositorio.

## Plan de trabajo en pareja (GitHub + Trello)

Tablero Trello con listas **Por hacer / En curso / Finalizado**. Commits y ramas sugeridos, tomados de las guías:

| Rama | Mensaje de commit |
|---|---|
| `main` | Inicio de proyecto + estructura base MVVM |
| `feature/home-screen` | Pantalla HomeScreen con estructura Scaffold |
| `feature/navegacion` | Navegación y menú generado |
| `feature/registro-formulario` | Captura rápida con botones e iconos |
| `feature/porticos` | Conteos de pórticos y cruce de información |
| `feature/estado-animaciones` | Estado y animaciones |
| `feature/imagen-inteligente` | Funcionalidad cámara y galería integrada |

Checklist de la tarjeta de cámara (Guía 13): ViewModel, ImagenInteligente, Galería, Cámara, Test en físico, GitHub, Evidencia.
Evidencias a adjuntar: capturas del Dashboard (compact y expanded) con el cruce de pórticos, registro rápido con datos reales, pantalla de pórticos, imagen desde galería y desde cámara.

## Notas

- Cambié `lifecycle-viewmodel-compose` de `2.8.0-rc02` (de las guías) a la versión estable `2.8.6`.
- La galería usa el selector del sistema (`GetContent`), por eso no pide permisos de almacenamiento; la cámara sí pide `CAMERA` en tiempo de ejecución.
- Se agregó `material-icons-extended` para los iconos de género, niños y movimientos usados en la captura rápida.
- Los conteos de pórticos se ingresan **manualmente** por ahora; si la tienda expone una API o
  exportación CSV de los pórticos, el siguiente paso es cargarlos en `conteos_portico` sin cambiar
  el resto de la app.
