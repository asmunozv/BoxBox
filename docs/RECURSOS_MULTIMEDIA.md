# Fotos, audio y video locales

## Cómo usarlos

- En **Explorador histórico → Resumen**, selecciona una fila de piloto. Aparecen
  la foto de tu colección y el botón **Escuchar**. **Detener**, cambiar de piloto,
  cambiar de módulo o cerrar sesión libera el reproductor.
- En **Pilotos** se conserva el perfil con fotografía y reproducción del audio al
  seleccionar/abrir el piloto. El login conserva su música y video, y Arcade su música.
- Una foto de la colección no acredita que se tomara en la temporada seleccionada.
  Un archivo de audio es material multimedia del proyecto, no evidencia de esa carrera.

## Resolución y ausencias

`model.util.LocalResources` localiza los archivos dentro de BoxBox a partir del
directorio de las clases compiladas, con respaldo en el directorio de ejecución.
Busca los recursos preservados en `assets/` y `src/assets/`. Reconoce los alias
`piastri`/`oscar_piastri` y `nico_hulkenberg`/`hulkenberg`. No modifica archivos
ni sigue rutas heredadas que apunten fuera de BoxBox.

`LocalImageService` usa las imágenes locales existentes. Si falta una foto,
muestra una silueta neutral propia; no realiza búsquedas web ni descarga imágenes
sin procedencia. Si falta audio, el botón queda deshabilitado y se informa al usuario.

## Evidencia ejecutada

`ResourceChecks` decodificó las **22 fotos de pilotos** y encontró sus **22 audios**,
el mapa y los medios del login, ejecutándose desde una carpeta diferente a la raíz.
`HistoricalExplorerSmoke` verificó la foto original de Max, audio en estado PLAYING,
Detener y liberación al cambiar de pestaña. `ApplicationSmoke` comprobó música y
video del login en PLAYING y la regresión de las funciones anteriores.

Los medios originales se conservan. Su presencia local no demuestra permisos de
redistribución; el estado de sus licencias sigue documentado en RUBRICA_EVIDENCIA.md.
