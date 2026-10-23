# BoxBox

Aplicación JavaFX compatible con Eclipse, conectada exclusivamente a su base
MySQL **`boxbox_db`** mediante la cuenta restringida **`boxbox_app`**.

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
56 archivos Java y copia sus FXML/CSS. Los respaldos se conservan en `private/`.

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

El proceso verificó que `F1_project` y `f1_championship` conservaron su estado.
El respaldo completo y el paquete portátil probado están en la carpeta privada
indicada en el informe. No incluir `private/` al compartir o versionar el proyecto.
