# DeckApp TTRPG — Project Instructions

## Architectural Standards
- **Multi-Module Gradle:** La app está modularizada por capas y features.
    - `:core:model`: Modelos de dominio puros (CardStack, Card, CardFace, ContentZone, Tag, DrawEvent, enums).
    - `:core:domain`: Interfaces de repositorio + UseCases. **Toda la lógica de negocio va aquí.**
    - `:core:data`: Implementaciones de repositorio, Room entities, DAOs, mappers.
    - `:core:ui`: Design system, tema `DeckAppTheme`, componentes compartidos (CardThumbnail, etc.).
    - `:feature:library`: LibraryScreen — biblioteca de mazos.
    - `:feature:deck`: DeckDetailScreen, CardEditorScreen, CardViewScreen.
    - `:feature:draw`: SessionScreen, PileScreen — pantalla principal de juego.
    - `:feature:import`: ImportScreen, PDF processor, background Worker.
    - `:feature:session`: SessionSetupScreen, SessionHistoryScreen.
- **Clean Architecture:** Toda la lógica de negocio DEBE residir en UseCases dentro de `:core:domain`.
- **Navigation:** Enrutamiento centralizado en `NavGraph.kt` dentro del módulo `:app`.
- **Dependency Injection:** Usar Hilt en todos los módulos.
- **UI:** Usar Jetpack Compose y seguir el sistema de diseño `DeckAppTheme` (Material Design 3).

## DEVLOG Mandate
Al realizar cambios sustanciales de arquitectura o feature, el desarrollador (o agente) DEBE
actualizar `DEVLOG.md` con fecha, tipo (DECISION / ARCHITECTURE / INTEGRATION / PROBLEM / SOLUTION)
y descripción. Mantener el registro histórico vivo del desarrollo.

## Planning and History Mandate
Cualquier desarrollador (o agente) DEBE realizar las siguientes acciones ANTES de generar código o proponer cambios:
1. **Verificar `DEVLOG.md`:** Entender las últimas decisiones técnicas y el estado actual.
2. **Revisar `docs/planning/`:** Consultar el `PLAN_GENERAL.md` y cualquier plan específico (ej. `ADVANCED_TABLES.md`) para asegurar consistencia con la visión del proyecto.
3. **Respetar el Roadmap:** Consultar `docs/planning/ROADMAP.md` para entender las dependencias entre features.

## Modular-First Mandate
- **Sin sobrecarga de archivos:** Features nuevas en archivos/módulos separados.
- **Extracción de componentes:** Cualquier componente UI con más de 50 líneas de lógica o
  estado complejo DEBE extraerse a un subdirectorio `components/` dentro de su módulo de feature.
- **Pantallas como orquestadoras:** Las pantallas (`*Screen.kt`) solo coordinan componentes.
  La lógica va en ViewModels/UseCases.
- **Estado UI inmutable:** Estado desde ViewModel (`collectAsStateWithLifecycle`), acciones
  hacia arriba mediante lambdas. Componentes "dumb" siempre que sea posible.

## Session Persistence Mandate
- Cada acción de juego (robar, descartar, voltear, pasar) DEBE escribirse en Room como un
  `DrawEvent` ANTES de ejecutar la animación.
- El estado de la sesión se reconstruye desde el event log, no desde snapshots mutables.
- Esto garantiza crash recovery, undo, e historial de sesión sin código adicional.

## Card Content Model
Las cartas soportan 8 modos de contenido (`CardContentMode`):
`IMAGE_ONLY`, `IMAGE_WITH_TEXT`, `REVERSIBLE`, `TOP_BOTTOM_SPLIT`,
`LEFT_RIGHT_SPLIT`, `FOUR_EDGE_CUES`, `FOUR_QUADRANT`, `DOUBLE_SIDED_FULL`.
Cada mazo tiene un `defaultContentMode`; cada carta puede sobreescribir el suyo.

## PDF Import
Usar `barteksc/PdfiumAndroid` para renderizar PDFs a Bitmap.
El recorte de cartas es matemático (Kotlin puro) para los 4 modos de layout.
OpenCV (`zynkware/Document-Scanning-Android-SDK`) reservado para auto-detección en Fase 2.

## UX Mandates
- **Screen Wake Lock:** `FLAG_KEEP_SCREEN_ON` activo mientras `SessionScreen` está en primer plano.
- **Haptic feedback:** Pulso fuerte en cada acción de robo de carta (configurable, default ON).
- **Dark theme:** Default ON — mesas de juego suelen ser con poca luz.
- **Bottom Nav:** Exactamente 4 ítems. FAB central = acción primaria (Robar / Nueva Sesión).
- **Touch targets:** Mínimo 48×48dp en todos los elementos interactivos.
