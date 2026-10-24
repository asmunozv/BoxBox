# Estado verificable de implementación

## Recuperación

Commit inicial seguro: `93f6214`. `private/`, credenciales, respaldos, binarios
generados y SDK local no se incluyen en Git. No hay publicación remota.

## Explorador histórico: primera entrega probada

- Modelos `model.history` sin dependencias JavaFX.
- Archivo MySQL aditivo `bb_races`/`bb_documents`, fuente, fecha, SHA-256 y FK.
- Instantáneas inmutables: insertar lo faltante, conservar registros existentes.
- Preparación explícita con HTTP, paginación validada, límites y timeout.
- Exploración, filtros y consultas exclusivamente desde MySQL; caché de hasta 8 carreras.
- Temporada, GP, piloto, escudería, búsqueda y sesión carrera/sprint.
- Resultados, parrilla, posición final, puntos oficiales y Q1/Q2/Q3 disponibles.
- Información de circuito, cobertura y fuentes visibles; estados vacíos y errores.
- Navegación lateral conservando las seis secciones anteriores.
- Importador de entrada sustituido por preparación no destructiva.

Datos realmente preparados: temporada 2024, 24 carreras, resultados, clasificación
cronometrada y seis sprints. Detalle de Baréin 2024: 20 pilotos, 1129 registros
vuelta/piloto, 63 stints, 43 pasos por pits, mensajes de control y ocho documentos
con procedencia. Los conteos son del archivo obtenido, no datos sintéticos.

Pruebas reales: compilación de 64 fuentes; preparación completa con validación de
paginación y conservación de los 26 puntos oficiales del ganador; prueba JavaFX
de selección local, filtros de piloto/equipo/búsqueda/sesión, estado vacío y fuentes.

La copia anterior de boxbox_db se conserva en
`private/backups/antes-historial-e1e62560-b082-438d-817e-ff40e9aa606b/`.
Los archivos del proyecto original se comprobaron sin cambios.

## Entregas analíticas anteriores

Entregas implementadas y verificadas durante el desarrollo anterior:

- Comparador piloto/escudería, métricas, curva de puntos y tabla por GP.
- Líneas de stints, muestras de ritmo y pendiente observada.
- Pits con duraciones separadas y detector heurístico de posibles undercut/overcut.
- Posiciones P1 arriba, visibilidad por piloto y puntos/pits interactivos.
- Simulador por vuelta, paradas, compuestos, edades, sensibilidad y validación.
- Explicaciones de comparación, stints, pits y narrativa de carrera con evidencia.
- Demo exportada y validada localmente sin HTTP.
- PBKDF2 y autenticación de fondo; transición aditiva sin sobrescribir contraseñas antiguas.
- Ciclo de vida para medios/animaciones, tema compartido y menú lateral desplazable.

Pruebas registradas en aquella entrega: 36 aserciones de cálculo/modelo y contraste real; seguridad
criptográfica; servidor HTTP de prueba con paginación completa/incompleta; rollback
de persistencia por FK inválida; importación demo idempotente; prueba JavaFX del
explorador y módulos analíticos; regresión de las seis funciones anteriores,
audio/video PLAYING y CRUD/registro/bingo con fixtures transitorios limpiados.

La comprobación de conservación usó el respaldo **de inicio de esta tarea**, no
reimpuso la copia original de independencia: los catálogos manuales de BoxBox
contenían cambios previos de uso que se conservaron. Las ocho tablas heredadas
coincidieron en definiciones e INSERT con ese respaldo después de limpiar pruebas.
También se comprobaron sin cambios los 280 archivos y la instantánea de la base original.

Pendientes reales de validación externa: pruebas con aficionados, ensayo presencial,
validación predictiva independiente del modelo y verificación de permisos individuales
de los medios heredados. La app no estima posiciones finales contrafactuales, ni
reconstruye compuestos/tiempos estacionarios donde la fuente no los ofrece.

La ejecución actual usa JDK 26 con JavaFX 21.0.11. Persisten advertencias de APIs
obsoletas/genéricos heredados y de Unsafe del renderizador; no impidieron las pruebas.
No se actualizó la tecnología ni se silenciaron errores para declarar éxito.

Las tablas nuevas se prepararon con la migración aditiva
`database/migrations/V001__historical_archive.sql`, y permisos SELECT/INSERT
específicos para boxbox_app. La cuenta de aplicación continúa sin privilegios DDL.

## Correcciones comprobadas — 23 de octubre de 2026

Punto de recuperación de esta continuación: `fc1fa5d`. Corrección de puntuación:
`22c0c86`. Integración multimedia: `77e877f`.

- Desglose de puntuación: **25 por P1 + 1 VR = 26** en Baréin 2024, con rango de
  vuelta rápida de la fuente; no se impone el extra desde 2025 ni se reescriben
  puntos reducidos/históricos sin evidencia.
- Fotos y audios originales integrados en el explorador; resolución independiente
  del directorio de lanzamiento y alias de pilotos. Ausencias locales informativas.
- Guía integrada con botones a demo, resultados, posiciones, historia y simulación.
- Editor visual de paradas, adelanto de tres vueltas, restablecimiento, cero paradas,
  coma decimal y resultados con interpretación sencilla y enfoque automático.
- Comparador con las ocho métricas legibles, selección inicial de líderes del GP,
  tablas/gráficas desplazables y ejes de rondas enteras. Navegación sincronizada.
- Temporadas sin datos limpian el contexto anterior y deshabilitan sus análisis;
  la demo permite recuperarse de ese estado.

Ejecución actual: **84 fuentes compiladas**, **49 aserciones analíticas**, y
**siete verificadores satisfactorios** con `java tools/VerifyBoxBox.java --ui --gallery`.
Se decodificaron **22 fotografías** y resolvieron **22 audios** de pilotos; el audio
de Max y el audio/video del login alcanzaron PLAYING. Los controles guiados,
filtros, demo, perfiles, clasificaciones, mapa, bingo cargado, arcade y logout
pasaron las comprobaciones JavaFX. Capturas revisadas a 1280×800 y 1000×650.

El detalle de comandos, archivos, resultados y límites está en
[INFORME_CORRECCIONES.md](INFORME_CORRECCIONES.md). El registro anterior de pruebas
CRUD y de igualdad completa con respaldos no se presenta como una nueva ejecución
de esta continuación.

## Fuentes y límites de interpretación

Jolpica-F1: https://api.jolpi.ca/ergast/f1/ ; OpenF1: https://openf1.org/docs/ .
Archivo de proveedor filtrado por carrera, conservando observaciones y metadatos de
paginación; no se mezclan estadísticas oficiales con simulaciones. Cobertura
histórica desigual: clasificación desde los años 1990, vueltas desde 1996,
pits desde 2011, OpenF1 desde 2023. Un campo ausente no se convierte en un tiempo cero.

El formato de archivo de respuestas externas es deliberado: metadatos relacionales,
documentos de procedencia inmutables con FK, JSON validado y texto conservado para
verificar su huella exacta. Los modelos de análisis se construyen en Java, permitiendo
reproducir cálculos y reutilizarlos en una futura interfaz web.
