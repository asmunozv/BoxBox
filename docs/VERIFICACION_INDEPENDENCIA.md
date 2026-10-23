# Evidencia de pruebas de independencia

Los resultados siguientes corresponden a ejecuciones reales de esta sesión.
No son escenarios simulados ni resultados previstos.

## Matriz de resultados

| Verificación | Resultado |
|---|---|
| Rutas y enlaces iniciales de BoxBox | 280 archivos; cero enlaces simbólicos/reparse y cero enlaces duros múltiples |
| Destinos/cuentas antes de crear | Nombres de bases y cuentas propuestos ausentes |
| Conexión de inspección al origen | READ ONLY = 1; REPEATABLE READ |
| Exportaciones nativas mysqldump | Código de salida 0; archivos completos/no vacíos; SHA-256 conservados |
| Restauración de prueba | 8 tablas, 2 vistas, 165 filas, igualdad de todas las columnas y metadatos comprobados |
| Restauración definitiva | Mismo paquete; igualdad completa e integridad comprobadas |
| Relaciones | 7 relaciones verificadas, cero huérfanos, cero referencias a otro esquema |
| Claves y restricciones | 8 PK, 10 UNIQUE y 5 CHECK activos, con columnas/reglas/índices equivalentes |
| Seguridad de vistas | Dos vistas INVOKER en BoxBox; seguridad y definidor originales conservados |
| Privilegios de boxbox_app | 10 concesiones por tabla/vista más USAGE; sin privilegios globales ni comodines de esquema |
| Acceso al origen como boxbox_app | SELECT rechazado por permisos, sin leer datos ni intentar escrituras |
| Cuenta técnica | Permisos revocados; ACCOUNT LOCK comprobado |
| Vistas tras bloquear la cuenta técnica | Ambas vistas legibles como boxbox_app; ejecución con privilegios INVOKER comprobada |
| Configuración privada | ACL NTFS restringida al propietario; contraseña fuera de código/informes |
| URL dirigida al origen | Rechazada por DatabaseConfig antes de JDBC |
| Usuario administrativo | Rechazado por DatabaseConfig antes de JDBC |
| Variables heredadas F1_DB_* | Ignoradas por BoxBox; conexión continúa en boxbox_db |
| Lectura y escritura transitoria | UPDATE/SELECT en la copia y ROLLBACK correctos |
| DAOs de las entidades | Lecturas y validación del esquema correctas sin privilegios CREATE/ALTER |
| CRUD y relaciones | Creación de fixtures identificados, lectura, actualización, eliminación, asignación piloto/equipo y estadísticas correctas |
| Registro/login/bingo | Cuenta transitoria registrada; login válido/incorrecto comprobados; bingo insertado, actualizado y leído |
| Limpieza de fixtures | Eliminados exclusivamente los registros de prueba de BoxBox; autoincrementos restablecidos |
| Estado posterior a pruebas | Los 165 registros y valores originales vuelven a coincidir en la copia |
| Proyecto original | Los 280 archivos conservan exactamente sus huellas SHA-256 |
| Base original | Filas, columnas, índices, claves, restricciones, vistas y autoincrementos conservados |
| Respaldos al finalizar | Las cuatro huellas de los archivos de restauración/dump siguen coincidiendo |

## Prueba gráfica con JavaFX real

`tools/verification/ApplicationSmoke.java` inicia el toolkit JavaFX y ejecuta
`application.Main.start`, sus FXML y sus controladores dentro del módulo boxbox.
La autenticación usa una cuenta real copiada, leída internamente para la prueba,
sin publicar usuario/contraseña. No se llamó al importador de APIs.

Resultados observados:

- Login y transición a la ventana principal correctos.
- Seis pestañas originales presentes.
- 22 pilotos, 11 constructores y 31 resultados cargados; los resultados se
  contrastaron con una consulta SQL de la temporada seleccionada.
- Mapa local y marcadores de carreras cargados.
- Nueve campos y nueve casillas de bingo cargados.
- Perfil de piloto, perfil de constructor y ambas ventanas de clasificación abiertos.
- Minijuego: renderizado, avance de AnimationTimer y movimiento por teclado comprobados.
- Audio y video de login en estado PLAYING y sin errores de MediaPlayer.
- Logout correcto y vuelta al login.

La evidencia gráfica quedó en `private/verification/`, sin publicarse ni incluirse
en Git. Una prueba adicional lanzó directamente **boxbox/application.Main**,
comprobó una ventana nativa visible y solicitó su cierre: terminó con **código 0**.
No quedó una aplicación de prueba ejecutándose.

## Herramientas y comandos utilizados

Compilación de producción, sin acceso a bases de datos:

```powershell
java tools/BuildBoxBox.java
```

Prueba gráfica (tras compilar el verificador en la salida privada):

```powershell
javac --release 25 -encoding UTF-8 --module-path "bin;lib/javafx-sdk-21.0.11/lib;gson-2.10.1.jar" --class-path "lib/mysql-connector-j-9.1.0.jar" --add-modules java.desktop --add-reads boxbox=java.desktop --patch-module "boxbox=tools/verification" -d "private/verification/classes" tools/verification/ApplicationSmoke.java
java --enable-native-access=javafx.graphics,javafx.media --module-path "bin;lib/javafx-sdk-21.0.11/lib;gson-2.10.1.jar" --class-path "lib/mysql-connector-j-9.1.0.jar" --add-modules java.desktop --add-reads boxbox=java.desktop --patch-module "boxbox=private/verification/classes" --module boxbox/verification.ApplicationSmoke
```

Los permisos adicionales de lectura de java.desktop se usan solo para guardar
capturas PNG del verificador; la aplicación normal no necesita esos argumentos.

`DatabaseIsolationCheck.java` contiene las pruebas de conexión, configuración,
CRUD, registro y bingo ejecutadas. Su ejecución de integración crea una cuenta de
prueba y requiere la limpieza administrativa acotada utilizada en esta migración;
no es una acción de arranque de la aplicación ni un comando de uso cotidiano.
La cuenta de aplicación no recibe DELETE sobre usuarios para facilitar una prueba.

El comparador administrativo y sus manifiestos están conservados en la carpeta
privada del respaldo. La comprobación de igualdad con la copia inicial es propia
de esta independencia: tras registrar datos reales nuevos en BoxBox, la copia
naturalmente podrá diferir del origen, manteniendo su aislamiento.

## Incidencias resueltas durante la verificación

1. MySQL representó algunas columnas con un `CHARACTER SET utf8mb4` redundante
   en SHOW CREATE TABLE después de restaurarlas. Se normalizó solo esa diferencia
   de representación, manteniendo la comparación estricta de tipos, charset,
   colación y demás metadatos. El intento inicial revirtió sus inserciones antes
   de continuar; el origen permaneció intacto.
2. El verificador gráfico inicialmente invocó un diálogo modal desde un callback
   de animación. Se corrigió el verificador para usar Platform.runLater, como un
   evento de interfaz ordinario. No fue necesario cambiar los controladores de perfiles.
3. La supervisión nativa conservó el handle del proceso para obtener correctamente
   su ExitCode en Windows PowerShell. El lanzamiento y cierre final devolvieron 0.

Los reintentos conservaron las evidencias anteriores. No se borraron respaldos ni
se reutilizó una base preexistente ajena a esta ejecución.
