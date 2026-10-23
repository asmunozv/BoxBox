# Informe de independencia de MySQL

## Resultado

BoxBox utiliza **boxbox_db**, una base nueva e independiente en MySQL 8.0.45,
con **boxbox_app@localhost** y permisos por tabla/vista. Se conservaron los
datos originales; las pruebas transitorias quedaron limpiadas o revertidas.

El origen se inspeccionó con conexiones marcadas como **READ ONLY**, transacciones
REPEATABLE READ y consultas SELECT/SHOW. Las operaciones administrativas se
limitaron a objetos nuevos y al esquema de BoxBox. No se ejecutaron escrituras
sobre `f1_championship` ni ediciones en `F1_project`.

## Copia comprobada

| Tabla | Filas originales y copiadas |
|---|---:|
| app_users | 5 |
| circuits | 25 |
| constructors | 11 |
| drivers | 22 |
| driver_constructor_seasons | 0 |
| races | 46 |
| results | 31 |
| user_bingo_predictions | 25 |
| **Total** | **165** |

Se verificaron 8 tablas, 2 vistas, 8 claves primarias, 10 restricciones únicas,
7 claves foráneas y 5 CHECK activos. Se compararon también columnas, tipos,
nulabilidad, valores predeterminados, codificación, colaciones, índices, reglas
ON DELETE/ON UPDATE y valores AUTO_INCREMENT. Las siete comprobaciones de
registros huérfanos devolvieron cero.

La comparación de datos recorrió todas las filas ordenadas por su clave primaria,
incluidas claves compuestas, y comparó cada columna como bytes distinguiendo NULL.
Las huellas SHA-256 y los conteos se conservan en los manifiestos privados.

Las vistas mantienen sus consultas y resultados. En los destinos se utiliza
`SQL SECURITY INVOKER`, con dependencias exclusivamente internas al nuevo esquema.
La seguridad y el definidor de las vistas originales permanecieron intactos.

## Respaldo conservado

Carpeta de esta ejecución, relativa a BoxBox:

```text
private/backups/independencia-c4fcfc9a-881f-43e2-a721-44360d714785/
```

Archivos principales:

- `respaldo_original_completo.sql`: exportación completa de mysqldump del origen,
  con opciones de estructura, datos, vistas, rutinas, eventos y triggers.
- `estructura.sql`, `datos.sql`, `vistas.sql`: paquete portátil restaurado y probado.
- `SHA256SUMS.txt`: huellas de los cuatro archivos anteriores, verificadas al finalizar.
- `source-before.properties`: conteos, huellas y estado estructural inicial.
- `boxbox_restore_check-verified.properties`, `boxbox_db-verified.properties`:
  resultados de ambas restauraciones.
- `F1_project-files-before.properties`: huellas de los **280 archivos** del original.
- `BoxBox-before.zip`: archivos de código/configuración y binarios previos de BoxBox.
- `final-verification.txt` y `procedure/`: evidencia y herramienta administrativa del proceso.

La carpeta `private/` tiene una ACL NTFS restringida al propietario y está excluida
de Git. Los archivos privados pueden contener datos y credenciales: no se publican.
No se eliminó ningún respaldo.

La restaurabilidad se probó cargando el paquete portátil en la base nueva
`boxbox_restore_check`; después se cargó el **mismo paquete** en `boxbox_db`.
El dump original fiel se conserva aparte: no se ejecutó ciegamente como administrador
contra otro esquema, porque contiene los definidores históricos de las vistas.

En el origen inspeccionado no existían procedimientos, funciones almacenadas,
triggers ni eventos. El respaldo completo se generó con las opciones que incluyen
esos objetos. El inventario se volvió a comprobar durante la verificación final.

La base de prueba se conserva como evidencia; no hay un borrado automático.

## Cuenta de aplicación y permisos mínimos

| Objetos de boxbox_db | Permisos de boxbox_app |
|---|---|
| drivers, constructors, circuits, races, results, driver_constructor_seasons | SELECT, INSERT, UPDATE, DELETE |
| app_users | SELECT, INSERT |
| user_bingo_predictions | SELECT, INSERT, UPDATE |
| vw_driver_standings, vw_constructor_standings | SELECT |

Se comprobó que la cuenta no tiene permisos globales, comodines de esquema,
GRANT OPTION ni acceso a otros esquemas. Un SELECT sin lectura de filas sobre
`f1_championship.drivers` fue rechazado por MySQL.

La cuenta técnica `boxbox_copy` se utilizó exclusivamente para las restauraciones
en los dos destinos nuevos. Sus permisos fueron revocados y su acceso quedó
bloqueado al finalizar. La cuenta original del proyecto original no se alteró.

## Configuración y barreras de seguridad

La configuración real está en `private/config/database.properties`. El ejemplo
público `database/database.properties.example` no contiene una contraseña.

Precedencia de valores:

1. Variables `BOXBOX_DB_URL`, `BOXBOX_DB_USER`, `BOXBOX_DB_PASSWORD`.
2. Archivo privado; su ubicación puede elegirse con `BOXBOX_DB_CONFIG` o
   `-Dboxbox.db.config=...`.
3. Solo para la URL existe un valor público predeterminado hacia boxbox_db.

Usuario y contraseña son obligatorios. No hay credenciales predeterminadas.
El archivo privado se localiza respecto de los binarios de BoxBox y no del
proyecto original ni de un archivo global de Eclipse.

`DatabaseConfig` rechaza un esquema distinto y una cuenta distinta a boxbox_app.
`DatabaseConnection` comprueba `DATABASE()` y `CURRENT_USER()` antes de devolver
la conexión y utiliza zona horaria de sesión UTC. MySQL vuelve a limitar las
operaciones mediante los permisos de la cuenta, aunque una consulta accidental
incluya explícitamente otro esquema.

Los DAOs de usuarios, bingo y resultados validan el esquema mediante SELECT:
ya no ejecutan CREATE/ALTER al abrir la aplicación o consultar resultados.
El login utiliza las cuentas persistidas de la copia; se retiraron las
credenciales incrustadas, el acceso alternativo y la creación automática de cuentas.
Las cinco cuentas existentes mantienen sus datos y sus identificadores.

## Archivos y dependencias de BoxBox

- `src/model/config/DatabaseConfig.java`: configuración externa exclusiva.
- `src/model/db/DatabaseConnection.java`: guardas de base y usuario reales.
- `src/model/dao/UserAccountDAO.java`, `UserBingoDAO.java`, `ResultDAO.java`:
  validación del esquema de solo lectura.
- `src/controller/AuthController.java`: acceso a las cuentas restauradas.
- `src/module-info.java`, `build.fxbuild`, `.classpath`, `BoxBox.launch`:
  identidad y ejecución del módulo **boxbox**.
- `database/schema.sql`: instalación administrativa en una base BoxBox nueva;
  incluye las tablas de usuarios/bingo y vistas con seguridad INVOKER.
- `.gitignore`, plantilla, herramientas y documentación de verificación.

Se copiaron y comprobaron por hash las versiones existentes de JavaFX 21.0.11
(incluidas bibliotecas nativas) y MySQL Connector/J 9.1.0 a `lib/`. Se utiliza el
Gson 2.10.1 que ya estaba en BoxBox. No se modificaron bibliotecas globales de Eclipse.
`ConexionDB.jar` se conservó como recurso legado, pero se excluyó de la ejecución
porque no es utilizado por el código. Todas las conexiones de la aplicación pasan
por `model.db.DatabaseConnection`.

## Estado final y pendientes acotados

Se recompilaron los 56 archivos Java con objetivo 25 utilizando el JDK 26 del
equipo. Se verificó el lanzamiento real del módulo, una ventana visible y cierre
normal con código 0. Las funcionalidades existentes se detallan en el informe de pruebas.

Advertencias pendientes, sin impedir el funcionamiento comprobado:

- El código existente usa algunas APIs obsoletas y operaciones genéricas sin comprobar.
- JavaFX 21 emite una advertencia de sun.misc.Unsafe con JDK 26. No produjo fallos
  en el arranque, renderizado, reproducción o pruebas ejecutadas. No se actualizó
  JavaFX ni se cambió la tecnología.
- El formato heredado de contraseñas de las cuentas de aplicación se conservó para
  una copia fiel. Su migración a hashes corresponde al trabajo posterior de seguridad.
- El importador mantiene la lógica heredada que borra datos de la temporada antes
  de descargarlos. Su corrección sigue pendiente de P1. Esta operación no se utilizó
  durante la independencia; la copia y sus respaldos quedaron verificados.

La independencia de datos y configuración está completada. El proyecto original
y su base mantuvieron sus archivos, registros, definiciones y autoincrementos.
