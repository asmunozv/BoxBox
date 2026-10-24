# Acceso protegido y migración aditiva

## Credenciales MySQL

Solo `boxbox_app` accede desde la aplicación, con base/identidad verificadas.
Configuración en variables BOXBOX_DB_* o archivo privado con ACL del propietario;
sin contraseña predeterminada ni valores en Git. Las rutas y el mecanismo de
independencia se describen en INDEPENDENCIA_MYSQL.md.

## Cuentas de aplicación

`PasswordHasher` usa PBKDF2-HMAC-SHA256, 600000 iteraciones, salt aleatorio de
128 bits y clave de 256 bits. El formato incluye versión, iteraciones y salt.
La comparación de claves usa MessageDigest.isEqual. Las codificaciones inválidas
y costes fuera de rango se rechazan. No hay una contraseña universal de respaldo.

Nuevas cuentas: se guarda una codificación, no la contraseña en claro. Se admiten
contraseñas de 8–1024 caracteres y nombres no vacíos de hasta 80.

Cuentas heredadas: después de un login correcto se añade el hash a
`bb_password_hashes`, con FK al identificador existente. **El campo password
heredado no se sobrescribe.** Las contraseñas cortas ya existentes mantienen su
acceso legítimo. Si ya hay hash, un fallo de verificación no habilita un acceso
alternativo por texto claro. Las carreras concurrentes de migración no reemplazan
un hash previamente insertado.

## Reversibilidad y límite explícito

Esta es una transición aditiva, no una eliminación del secreto heredado. Los datos
antiguos permanecen para conservar acceso y permitir rollback de código de las
cuentas anteriores. Las cuentas nuevas codificadas requieren el verificador actual;
un rollback antiguo que compare texto claro no comprende su formato.

Retirar definitivamente los campos heredados en claro exige otra migración
administrativa con respaldo y estrategia de recuperación/reset; no se ejecutó un
borrado ni una sustitución masiva de credenciales existentes.

El cálculo criptográfico y la consulta de acceso ocurren en una Task de fondo.
Los controles se bloquean mientras se verifica, se muestran errores comprensibles
y el campo de contraseña se limpia al entrar. La lógica reusable vive en
AuthenticationService; el controlador solo actualiza la sesión de escritorio.

## Pruebas

SecurityChecks: salts distintos, credencial válida/inválida, codificaciones rotas,
trabajo acotado y comparación heredada. La regresión SQL comprobó registro de una
cuenta transitoria con contraseña codificada, login correcto/incorrecto y bingo.
La cuenta de prueba se retiró de boxbox_db con un filtro de propiedad explícito;
no se concedió DELETE sobre usuarios a la cuenta de aplicación.

Referencia de política criptográfica:
https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html .
