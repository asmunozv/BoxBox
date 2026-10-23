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

## Continuación de esta sesión

En desarrollo a continuación: comparador, neumáticos/pits, posiciones, simulador,
explicaciones, autenticación con hashes y pruebas de regresión. No declarar esas
entregas terminadas hasta ejecutar sus verificaciones correspondientes.

Las tablas nuevas se prepararon con la migración aditiva
`database/migrations/V001__historical_archive.sql`, y permisos SELECT/INSERT
específicos para boxbox_app. La cuenta de aplicación continúa sin privilegios DDL.

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
