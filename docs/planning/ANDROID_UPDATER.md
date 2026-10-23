# Actualizaciones Android desde GitHub Releases

## Contrato de publicación

El actualizador consulta el último release estable público de `AkkarinRothen/deckapp` y busca un asset llamado exactamente `DeckApp.apk`. El tag debe tener una versión numérica mayor que la instalada (por ejemplo, `v0.1.2`) y el APK debe usar el mismo `applicationId` y un `versionCode` superior.

El workflow `.github/workflows/android-release.yml` prepara esa publicación cuando el propietario envía un tag `vMAJOR.MINOR.PATCH`. Calcula un `versionCode` monotónico, compila `release`, verifica el APK y crea el GitHub Release con el asset esperado. Está limitado al usuario `AkkarinRothen` y al environment `release`.

### Configuración de secretos en GitHub

El environment `release` ya está creado en el repositorio y contiene estos secretos:

- `ANDROID_KEYSTORE_BASE64`: el archivo `.jks` de publicación codificado en Base64.
- `ANDROID_KEYSTORE_PASSWORD`: contraseña del almacén.
- `ANDROID_KEY_ALIAS`: alias de la clave de publicación.
- `ANDROID_KEY_PASSWORD`: contraseña de la clave.

Los secretos se cargaron desde la clave local `debug.keystore` para conservar la firma de la versión estable que ya se distribuyó. Su certificado es el de desarrollo de Android, no una clave dedicada de producción. Mantener una copia segura de ese archivo: perderlo impediría firmar actualizaciones compatibles. No subir el `.jks`, las contraseñas ni el Base64 al código fuente.

El environment todavía no tiene aprobación manual configurada; el workflow además restringe la ejecución a etiquetas creadas por `AkkarinRothen`. Para publicar, primero hay que incluir este workflow en GitHub y luego crear una etiqueta como `v0.1.2` sobre ese commit.

Android valida la firma del APK antes de reemplazar la instalación. Todos los APK de esta línea de actualización deben estar firmados con la misma clave privada que la versión ya instalada. El workflow usa la clave local guardada en los secretos, así que no depende de la clave debug que pudiera existir en el runner.

## Experiencia en la app

- Ajustes → Acerca de permite comprobar manualmente si hay una versión nueva.
- La búsqueda automática al abrir esa sección es opcional y viene desactivada.
- La descarga muestra progreso, valida que el archivo sea un APK de esta app y que su `versionCode` sea superior.
- La instalación la confirma Android. Si DeckApp no tiene permiso para instalar paquetes, se abre el ajuste del sistema para concederlo.
- La app no descarga ni instala versiones preliminares o drafts.

## Estado y migración de firma

El repositorio no tenía una versión publicada en GitHub Releases al implementar el cliente. Hasta publicar el primer release estable con el asset y firma descritos arriba, el actualizador mostrará que todavía no hay una versión publicada.

La versión estable instalada actualmente se ha compilado con la clave debug local (`signingConfig = debug`). Un APK firmado con una clave de publicación distinta **no podrá instalarse sobre esa copia**. Para migrar a una clave dedicada, primero hacer un backup desde la app, instalar manualmente una primera versión firmada con la nueva clave (desinstalar la anterior si Android rechaza la firma) y restaurar el backup. Después, las siguientes versiones podrán actualizarse desde la app.
