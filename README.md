# BoxBox

Aplicación JavaFX compatible con Eclipse, conectada exclusivamente a su base
MySQL **`boxbox_db`** mediante la cuenta restringida **`boxbox_app`**.

## Laboratorio histórico implementado

- Explorador por temporada/GP, piloto, escudería, búsqueda y carrera/sprint.
- Parrilla, clasificación cronometrada, resultados y puntos conservados de la fuente.
- Comparador de pilotos/equipos, evolución de puntos y tabla por Gran Premio.
- Stints interactivos, tiempos por vuelta, pits y explicaciones de sus límites.
- Posiciones con P1 arriba, pilotos ocultables, huecos preservados y marcadores de pits.
- Simulador educativo con alternativas de parada/compuesto, parámetros editables,
  diferencia de tiempos, sensibilidad y supuestos visibles.
- Narrativa determinista de eventos con evidencia observada/calculada.

Los módulos anteriores siguen disponibles en el menú lateral: pilotos, escuderías,
mapa/circuitos, gestión de resultados, bingo y arcade.

**Demo preparada:** 2024 / R1 Bahrain Grand Prix. El botón **Abrir demo · Baréin 2024**
valida e importa el paquete público local; para explorar no se necesita conexión
a las APIs. MySQL local sí debe estar en ejecución.

## Primer recorrido (sin conocimientos técnicos)

1. Después de entrar, pulsa **Cómo usar BoxBox** para abrir la guía integrada.
2. Pulsa **Abrir demo · Baréin 2024**. En Resumen selecciona un piloto: aparecen
   su foto y los botones **Escuchar / Detener**. **Ver tiempos Q1–Q3** añade el
   detalle de clasificación sin recargar la tabla inicial.
3. Abre **Posiciones** y activa pilotos; pulsa un punto para consultar su evidencia.
   En **Neumáticos**, pulsa un bloque para interpretar su stint.
4. En **Simulador**, pulsa **Adelantar primera parada 3 vueltas** o usa las filas
   visuales para añadir/quitar paradas y cambiar compuesto/edad. **Comparar escenarios**
   lleva al resultado; **Restablecer** recupera la estrategia y parámetros iniciales.
5. Desplázate dentro del módulo para ver las gráficas, tablas y supuestos completos.
   **Añadir datos con internet** queda reservado para preparar otras selecciones.

**Puntos:** ganar otorga **25** con la escala completa. En Baréin 2024, Max sumó
además **1 por vuelta rápida**, por eso aparece `25 + 1 VR = 26`. El total oficial
se conserva; desde 2025 no existe ese punto extra.

## Ejecutar en este equipo

1. Comprobar que el servicio **MySQL80** esté iniciado.
2. Abrir el proyecto **BoxBox** en Eclipse y actualizarlo con **Refresh**.
3. Abrir `BoxBox.launch` y ejecutarlo como configuración de aplicación Java.

El lanzamiento utiliza el módulo `boxbox`, el JDK 26 registrado y el directorio
de trabajo de BoxBox. Las dependencias JavaFX, JDBC y Gson son locales al proyecto.
La compilación conserva el objetivo Java 25.

También se puede compilar y ejecutar desde una terminal situada en BoxBox:

```powershell
java tools/BuildBoxBox.java
java tools/BuildBoxBox.java --run
```

La herramienta limpia únicamente la salida generada `bin/` de BoxBox, compila los
archivos Java y copia sus FXML/CSS. Los respaldos se conservan en `private/`.

Pruebas repetibles (datos locales y servidor HTTP sintético de pruebas):

```powershell
java tools/VerifyBoxBox.java
java tools/VerifyBoxBox.java --ui --gallery
```

Las pruebas UI abren ventanas y conservan capturas privadas. No requieren servicios
de pago ni bibliotecas de pruebas adicionales.
La suite actual ejecuta cinco verificadores sin UI o siete con `--ui`, incluyendo
49 aserciones de análisis y comprobaciones de los recursos multimedia originales.

## Acceso y configuración

- Las cuentas de la pantalla de login son las cuentas de aplicación copiadas
  del proyecto existente; no son la cuenta MySQL `boxbox_app`.
- La conexión ya quedó configurada en `private/config/database.properties`,
  con ACL de Windows restringida al propietario. Ese archivo no se versiona.
- Las variables `BOXBOX_DB_URL`, `BOXBOX_DB_USER` y `BOXBOX_DB_PASSWORD`
  permiten suministrar la configuración externamente.
- `BOXBOX_DB_CONFIG` o `-Dboxbox.db.config=...` permiten elegir otro archivo privado.
- El código rechaza otra base u otro usuario y valida la identidad real de la
  conexión. Las variables heredadas `F1_DB_*` no intervienen en BoxBox.

La plantilla pública sin secretos está en `database/database.properties.example`.

## Documentación de la independencia

- [Informe y configuración MySQL](docs/INDEPENDENCIA_MYSQL.md)
- [Pruebas ejecutadas y resultados](docs/VERIFICACION_INDEPENDENCIA.md)

## Documentación para el concurso

- [Estado de avance verificable](docs/AVANCE_BE_A_DRAGON.md)
- [Arquitectura y relaciones](docs/ARQUITECTURA.md)
- [Fórmulas, supuestos, complejidad y límites](docs/MODELO_ESTRATEGIAS.md)
- [Guion de demostración y preguntas del jurado](docs/DEMO_CONCURSO.md)
- [Correcciones, archivos y pruebas de esta continuación](docs/INFORME_CORRECCIONES.md)
- [Fotos, audio y video locales](docs/RECURSOS_MULTIMEDIA.md)
- [Acceso protegido y transición de contraseñas](docs/SEGURIDAD_ACCESO.md)
- [Rúbrica y evidencia](docs/RUBRICA_EVIDENCIA.md)
- [Condiciones de los datos demo](datasets/LICENSE-DATA.md)

`database/migrations/V001__historical_archive.sql` es la migración aditiva ya aplicada
en este equipo. La cuenta de ejecución recibe SELECT/INSERT en las nuevas tablas;
la aplicación no realiza DDL. Para otra instalación se requiere preparar el esquema
y sus permisos expresamente con un administrador, conservando la configuración privada.

El proceso verificó que `F1_project` y `f1_championship` conservaron su estado.
El respaldo completo y el paquete portátil probado están en la carpeta privada
indicada en el informe. No incluir `private/` al compartir o versionar el proyecto.
