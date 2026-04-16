# Backup y Restauración — Plan de Implementación

> **Propósito:** Planificar el sistema de respaldo completo de la biblioteca del DM.
> Cubre el ítem D-4 del `ROADMAP.md`: export ZIP de toda la biblioteca + restore.
> Prerequisito crítico antes de cualquier release a producción.
>
> **Sprint objetivo:** 19 — ver `ROADMAP.md` para contexto.

---

## 1. Visión de Producto

El DM acumula horas de trabajo en su biblioteca: mazos importados, tablas construidas,
NPCs escritos, wikis de campaña. Un backup corrupto, un cambio de teléfono sin preparación
o una desinstalación accidental no puede significar perder todo ese trabajo.

**Principios:**
- **Un solo gesto:** "Crear backup" → archivo ZIP listo para compartir
- **Portable:** el ZIP puede copiarse a otro dispositivo y restaurarse sin fricción
- **Sin servidor:** no requiere cuenta ni internet — es un archivo local
- **Completo:** incluye imágenes, base de datos y configuración
- **Verificable:** el ZIP tiene un manifiesto legible que permite saber qué contiene

---

## 2. Formato del Archivo de Backup

### Estructura del ZIP

```
deckapp_backup_20260415_143022.zip
├── manifest.json              ← metadatos del backup
├── database/
│   └── deckapp_export.json    ← toda la DB como JSON (no el archivo .db binario)
├── images/
│   ├── decks/
│   │   ├── {deckId}/
│   │   │   ├── cover.jpg
│   │   │   ├── {cardId}_face0.jpg
│   │   │   └── {cardId}_face1.jpg
│   │   └── ...
│   └── npcs/
│       ├── {npcId}_portrait.jpg
│       └── ...
└── settings.json              ← preferencias de Settings exportables
```

### manifest.json

```json
{
  "version": 1,
  "app_version": "1.0.0",
  "created_at": "2026-04-15T14:30:22Z",
  "device_model": "Samsung Galaxy S24",
  "db_version": 11,
  "counts": {
    "decks": 12,
    "cards": 847,
    "random_tables": 23,
    "table_entries": 412,
    "npcs": 34,
    "wiki_entries": 18,
    "session_plans": 7
  },
  "images_size_bytes": 94371840,
  "checksum_sha256": "a3f7..."
}
```

### deckapp_export.json

Serialización completa de todas las entidades usando `kotlinx.serialization`.
El formato es legible e independiente del schema interno de Room:

```json
{
  "schema_version": 1,
  "decks": [
    {
      "id": 1,
      "name": "Tarokka",
      "description": "...",
      "aspectRatio": "TAROT",
      "cards": [
        {
          "id": 101,
          "title": "La Torre",
          "suit": "Arcanos Mayores",
          "faces": [...]
        }
      ]
    }
  ],
  "random_tables": [...],
  "table_entries": [...],
  "npcs": [...],
  "wiki_entries": [...],
  "session_plans": [...],
  "sessions": [...],
  "tags": [...]
}
```

> **Nota:** Las imágenes NO van codificadas en el JSON (evita archivos de cientos de MB).
> El JSON contiene rutas relativas (`"images/decks/1/101_face0.jpg"`) que se resuelven
> contra la carpeta `images/` del ZIP durante la restauración.

---

## 3. Domain — UseCases

```kotlin
// :core:domain/usecase/backup/

class CreateBackupUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val tableRepository: TableRepository,
    private val npcRepository: NpcRepository,
    private val wikiRepository: WikiRepository,
    private val sessionPlanRepository: SessionPlanRepository,
    private val fileRepository: FileRepository
) {
    /**
     * Crea el ZIP de backup en el directorio de caché de la app.
     * @return Uri del archivo creado (compartible via FileProvider)
     */
    suspend operator fun invoke(
        onProgress: (BackupProgress) -> Unit = {}
    ): Uri

    data class BackupProgress(
        val phase: Phase,
        val current: Int,
        val total: Int
    )

    enum class Phase {
        EXPORTING_DATABASE,
        COPYING_IMAGES,
        WRITING_SETTINGS,
        COMPRESSING,
        DONE
    }
}

class RestoreBackupUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val tableRepository: TableRepository,
    private val npcRepository: NpcRepository,
    private val wikiRepository: WikiRepository,
    private val sessionPlanRepository: SessionPlanRepository,
    private val fileRepository: FileRepository
) {
    /**
     * Restaura desde un ZIP de backup.
     * DESTRUCTIVO: borra la biblioteca actual y la reemplaza con el backup.
     * @param zipUri URI del ZIP seleccionado por el usuario (SAF)
     */
    suspend operator fun invoke(
        zipUri: Uri,
        onProgress: (RestoreProgress) -> Unit = {}
    ): RestoreResult

    data class RestoreProgress(
        val phase: Phase,
        val current: Int,
        val total: Int
    )

    enum class Phase {
        VALIDATING,
        CLEARING_CURRENT_DATA,
        IMPORTING_DATABASE,
        COPYING_IMAGES,
        DONE
    }

    sealed class RestoreResult {
        data class Success(val manifest: BackupManifest) : RestoreResult()
        data class Failure(val reason: FailureReason) : RestoreResult()
    }

    enum class FailureReason {
        INVALID_ZIP,
        INCOMPATIBLE_VERSION,
        CORRUPTED_DATA,
        INSUFFICIENT_STORAGE,
        UNKNOWN
    }
}

class ValidateBackupUseCase @Inject constructor() {
    // Lee solo el manifest.json del ZIP sin descomprimir todo
    // Devuelve info para mostrar al usuario antes de confirmar la restauración
    suspend operator fun invoke(zipUri: Uri): BackupManifest?
}
```

---

## 4. Serialización del Schema — Adaptadores

Cada entidad de dominio necesita un `@Serializable` equivalente para el JSON de backup.
Para no contaminar los data classes del modelo con anotaciones de serialización,
se crean DTOs en un paquete separado:

```kotlin
// :core:data/backup/dto/

@Serializable data class DeckBackupDto(val id: Long, val name: String, ...)
@Serializable data class CardBackupDto(val id: Long, val deckId: Long, ...)
@Serializable data class RandomTableBackupDto(...)
@Serializable data class NpcBackupDto(...)
@Serializable data class WikiEntryBackupDto(...)
// etc.

@Serializable
data class FullBackupDto(
    val schemaVersion: Int = 1,
    val decks: List<DeckBackupDto> = emptyList(),
    val cards: List<CardBackupDto> = emptyList(),
    val randomTables: List<RandomTableBackupDto> = emptyList(),
    val tableEntries: List<TableEntryBackupDto> = emptyList(),
    val npcs: List<NpcBackupDto> = emptyList(),
    val wikiEntries: List<WikiEntryBackupDto> = emptyList(),
    val sessionPlans: List<SessionPlanBackupDto> = emptyList(),
    val sessions: List<SessionBackupDto> = emptyList(),
    val tags: List<TagBackupDto> = emptyList()
)
```

`kotlinx.serialization` ya está en el proyecto — solo hay que añadir los DTOs.

---

## 5. Pantalla — Backup en SettingsScreen

### Sección nueva en SettingsScreen: "Copia de seguridad"

```
┌──────────────────────────────────────────┐
│  COPIA DE SEGURIDAD                      │
├──────────────────────────────────────────┤
│  Última copia: 12 de abril, 14:30        │
│  Tamaño estimado: ~92 MB                 │
│                                          │
│  [  📤 Crear backup  ]                   │
│                                          │
│  [  📥 Restaurar desde archivo  ]        │
│                                          │
│  ⚠ Restaurar borrará toda la            │
│  biblioteca actual y la reemplazará      │
│  con el contenido del backup.            │
└──────────────────────────────────────────┘
```

### Flujo de Crear Backup

1. El DM toca "Crear backup"
2. `ProgressDialog` con fases animadas (exportando DB → copiando imágenes → comprimiendo)
3. Al terminar → `Intent.ACTION_CREATE_DOCUMENT` (SAF) para que el DM elija dónde guardar
   el ZIP, o directamente `Intent.ACTION_SEND` para compartir (WhatsApp, Drive, etc.)
4. Snackbar de confirmación: "Backup creado: deckapp_backup_20260415.zip"

### Flujo de Restaurar

1. El DM toca "Restaurar desde archivo"
2. `Intent.ACTION_OPEN_DOCUMENT` para seleccionar el ZIP
3. `ValidateBackupUseCase` lee el manifest → dialog de confirmación:
   ```
   ┌─────────────────────────────────────┐
   │  Restaurar backup                   │
   │  Creado: 12 de abril de 2026        │
   │  Contenido:                         │
   │  · 12 mazos · 847 cartas            │
   │  · 23 tablas · 34 NPCs              │
   │  · 18 entradas de wiki              │
   │                                     │
   │  ⚠ Esto BORRARÁ tu biblioteca       │
   │  actual. ¿Continuar?                │
   │                                     │
   │  [Cancelar]  [Restaurar y reemplazar]│
   └─────────────────────────────────────┘
   ```
4. Confirmado → `ProgressDialog` con fases (validando → limpiando → importando → imágenes)
5. Al terminar → reinicia el NavGraph a `LibraryRoute` (para que todo el estado se recargue)

---

## 6. Backup Incremental — Fase 2

El backup completo puede ser grande (decenas de MB con muchas imágenes).
En Fase 2, implementar backup incremental:

- El manifest guarda un `lastBackupAt: Long`
- Solo se incluyen imágenes cuyo `lastModified` es posterior a `lastBackupAt`
- La DB sigue siendo completa (es pequeña comparada con las imágenes)

Esto reduce el tiempo de backup de ~30s a ~2-3s para backups frecuentes.

---

## 7. Export por Mazo Individual — Sprint 15

Separado del backup completo, el export de un único mazo como ZIP es una feature
más pequeña que ya está casi implementada (el UseCase existe):

```kotlin
// Ya existe: ExportDeckUseCase (o similar)
// Pendiente: botón en DeckDetailScreen overflow
```

El ZIP por mazo tiene formato diferente al backup global:
```
tarokka_export.zip
├── deck.json        ← metadata del mazo + cartas (sin imágenes de otros mazos)
└── images/
    ├── cover.jpg
    └── cards/...
```

Este formato es el que se usa en Import → "Desde ZIP" — circuito completo de export/import
de mazos individuales entre usuarios.

---

## 8. Compatibilidad entre Versiones

El campo `db_version` del manifest permite manejar backups de versiones anteriores:

```kotlin
// En RestoreBackupUseCase
when {
    manifest.dbVersion > currentDbVersion ->
        RestoreResult.Failure(FailureReason.INCOMPATIBLE_VERSION)
    manifest.dbVersion < currentDbVersion ->
        // Migrar los datos del backup a la versión actual
        // Usar los mismos adaptadores de migración que Room
        runMigrations(manifest.dbVersion, currentDbVersion, backupDto)
    else ->
        // Versión exacta — restaurar directamente
        restoreDirectly(backupDto)
}
```

En Fase 1, la migración de versiones antiguas se implementa como un `when` por versión.
En Fase 2, si el número de versiones crece, usar una cadena de migraciones análoga a Room.

---

## 9. Sync con la Nube — Fase 3 (Supabase)

El sistema de backup local es la base sobre la que se construye la sync opcional:

```
Backup local (Sprint 19)     →    Sync Supabase (Fase 3)
─────────────────────────         ──────────────────────────
ZIP manual                        Automático en background
Comparte el ZIP                   Almacena en bucket Supabase
Restaura manualmente              Restaura automáticamente al instalar
Sin cuenta                        Requiere cuenta Supabase
```

La integración Supabase reutilizaría `CreateBackupUseCase` para generar el ZIP
y lo subiría a un bucket `deckapp-backups/{userId}/backup_latest.zip`.

---

## 10. Roadmap de Implementación

### Sprint 19 — Backup Fase 1
```
Día 1:
  · DTOs de serialización para todas las entidades (@Serializable)
  · FullBackupDto + BackupManifest
  · CreateBackupUseCase: export DB a JSON + copia imágenes + comprime ZIP
  · ValidateBackupUseCase: leer manifest sin descomprimir

Día 2:
  · RestoreBackupUseCase: validar → limpiar DB → importar JSON → copiar imágenes
  · Manejo de errores + RestoreResult sealed class
  · Tests unitarios de serialización/deserialización

Día 3:
  · SettingsScreen: nueva sección "Copia de seguridad"
  · BackupViewModel: estados de progreso
  · ProgressDialog con fases animadas

Día 4:
  · AlertDialog de confirmación con contenido del manifest
  · Reinicio del NavGraph tras restauración exitosa
  · Export individual de mazo (Sprint 15 deuda — A-6): botón en DeckDetailScreen
  · BUILD SUCCESSFUL + smoke test con ZIP real
```

### Sprint 20+ — Backup Fase 2
```
  · Backup incremental (solo imágenes modificadas desde último backup)
  · Programar recordatorio periódico de backup (NotificationManager, semanal)
  · Compatibilidad de migración para backups de versiones anteriores
  · Auto-backup al Drive del dispositivo (Android Auto Backup API) — solo metadata, sin imágenes grandes
```

---

## 11. Decisiones de Producto Pendientes

| # | Pregunta | Opciones | Recomendación |
|---|----------|---------|---------------|
| B-1 | **¿El backup incluye las sessions/DrawEvents (historial completo)?** | (a) Sí — backup más completo pero mucho más grande. (b) Solo la biblioteca (mazos, tablas, NPCs, wiki). | Opción (b) para Fase 1: el historial de sesiones puede ser muy voluminoso; añadir en Fase 2 como opción |
| B-2 | **¿Restaurar es siempre destructivo o puede ser aditivo?** | (a) Reemplaza todo (simple). (b) Merge: solo añade lo que no existe (complejo). | Opción (a): reemplaza todo — reduce la complejidad de conflictos enormemente |
| B-3 | **¿El ZIP se guarda en almacenamiento interno o externo?** | (a) `cacheDir` (temporal, se puede limpiar). (b) `getExternalFilesDir` (persistente, sin permisos extra en API 29+). | Opción (a) + SAF `ACTION_CREATE_DOCUMENT` para que el usuario elija el destino final |
| B-4 | **¿Recordatorio automático de backup?** | (a) No. (b) Notificación cada 7 días si no se ha hecho backup. | Opción (b) en Fase 2 — no bloquea MVP |
| B-5 | **¿El backup cifra el archivo?** | Los datos son sensibles (secretos de campaña). Cifrar con contraseña del DM. | No en Fase 1 — el ZIP no es un formato cifrado estándar. Fase 3: considerar AES si hay sync en la nube |

---

## 12. Criterios de Calidad

- El proceso de backup no debe bloquear el hilo principal — todo en `viewModelScope` + `Dispatchers.IO`
- Un backup de 50 mazos y 1000 cartas debe completarse en < 60 segundos
- La restauración debe ser atómica: si falla a mitad, la DB queda en estado consistente
  (restaurar en una transacción Room, sin borrar los datos viejos hasta confirmar que el import es válido)
- El ZIP de backup debe abrirse correctamente en un explorador de archivos estándar del PC
- El campo `checksum_sha256` del manifest debe verificarse antes de restaurar
- Mensaje de error debe ser claro: "El archivo no es un backup de DeckApp" vs
  "El backup fue creado con una versión más nueva de la app"

---

*Creado: 15 de abril de 2026*  
*Relacionado: `ROADMAP.md` D-4 + Sprint 19, `FUTURE_PHASES.md` §Cloud, `PLAN_GENERAL.md` §Fase 3*
