# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Comandos esenciales

```bash
# Build completo (ejecutar al final de cada sprint)
./gradlew :app:assembleDebug

# Compilar solo un módulo afectado
./gradlew :core:data:compileDebugKotlin
./gradlew :feature:import:compileDebugKotlin

# Tests unitarios (solo existen en :core:domain por ahora)
./gradlew :core:domain:test
./gradlew :core:domain:test --tests "com.deckapp.core.domain.usecase.MergeDecksUseCaseTest"
```

## Mandato antes de tocar código

1. **Leer `DEVLOG.md`** — entender las últimas decisiones técnicas antes de proponer cambios.
2. **Revisar `docs/planning/`** — `PLAN_GENERAL.md`, `ROADMAP.md` y los planes activos en `planificaciones completadas/` para no contradecir decisiones ya tomadas.
3. **Al terminar** un sprint con cambios sustanciales, actualizar `DEVLOG.md` con categoría (`FEATURE / ARCHITECTURE / FIX / INTEGRATION`) y descripción.

## Arquitectura de módulos

```
:core:model     → Modelos de dominio puros (CardStack, Card, RandomTable, TableEntry…)
:core:domain    → Interfaces de repositorio + UseCases (TODA la lógica de negocio)
:core:data      → Implementaciones Room, DAOs, mappers, repositorios de datos, GeminiAiRepository
:core:ui        → DeckAppTheme (Material 3), componentes compartidos (CardThumbnail…)
:feature:*      → Pantallas + ViewModels. Dependen de :core:domain, nunca de :core:data directamente
:app            → NavGraph.kt, DeckApplication (@HiltAndroidApp), MainActivity
```

La dependencia fluye en una sola dirección: `feature → domain ← data`. Ningún módulo de feature importa `:core:data`.

## Inyección de dependencias

- **Regla general:** `@HiltViewModel` en todos los ViewModels de feature.
- **Excepción crítica:** `TableImportViewModel` usa una **Factory manual** (no Hilt) para evitar un `NullPointerException` interno de KSP en `:feature:import`. El ViewModel se instancia en `TableImportScreen` mediante un `@EntryPoint` y `EntryPointAccessors.fromApplication(...)`. Al añadir dependencias nuevas a ese ViewModel hay que actualizar tanto el constructor como la `Factory` y el `TableImportEntryPoint`.
- **:feature:tables** usa **kapt** (no KSP) por un error interno del compilador KSP en ese módulo específico.
- **:core:data** usa **kapt** para Room + Hilt. El resto de módulos usan **KSP**.

## Room — reglas de migración

- La base de datos está en **versión 24**. No hay `fallbackToDestructiveMigration` en producción.
- Cada cambio de esquema requiere: (1) nueva `MIGRATION_N_N+1` en `Migrations.kt`, (2) registrarla en `databaseBuilder` dentro de `DataModule.kt`, y (3) añadir el nuevo DAO con `@Provides` si aplica.
- Las entidades FTS (`CardFtsEntity`, `RandomTableFtsEntity`, etc.) exigen que sus tablas virtuales se creen en la migración correspondiente.

## Navegación

Las rutas son **type-safe** con `@Serializable` (Navigation Compose 2.8+). Están definidas en `Screen.kt`:

```kotlin
@Serializable object LibraryRoute          // Sin parámetros
@Serializable data class DeckDetailRoute(val deckId: Long)   // Con parámetros
```

Para añadir una pantalla nueva: (1) declarar su ruta en `Screen.kt`, (2) añadir el `composable<MiRuta> { ... }` en `NavGraph.kt`.

## Persistencia de sesión (event log)

Cada acción de juego (DRAW, DISCARD, FLIP, REVERSE, ROTATE, RESET, PEEK) se escribe en `DrawEventEntity` **antes** de ejecutar la animación. El estado actual de la sesión es una proyección del log, no un snapshot mutable. Esto da undo, historial y crash recovery gratis.

## Pipeline de importación de tablas (OCR + IA)

El flujo vive en `:feature:import` y pasa por 5 pasos (`ImportStep`): `SOURCE_SELECTION → FILE_PREVIEW → CROP → RECOGNITION → MAPPING → REVIEW`.

- **OCR:** `OcrRepositoryImpl` (ML Kit, `@ActivityRetainedScoped`) → `AnalyzeTableImageUseCase` (clustering espacial + detección de columnas).
- **IA texto:** `TranscribeTableWithAiUseCase` → `GeminiAiRepository.reconstructTable()` — limpieza post-OCR.
- **IA Vision:** `RecognizeTableFromImageUseCase` → `GeminiAiRepository.recognizeTableFromImage()` — bypasa OCR, envía el bitmap directamente a Gemini.
- `GeminiAiRepository` cachea el `GenerativeModel` por API key para evitar recrearlo en cada llamada. El bitmap se escala a ≤800 px de ancho antes del envío.
- La API key de Gemini se gestiona en `SettingsRepository` (SharedPreferences, nunca en el código).

## Modelos de contenido de carta

8 modos (`CardContentMode`): `IMAGE_ONLY`, `IMAGE_WITH_TEXT`, `REVERSIBLE`, `TOP_BOTTOM_SPLIT`, `LEFT_RIGHT_SPLIT`, `FOUR_EDGE_CUES`, `FOUR_QUADRANT`, `DOUBLE_SIDED_FULL`. Cada mazo tiene `defaultContentMode`; cada carta puede sobreescribirlo. `CardViewScreen` despacha el rendering con un `when(face.contentMode)`.

## Testing

- Tests unitarios solo en `:core:domain`. Stack: **JUnit 4 + MockK + Turbine + kotlinx-coroutines-test**.
- Patrón: instanciar el UseCase directamente con mocks, sin Hilt. Ver `MergeDecksUseCaseTest` como referencia.

## Mandatos de componentes UI

- Componentes con >50 líneas de lógica/estado → extraer a `components/` dentro del módulo de feature.
- `*Screen.kt` solo orquesta componentes; la lógica va en ViewModel/UseCase.
- Estado UI inmutable desde ViewModel con `collectAsStateWithLifecycle`. Acciones hacia arriba como lambdas.
- Touch targets mínimo 48×48 dp. Dark theme default ON. Wake lock activo en `SessionScreen`.
