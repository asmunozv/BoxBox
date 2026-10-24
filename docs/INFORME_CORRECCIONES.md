# BoxBox — correcciones y verificación de la continuación

Fecha: 23 de octubre de 2026.

## Resultado entregado

### Puntuación comprensible y contrastada

`RaceScoring` explica los totales publicados. La victoria con escala completa vale
25 puntos. La instantánea de Baréin 2024 identifica a Max como autor de la vuelta
rápida; se muestran **25 + 1 VR = 26**, también en la narrativa. Desde 2025 no se
otorga ese extra. Sin evidencia de un desglose, se conserva el total oficial y se
explica la ausencia. Se verificaron casos de puntos reducidos, escalas antiguas,
sprint, 2025 y falta de rango de vuelta rápida.

Contraste: https://www.formula1.com/en/results/2024/races/1229/bahrain/race-result .

### Colección multimedia recuperada en el explorador

Seleccionar un resultado muestra la foto local y permite escuchar/detener el audio.
Los reproductores se liberan al cambiar de piloto, módulo o sesión. La resolución
de recursos también funciona desde una carpeta de ejecución diferente a BoxBox y
reconoce alias de identificadores. Los perfiles anteriores usan la misma resolución.
Una ausencia muestra una silueta propia o deshabilita Escuchar con un mensaje.

### Uso guiado y navegación

- **Cómo usar BoxBox** explica un recorrido de tres pasos con accesos funcionales.
- El resumen prioriza las columnas principales; Q1/Q2/Q3 se activan con una casilla.
- **Abrir demo · Baréin 2024** está separado de **Añadir datos con internet**.
- Barra lateral y pestaña activa se mantienen sincronizadas; Explorador vuelve a Resumen.
- Una temporada sin datos limpia resultados, fuentes y ficha y deshabilita los análisis
  de la carrera anterior. Abrir la demo restaura un contexto válido.
- Tablas y gráficos conservan alturas legibles y desplazamiento vertical en ventanas
  de 1280×800 y 1000×650. El comparador comienza con líderes de la carrera seleccionada.

### Simulador visual

Filas de parada con vuelta, compuesto y edad del juego; botones de añadir, quitar,
probar sin paradas, adelantar la primera tres vueltas y restablecer. Admite coma o
punto decimal. Los cambios invalidan visualmente el resultado anterior hasta volver
a comparar. El resultado muestra delta, interpretación, tiempos modelados y gráfica;
los parámetros avanzados, error descriptivo, sensibilidad y supuestos son desplegables.
La edición por texto continúa como opción técnica, no como requisito de uso.

## Archivos principales

Rutas relativas a la raíz de BoxBox:

| Área | Archivos |
|---|---|
| Puntuación | `src/model/history/RaceScoring.java`, `RaceData.java`, `HistoryParser.java`, `RaceInsights.java` |
| Recursos | `src/model/util/LocalResources.java`, `src/view/LocalImageService.java`, `src/view/AppAudio.java` |
| Ficha multimedia | `src/controller/DriverMediaCard.java`, `src/view/DriverMediaCard.fxml` |
| Explorador y navegación | `src/controller/HistoricalExplorerController.java`, `MainViewController.java`, `RaceLabController.java`, `ComparisonViewController.java`; `src/view/HistoricalExplorer.fxml`, `ComparisonView.fxml` |
| Guía | `src/controller/GuideViewController.java`, `src/view/GuideView.fxml` |
| Simulador | `src/controller/SimulationViewController.java`, `src/view/SimulationView.fxml` |
| Vistas y medios anteriores | `src/controller/DriverViewController.java`, `DriverDetailsViewController.java`, `ConstructorDetailsViewController.java`, `CircuitDetailsViewController.java`, `RaceDetailsViewController.java`, `RaceViewController.java`, `LoginViewController.java` |
| Legibilidad | `src/view/TyreView.fxml`, `PitView.fxml`, `PositionsView.fxml`, `src/application/application.css` |
| Verificación | `tools/VerifyBoxBox.java`, `tools/verification/AnalysisChecks.java`, `HistoricalExplorerSmoke.java`, `ResourceChecks.java` |
| Documentación | `README.md`, `docs/AVANCE_BE_A_DRAGON.md`, `DEMO_CONCURSO.md`, `RECURSOS_MULTIMEDIA.md`, este informe |

## Pruebas ejecutadas y resultados

Desde la raíz de BoxBox, con JDK 26 y MySQL80 activo:

```powershell
java tools/BuildBoxBox.java
java tools/VerifyBoxBox.java --ui --gallery
```

| Verificador | Resultado observado |
|---|---|
| Compilación | `BUILD_OK: 84 Java sources; module boxbox; target Java 25` |
| `SecurityChecks` | PBKDF2 con salts distintos, acceso correcto/incorrecto, codificaciones inválidas y coste acotado |
| `AnalysisChecks` | **49 aserciones**: fórmulas, escenarios inválidos, estadísticas, ritmo, stints, puntuación, datos reales 2024 e idempotencia |
| `ArchiveFailureChecks` | Paginación completa aceptada; incompleta rechazada; FK inválida revierte la transacción sintética |
| `ExportDemo` | Archivo existente conservado, validación y reimportación local con **0 documentos nuevos** |
| `ResourceChecks` | **22 fotos decodificadas**, 22 audios localizados, alias, mapa, música/video del login, ejecución desde carpeta distinta |
| `HistoricalExplorerSmoke` | Filtros, desglose P1, Q1–Q3, comparador, neumáticos, pits, posiciones, editor guiado, errores, coma decimal, guía, selección vacía y demo |
| Multimedia histórica | Foto original de Max válida, audio en **PLAYING**, Detener y cambio de pestaña liberan el reproductor |
| `ApplicationSmoke` | Login real; audio/video en **PLAYING**; 22 pilotos, 11 escuderías, 28 resultados locales; perfiles, tablas de campeonato, mapa, nueve casillas de bingo, arcade/teclado y logout |
| Suite | `VERIFICATION_SUITE_PASSED: 7 verificadores reales; sin acceso de escritura al origen.` |

Última galería revisada:
`private/verification/gallery-2d7a67ad-7ebb-4e64-a45b-af5ec710812b/`.
Contiene las ocho pestañas, resumen/simulador compactos y guía. Es salida privada,
no parte del repositorio. Los tests sintéticos están identificados como tales.

La comprobación Git de recursos, dataset, SQL y configuración Eclipse no mostró
diferencias. Esta continuación usa el acceso restringido a `boxbox_db`; no reejecuta
la restauración ni la comparación administrativa completa de bases originales.

## Recuperación y ejecución desde Eclipse

Punto de recuperación: `fc1fa5d`. Módulos ya registrados: puntuación `22c0c86`,
multimedia `77e877f`. La guía/editor visual se registra al cerrar esta entrega.

1. Iniciar el servicio **MySQL80**.
2. En Eclipse, **Refresh** sobre **BoxBox**, y **Project → Clean** si hay una salida
   anterior. Usar el JDK 26 registrado y las bibliotecas locales de `.classpath`.
3. Ejecutar `BoxBox.launch` como **Java Application**. Su configuración fija módulo
   `boxbox`, directorio de BoxBox y acceso nativo JavaFX.
4. Entrar con una cuenta de aplicación; abrir **Cómo usar BoxBox** y la demo.

La configuración privada existente sigue en `private/config/database.properties`;
sus valores no se incluyen en este informe. Alternativa desde terminal:
`java tools/BuildBoxBox.java --run`.

## Límites pendientes

- El detalle completo incluido en la demo corresponde a Baréin 2024. Otras carreras
  requieren preparación explícita y cobertura de las fuentes.
- No se reconstruyen tiempos estacionarios ausentes ni posiciones finales hipotéticas.
  El modelo sigue sin tráfico, respuestas rivales, lluvia o neutralizaciones.
- JDK 26/JavaFX 21.0.11 emiten advertencias de APIs obsoletas/genéricos y `Unsafe`
  del renderizador. La compilación y las pruebas terminaron correctamente.
- La utilidad educativa y precisión predictiva externa siguen pendientes de estudios.
  Las licencias individuales de los medios heredados requieren revisión para distribución.
- Esta suite carga bingo y comprueba las pantallas anteriores; no presenta el registro
  previo de CRUD con fixtures administrativos como una nueva prueba de esta entrega.
