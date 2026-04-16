# Plan: Robustez Técnica y Backup (Fase 3)

Plan para asegurar que los datos del usuario estén protegidos y las operaciones pesadas sean resilientes.

## WorkManager (E-5)
- Migrar `ImportDeckUseCase` a un `CoroutineWorker`.
- Notificaciones de progreso persistentes.

## Backup Global (D-4)
- Exportación total a `.zip` (Library + DB snapshot).
- Restauración atómica de la biblioteca.

## Calidad e Imágenes (A-4, A-5)
- Configuración de compresión JPEG en Settings.
- Reporte detallado de errores de importación.

## Estado de Implementación
- [ ] Base de WorkManager con Hilt.
- [ ] Implementación de `BackupUseCase`.
- [ ] Exportador de DB a JSON/SQL dump.
