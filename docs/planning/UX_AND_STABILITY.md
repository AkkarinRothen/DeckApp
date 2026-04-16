# Plan: UX y Estabilidad Pro (Fase 2.5)

Este plan aborda las mejoras críticas de experiencia de usuario y robustez técnica identificadas en el Roadmap.

## Mejoras de UX
- **[C-1] Drag & Drop Reorder:** Implementar reordenamiento visual de cartas en `DeckDetailScreen`.
- **[C-4] Undo Delete:** Sistema de Snackbar para "Deshacer" borrados accidentales de cartas/mazos.

## Estabilidad Técnica
- **[A-2] Auditoría de Migraciones:** Formalizar el uso de `AutoMigration` y objetos `Migration` en Room.
- **[UX] Vibración Háptica:** Integrar feedback táctil al seleccionar/reordenar cartas.

## Estado de Implementación
- [ ] Integrar librería de reordenamiento para Compose.
- [ ] Implementar `UndoManager` básico (borrado diferido).
- [ ] Probar migraciones de DB (v8 -> v9).
