# Guía de Control de Versiones, Respaldo y Coexistencia de Apps — DeckApp

Este documento describe la arquitectura de control de versiones, prevención de pérdida de datos y la estrategia para compilar e instalar versiones paralelas de **DeckApp**.

---

## 1. Arquitectura de Ramas y Versiones

DeckApp utiliza una estrategia de dos ramas principales más etiquetas (*Git Tags*) inmutables:

```
[master]  ──(Desarrollo diario, sprints, nuevas features)───────►
    │
    ▼ Checkpoint / Tag (ej. sprint-58, v0.1.2) + Backup local (.bundle)
    │
[release] ──(Versión estable validada y lista para jugar)───────►
```

- **`master`:** Rama de desarrollo continuo. Aquí se implementan sprints, correcciones y nuevas herramientas.
- **`release`:** Rama de producción y estabilidad. Solo avanza cuando un sprint está 100% probado y libre de regresiones.
- **Git Tags (`sprint-XX`, `vX.Y.Z`):** Puntos de control congelados e inmutables. Incluso si una rama se borra o sobreescribe, los tags nunca se pierden.
- **Respaldos locales `.bundle` (`.backups/`):** Instantáneas binarias completas del historial de Git guardadas fuera del alcance de GitHub en tu máquina local.

---

## 2. Coexistencia de Apps en Android (Estable vs Dev)

Para evitar que una versión en desarrollo sobreescriba tu app de juego real o corrompa su base de datos SQLite con migraciones experimentales, las variantes están separadas por identificador de paquete:

| Característica | Versión Estable (`release`) | Versión de Desarrollo (`debug`) |
| :--- | :--- | :--- |
| **Nombre en pantalla** | **DeckApp** | **DeckApp (Dev)** |
| **Application ID** | `com.deckapp.ttrpg` | `com.deckapp.ttrpg.debug` |
| **Base de datos** | Separada e intacta | Sandbox aislado e independiente |
| **Comando para compilar** | `./gradlew :app:assembleRelease` | `./gradlew :app:assembleDebug` |
| **Instalación simultánea** | ✅ Sí (ambas conviven en el mismo dispositivo) | ✅ Sí |

> [!NOTE]
> Puedes tener **ambas aplicaciones instaladas a la vez** en tu teléfono o tablet. Jugar tus campañas en la estable y probar nuevas mecánicas en la versión Dev sin ningún riesgo de pérdida de partidas.

---

## 3. Scripts Automatizados de Gestión

Hemos provisto scripts sencillos en PowerShell ubicados en `scripts/`:

### A. Crear un Punto de Control (Checkpoint) y Respaldo
Cuando termines una sesión de desarrollo o un sprint:
```powershell
.\scripts\checkpoint.ps1
```
*O con parámetros directos:*
```powershell
.\scripts\checkpoint.ps1 -Tag "sprint-58" -Message "Mejoras de Atlas VTT completas" -SyncRelease -PushRemote
```
**¿Qué hace automáticamente?**
1. Comprueba que no queden cambios sin guardar.
2. Crea el Tag anotado inmutable en Git.
3. Genera un archivo `.bundle` de respaldo en `.backups/`.
4. Verifica la integridad criptográfica del respaldo.
5. Te pregunta si deseas sincronizar la rama `release` y subir los tags a GitHub.

---

### B. Cambiar entre Versiones de la Aplicación
Para viajar en el tiempo e inspeccionar o compilar cualquier versión previa:
```powershell
.\scripts\switch-version.ps1
```
El script te mostrará una lista interactiva con todas las versiones disponibles y sus fechas:
```text
  [1] sprint-58 - Mejoras de Atlas VTT completas (creado 2026-10-15)
  [2] sprint-57 - Elevación 3D y Condiciones (creado 2026-10-15)
  [0] Regresar a la última versión en 'master'
```

Para regresar en cualquier momento a la última versión de desarrollo:
```powershell
.\scripts\switch-version.ps1 -BackToMaster
```

---

### C. Instalar Cualquier Versión en tu Dispositivo Android
Para instalar el último build o cualquier APK archivado en tu teléfono/tablet:
```powershell
.\scripts\install-version.ps1
```
- **Conexión ADB automática:** Si tienes el teléfono conectado por USB con depuración activada (o un emulador abierto), el script instala el APK de inmediato sin perder datos (`adb install -r -d`).
- **Transferencia manual asistida:** Si no hay dispositivo conectado, abre automáticamente la carpeta del archivo APK en el Explorador de Windows para transferirlo vía USB, Google Drive o mensajería.

---

### D. Bóveda Histórica de APKs (`.backups/apks/`)
Al ejecutar `.\scripts\checkpoint.ps1`, puedes activar `-BuildApk` para compilar y archivar el APK listo para jugar:
- Guarda copias fechadas: `DeckApp_Dev_sprint-XX_YYYYMMDD_HHMMSS.apk`.
- Mantiene un catálogo automático en `.backups/apks/CATALOG.md` con el commit SHA, tamaño y notas de la versión.

---

### E. Migración de Campañas y Mapas Tácticos entre Versiones
DeckApp incluye un motor de respaldo completo e independiente de la versión de base de datos Room:
1. En **`DeckApp`** o **`DeckApp (Dev)`**, ve a **Ajustes > Crear Backup**.
2. Guarda el archivo `.zip` de respaldo generado.
3. Ahora puedes abrir la otra versión de la app (o una versión previa) e ir a **Ajustes > Restaurar Backup**.
4. Se migrarán íntegramente:
   - Mazos, cartas, caras y rotaciones.
   - Tablas aleatorias y tablas de referencia del sistema.
   - Sesiones, notas del DM y eventos de juego.
   - Encuentros y bitácora de combate.
   - **Mapas tácticos, niebla de guerra, terrenos, pines (sub-mapas) y tokens con elevación.**

---

### F. Recuperación ante Desastres de GitHub
Si en algún momento GitHub sufre un desastre, una sincronización incorrecta o una sobrescritura accidental:
```powershell
.\scripts\restore-from-backup.ps1
```
Este script analiza la carpeta `.backups/`, verifica el bundle seleccionado y restaura inmediatamente todas las ramas y commits al estado exacto del respaldo offline.

---

## 4. Buenas Prácticas para Evitar Pérdidas de Código

1. **Nunca usar `git push --force` a ciegas:** Si GitHub rechaza un push, investiga con `git log origin/master..master` antes de forzar nada.
2. **Crear siempre un Checkpoint al cerrar un sprint:** Ejecutar `.\scripts\checkpoint.ps1` garantiza que el trabajo queda sellado con un tag y guardado en un `.bundle` offline.
3. **Probar primero en `debug`:** Compila `./gradlew :app:assembleDebug` y verifica la app en tu dispositivo antes de fusionar en `release`.

