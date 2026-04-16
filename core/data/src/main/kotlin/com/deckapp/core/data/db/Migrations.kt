package com.deckapp.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Sprint 15: notas del DM por carta
        database.execSQL("ALTER TABLE cards ADD COLUMN dmNotes TEXT")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE cards ADD COLUMN linkedTableId INTEGER")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Sprint 13: archivar mazos
        database.execSQL(
            "ALTER TABLE card_stacks ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // B-2: columna isRevealed para cartas robadas boca abajo
        database.execSQL(
            "ALTER TABLE cards ADD COLUMN isRevealed INTEGER NOT NULL DEFAULT 1"
        )
        // A-3: índices para búsquedas por tag (cross-ref tables)
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_card_stack_tags_tagId` ON `card_stack_tags` (`tagId`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_card_tags_tagId` ON `card_tags` (`tagId`)"
        )
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE INDEX IF NOT EXISTS index_session_deck_refs_stackId ON session_deck_refs(stackId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_session_table_refs_tableId ON session_table_refs(tableId)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Crear tabla cruzada para etiquetas de tablas
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `random_table_tags` (
                `tableId` INTEGER NOT NULL, 
                `tagId` INTEGER NOT NULL, 
                PRIMARY KEY(`tableId`, `tagId`), 
                FOREIGN KEY(`tableId`) REFERENCES `random_tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_random_table_tags_tagId` ON `random_table_tags` (`tagId`)")

        // 2. Rescatar categorías y convertirlas en Tags
        // Color por defecto: 0xFF6200EE (Purple 500 / Primary) -> Int: -10354450
        database.execSQL("""
            INSERT INTO tags (name, color) 
            SELECT DISTINCT category, -10354450 FROM random_tables 
            WHERE category IS NOT NULL AND category != '' 
            AND category NOT IN (SELECT name FROM tags)
        """.trimIndent())

        // 3. Vincular tablas con sus nuevos tags
        database.execSQL("""
            INSERT INTO random_table_tags (tableId, tagId) 
            SELECT rt.id, t.id FROM random_tables rt 
            JOIN tags t ON rt.category = t.name
        """.trimIndent())

        // 4. Recrear random_tables para eliminar la columna 'category'
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `random_tables_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `description` TEXT NOT NULL, 
                `rollFormula` TEXT NOT NULL, 
                `rollMode` TEXT NOT NULL, 
                `isPinned` INTEGER NOT NULL, 
                `sourceType` TEXT NOT NULL, 
                `sourceName` TEXT, 
                `isBuiltIn` INTEGER NOT NULL, 
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())
        
        // Copiar datos (sin category)
        database.execSQL("""
            INSERT INTO random_tables_new (id, name, description, rollFormula, rollMode, isPinned, sourceType, sourceName, isBuiltIn, createdAt)
            SELECT id, name, description, rollFormula, rollMode, isPinned, sourceType, sourceName, isBuiltIn, createdAt FROM random_tables
        """.trimIndent())
        
        database.execSQL("DROP TABLE random_tables")
        database.execSQL("ALTER TABLE random_tables_new RENAME TO random_tables")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 0. Recrear cards para añadir FK originDeckId (requerido por Room v13)
        // SQLite no permite añadir FOREIGN KEY a una tabla existente.
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `cards_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `stackId` INTEGER NOT NULL, 
                `originDeckId` INTEGER, 
                `title` TEXT NOT NULL, 
                `suit` TEXT, 
                `value` INTEGER, 
                `currentFaceIndex` INTEGER NOT NULL, 
                `currentRotation` INTEGER NOT NULL, 
                `isReversed` INTEGER NOT NULL, 
                `isDrawn` INTEGER NOT NULL, 
                `isRevealed` INTEGER NOT NULL, 
                `sortOrder` INTEGER NOT NULL, 
                `linkedTableId` INTEGER, 
                `dmNotes` TEXT, 
                FOREIGN KEY(`stackId`) REFERENCES `card_stacks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`originDeckId`) REFERENCES `card_stacks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        
        // Copiar datos existentes
        database.execSQL("""
            INSERT INTO cards_new (id, stackId, originDeckId, title, suit, value, currentFaceIndex, currentRotation, isReversed, isDrawn, isRevealed, sortOrder, linkedTableId, dmNotes)
            SELECT id, stackId, originDeckId, title, suit, value, currentFaceIndex, currentRotation, isReversed, isDrawn, isRevealed, sortOrder, linkedTableId, dmNotes FROM cards
        """.trimIndent())
        
        database.execSQL("DROP TABLE cards")
        database.execSQL("ALTER TABLE cards_new RENAME TO cards")
        
        // Recrear índices
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_stackId` ON `cards` (`stackId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_originDeckId` ON `cards` (`originDeckId`)")

        // 1. Asegurar índice en session_deck_refs (resolviendo el IllegalStateException previo)
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_session_deck_refs_stackId` ON `session_deck_refs` (`stackId`)")

        // 2. Recrear draw_events para añadir FK a cards y CASCADE
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `draw_events_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `sessionId` INTEGER NOT NULL, 
                `cardId` INTEGER NOT NULL, 
                `action` TEXT NOT NULL, 
                `metadata` TEXT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`cardId`) REFERENCES `cards`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("INSERT INTO draw_events_new (id, sessionId, cardId, action, metadata, timestamp) SELECT id, sessionId, cardId, action, metadata, timestamp FROM draw_events")
        database.execSQL("DROP TABLE draw_events")
        database.execSQL("ALTER TABLE draw_events_new RENAME TO draw_events")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_draw_events_sessionId` ON `draw_events` (`sessionId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_draw_events_cardId` ON `draw_events` (`cardId`)")

        // 3. Recrear table_roll_results para añadir FKs y CASCADE
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `table_roll_results_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `tableId` INTEGER NOT NULL, 
                `tableName` TEXT NOT NULL, 
                `sessionId` INTEGER, 
                `rollValue` INTEGER NOT NULL, 
                `resolvedText` TEXT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                FOREIGN KEY(`tableId`) REFERENCES `random_tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("INSERT INTO table_roll_results_new (id, tableId, tableName, sessionId, rollValue, resolvedText, timestamp) SELECT id, tableId, tableName, sessionId, rollValue, resolvedText, timestamp FROM table_roll_results")
        database.execSQL("DROP TABLE table_roll_results")
        database.execSQL("ALTER TABLE table_roll_results_new RENAME TO table_roll_results")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_table_roll_results_sessionId` ON `table_roll_results` (`sessionId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_table_roll_results_tableId` ON `table_roll_results` (`tableId`)")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Cards: last_drawn_at
        // Intentar añadir la columna. Si ya existe (por un intento fallido), el catch evitará el crash.
        try {
            database.execSQL("ALTER TABLE cards ADD COLUMN last_drawn_at INTEGER DEFAULT NULL")
        } catch (e: Exception) {
            // Probablemente ya existe
        }

        // 2. Normalización de Sessions (Limpieza de dm_notes vs dmNotes)
        // Recreamos la tabla sessions y sus dependencias para asegurar un esquema limpio.
        
        // Desactivamos FKs temporalmente para permitir el baile de tablas
        database.execSQL("PRAGMA foreign_keys=OFF")

        // -- SESSIONS --
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `sessions_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `isActive` INTEGER NOT NULL, 
                `createdAt` INTEGER NOT NULL, 
                `endedAt` INTEGER, 
                `showCardTitles` INTEGER NOT NULL, 
                `dmNotes` TEXT
            )
        """.trimIndent())

        // Intentar rescatar datos de dm_notes si existe el duplicado
        val sessionCols = database.query("PRAGMA table_info(sessions)")
        var hasSnakeNotes = false
        while (sessionCols.moveToNext()) {
            val nameIdx = sessionCols.getColumnIndex("name")
            if (nameIdx != -1 && sessionCols.getString(nameIdx) == "dm_notes") {
                hasSnakeNotes = true
            }
        }
        sessionCols.close()

        if (hasSnakeNotes) {
            database.execSQL("""
                INSERT INTO sessions_new (id, name, isActive, createdAt, endedAt, showCardTitles, dmNotes)
                SELECT id, name, isActive, createdAt, endedAt, showCardTitles, COALESCE(dm_notes, dmNotes) FROM sessions
            """.trimIndent())
        } else {
            database.execSQL("""
                INSERT INTO sessions_new (id, name, isActive, createdAt, endedAt, showCardTitles, dmNotes)
                SELECT id, name, isActive, createdAt, endedAt, showCardTitles, dmNotes FROM sessions
            """.trimIndent())
        }

        database.execSQL("DROP TABLE sessions")
        database.execSQL("ALTER TABLE sessions_new RENAME TO sessions")

        // -- DEPENDENCIAS DE SESSIONS (Recreación obligatoria para FKs) --

        // A. session_deck_refs
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `session_deck_refs_new` (
                `sessionId` INTEGER NOT NULL, 
                `stackId` INTEGER NOT NULL, 
                `drawModeOverride` TEXT, 
                `sortOrder` INTEGER NOT NULL, 
                PRIMARY KEY(`sessionId`, `stackId`), 
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`stackId`) REFERENCES `card_stacks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("INSERT INTO session_deck_refs_new SELECT * FROM session_deck_refs")
        database.execSQL("DROP TABLE session_deck_refs")
        database.execSQL("ALTER TABLE session_deck_refs_new RENAME TO session_deck_refs")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_session_deck_refs_stackId` ON `session_deck_refs` (`stackId`)")

        // B. session_table_refs
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `session_table_refs_new` (
                `sessionId` INTEGER NOT NULL, 
                `tableId` INTEGER NOT NULL, 
                `sortOrder` INTEGER NOT NULL, 
                PRIMARY KEY(`sessionId`, `tableId`), 
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`tableId`) REFERENCES `random_tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("INSERT INTO session_table_refs_new SELECT * FROM session_table_refs")
        database.execSQL("DROP TABLE session_table_refs")
        database.execSQL("ALTER TABLE session_table_refs_new RENAME TO session_table_refs")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_session_table_refs_tableId` ON `session_table_refs` (`tableId`)")

        // C. draw_events (Apunta a sessions y a cards)
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `draw_events_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `sessionId` INTEGER NOT NULL, 
                `cardId` INTEGER, 
                `action` TEXT NOT NULL, 
                `metadata` TEXT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`cardId`) REFERENCES `cards`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        // Migramos los datos convirtiendo IDs de carta 0 a NULL para cumplir con la integridad referencial
        database.execSQL("""
            INSERT INTO draw_events_new (id, sessionId, cardId, action, metadata, timestamp) 
            SELECT id, sessionId, CASE WHEN cardId = 0 THEN NULL ELSE cardId END, action, metadata, timestamp 
            FROM draw_events
        """.trimIndent())
        database.execSQL("DROP TABLE draw_events")
        database.execSQL("ALTER TABLE draw_events_new RENAME TO draw_events")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_draw_events_sessionId` ON `draw_events` (`sessionId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_draw_events_cardId` ON `draw_events` (`cardId`)")

        // D. table_roll_results
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `table_roll_results_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `tableId` INTEGER NOT NULL, 
                `tableName` TEXT NOT NULL, 
                `sessionId` INTEGER, 
                `rollValue` INTEGER NOT NULL, 
                `resolvedText` TEXT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                FOREIGN KEY(`tableId`) REFERENCES `random_tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("INSERT INTO table_roll_results_new SELECT * FROM table_roll_results")
        database.execSQL("DROP TABLE table_roll_results")
        database.execSQL("ALTER TABLE table_roll_results_new RENAME TO table_roll_results")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_table_roll_results_sessionId` ON `table_roll_results` (`sessionId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_table_roll_results_tableId` ON `table_roll_results` (`tableId`)")

        database.execSQL("PRAGMA foreign_keys=ON")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("PRAGMA foreign_keys=OFF")

        // 1. Limpieza de Sessions (dmNotes vs dm_notes)
        database.execSQL("CREATE TABLE IF NOT EXISTS `sessions_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `endedAt` INTEGER, `showCardTitles` INTEGER NOT NULL, `dmNotes` TEXT)")
        
        val sessionCols = database.query("PRAGMA table_info(sessions)")
        var hasSnakeNotes = false
        var hasCamelNotes = false
        while (sessionCols.moveToNext()) {
            val nameIdx = sessionCols.getColumnIndex("name")
            if (nameIdx != -1) {
                val name = sessionCols.getString(nameIdx)
                if (name == "dm_notes") hasSnakeNotes = true
                if (name == "dmNotes") hasCamelNotes = true
            }
        }
        sessionCols.close()

        val selectSessions = when {
            hasSnakeNotes && hasCamelNotes -> "id, name, isActive, createdAt, endedAt, showCardTitles, COALESCE(dm_notes, dmNotes)"
            hasSnakeNotes -> "id, name, isActive, createdAt, endedAt, showCardTitles, dm_notes"
            else -> "id, name, isActive, createdAt, endedAt, showCardTitles, dmNotes"
        }
        database.execSQL("INSERT INTO sessions_new (id, name, isActive, createdAt, endedAt, showCardTitles, dmNotes) SELECT $selectSessions FROM sessions")
        database.execSQL("DROP TABLE sessions")
        database.execSQL("ALTER TABLE sessions_new RENAME TO sessions")

        // 2. Limpieza de Cards (Asegurar dmNotes y last_drawn_at)
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `cards_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `stackId` INTEGER NOT NULL, 
                `originDeckId` INTEGER, 
                `title` TEXT NOT NULL, 
                `suit` TEXT, 
                `value` INTEGER, 
                `currentFaceIndex` INTEGER NOT NULL, 
                `currentRotation` INTEGER NOT NULL, 
                `isReversed` INTEGER NOT NULL, 
                `isDrawn` INTEGER NOT NULL, 
                `isRevealed` INTEGER NOT NULL, 
                `sortOrder` INTEGER NOT NULL, 
                `linkedTableId` INTEGER, 
                `dmNotes` TEXT, 
                `last_drawn_at` INTEGER, 
                FOREIGN KEY(`stackId`) REFERENCES `card_stacks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`originDeckId`) REFERENCES `card_stacks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        
        val cardCols = database.query("PRAGMA table_info(cards)")
        var hasLastDrawn = false
        while (cardCols.moveToNext()) {
            val nameIdx = cardCols.getColumnIndex("name")
            if (nameIdx != -1 && cardCols.getString(nameIdx) == "last_drawn_at") hasLastDrawn = true
        }
        cardCols.close()

        val selectCards = if (hasLastDrawn) {
            "id, stackId, originDeckId, title, suit, value, currentFaceIndex, currentRotation, isReversed, isDrawn, isRevealed, sortOrder, linkedTableId, dmNotes, last_drawn_at"
        } else {
            "id, stackId, originDeckId, title, suit, value, currentFaceIndex, currentRotation, isReversed, isDrawn, isRevealed, sortOrder, linkedTableId, dmNotes, NULL"
        }
        database.execSQL("INSERT INTO cards_new (id, stackId, originDeckId, title, suit, value, currentFaceIndex, currentRotation, isReversed, isDrawn, isRevealed, sortOrder, linkedTableId, dmNotes, last_drawn_at) SELECT $selectCards FROM cards")
        database.execSQL("DROP TABLE cards")
        database.execSQL("ALTER TABLE cards_new RENAME TO cards")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_stackId` ON `cards` (`stackId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_originDeckId` ON `cards` (`originDeckId`)")

        // 3. Limpieza de DrawEvents (cardId nullable y corrección de ceros)
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `draw_events_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `sessionId` INTEGER NOT NULL, 
                `cardId` INTEGER, 
                `action` TEXT NOT NULL, 
                `metadata` TEXT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`cardId`) REFERENCES `cards`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("""
            INSERT INTO draw_events_new (id, sessionId, cardId, action, metadata, timestamp) 
            SELECT id, sessionId, CASE WHEN cardId = 0 THEN NULL ELSE cardId END, action, metadata, timestamp FROM draw_events
        """.trimIndent())
        database.execSQL("DROP TABLE draw_events")
        database.execSQL("ALTER TABLE draw_events_new RENAME TO draw_events")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_draw_events_sessionId` ON `draw_events` (`sessionId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_draw_events_cardId` ON `draw_events` (`cardId`)")

        // 4. Recrear referencias (FKs a sessions)
        // session_deck_refs
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `session_deck_refs_new` (
                `sessionId` INTEGER NOT NULL, 
                `stackId` INTEGER NOT NULL, 
                `drawModeOverride` TEXT, 
                `sortOrder` INTEGER NOT NULL, 
                PRIMARY KEY(`sessionId`, `stackId`), 
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`stackId`) REFERENCES `card_stacks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("INSERT INTO session_deck_refs_new SELECT * FROM session_deck_refs")
        database.execSQL("DROP TABLE session_deck_refs")
        database.execSQL("ALTER TABLE session_deck_refs_new RENAME TO session_deck_refs")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_session_deck_refs_stackId` ON `session_deck_refs` (`stackId`)")

        // session_table_refs
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `session_table_refs_new` (
                `sessionId` INTEGER NOT NULL, 
                `tableId` INTEGER NOT NULL, 
                `sortOrder` INTEGER NOT NULL, 
                PRIMARY KEY(`sessionId`, `tableId`), 
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`tableId`) REFERENCES `random_tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("INSERT INTO session_table_refs_new SELECT * FROM session_table_refs")
        database.execSQL("DROP TABLE session_table_refs")
        database.execSQL("ALTER TABLE session_table_refs_new RENAME TO session_table_refs")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_session_table_refs_tableId` ON `session_table_refs` (`tableId`)")

        database.execSQL("PRAGMA foreign_keys=ON")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Crear tabla encounters
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `encounters` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `description` TEXT NOT NULL, 
                `linkedSessionId` INTEGER, 
                `isActive` INTEGER NOT NULL, 
                `currentRound` INTEGER NOT NULL, 
                `currentTurnIndex` INTEGER NOT NULL, 
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())

        // 2. Crear tabla encounter_creatures
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `encounter_creatures` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `encounterId` INTEGER NOT NULL, 
                `name` TEXT NOT NULL, 
                `maxHp` INTEGER NOT NULL, 
                `currentHp` INTEGER NOT NULL, 
                `armorClass` INTEGER NOT NULL, 
                `initiativeBonus` INTEGER NOT NULL, 
                `initiativeRoll` INTEGER, 
                `conditionsJson` TEXT NOT NULL, 
                `notes` TEXT NOT NULL, 
                `sortOrder` INTEGER NOT NULL, 
                FOREIGN KEY(`encounterId`) REFERENCES `encounters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())

        // 3. Crear índice
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_encounter_creatures_encounterId` ON `encounter_creatures` (`encounterId`)")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `combat_log` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `encounterId` INTEGER NOT NULL, 
                `message` TEXT NOT NULL, 
                `type` TEXT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                FOREIGN KEY(`encounterId`) REFERENCES `encounters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_combat_log_encounterId` ON `combat_log` (`encounterId`)")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("PRAGMA foreign_keys=OFF")

        // 1. Reconstruction of SESSIONS
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `sessions_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `isActive` INTEGER NOT NULL, 
                `createdAt` INTEGER NOT NULL, 
                `endedAt` INTEGER, 
                `showCardTitles` INTEGER NOT NULL, 
                `dm_notes` TEXT
            )
        """.trimIndent())

        // Check columns to COALESCE if both exist
        val sessionCols = database.query("PRAGMA table_info(sessions)")
        var hasSnakeNotes = false
        var hasCamelNotes = false
        while (sessionCols.moveToNext()) {
            val nameIdx = sessionCols.getColumnIndex("name")
            if (nameIdx != -1) {
                val name = sessionCols.getString(nameIdx)
                if (name == "dm_notes") hasSnakeNotes = true
                if (name == "dmNotes") hasCamelNotes = true
            }
        }
        sessionCols.close()

        val selectSessions = when {
            hasSnakeNotes && hasCamelNotes -> "id, name, isActive, createdAt, endedAt, showCardTitles, COALESCE(dm_notes, dmNotes)"
            hasSnakeNotes -> "id, name, isActive, createdAt, endedAt, showCardTitles, dm_notes"
            else -> "id, name, isActive, createdAt, endedAt, showCardTitles, dmNotes"
        }

        database.execSQL("INSERT INTO sessions_new (id, name, isActive, createdAt, endedAt, showCardTitles, dm_notes) SELECT $selectSessions FROM sessions")
        database.execSQL("DROP TABLE sessions")
        database.execSQL("ALTER TABLE sessions_new RENAME TO sessions")

        // 2. Reconstruction of CARDS
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `cards_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `stackId` INTEGER NOT NULL, 
                `originDeckId` INTEGER, 
                `title` TEXT NOT NULL, 
                `suit` TEXT, 
                `value` INTEGER, 
                `currentFaceIndex` INTEGER NOT NULL, 
                `currentRotation` INTEGER NOT NULL, 
                `isReversed` INTEGER NOT NULL, 
                `isDrawn` INTEGER NOT NULL, 
                `isRevealed` INTEGER NOT NULL, 
                `sortOrder` INTEGER NOT NULL, 
                `linkedTableId` INTEGER, 
                `dm_notes` TEXT, 
                `last_drawn_at` INTEGER, 
                FOREIGN KEY(`stackId`) REFERENCES `card_stacks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                FOREIGN KEY(`originDeckId`) REFERENCES `card_stacks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())

        val cardCols = database.query("PRAGMA table_info(cards)")
        hasSnakeNotes = false
        hasCamelNotes = false
        while (cardCols.moveToNext()) {
            val nameIdx = cardCols.getColumnIndex("name")
            if (nameIdx != -1) {
                val name = cardCols.getString(nameIdx)
                if (name == "dm_notes") hasSnakeNotes = true
                if (name == "dmNotes") hasCamelNotes = true
            }
        }
        cardCols.close()

        val selectCardsNotes = when {
            hasSnakeNotes && hasCamelNotes -> "COALESCE(dm_notes, dmNotes)"
            hasSnakeNotes -> "dm_notes"
            else -> "dmNotes"
        }

        database.execSQL("""
            INSERT INTO cards_new (id, stackId, originDeckId, title, suit, value, currentFaceIndex, currentRotation, isReversed, isDrawn, isRevealed, sortOrder, linkedTableId, dm_notes, last_drawn_at)
            SELECT id, stackId, originDeckId, title, suit, value, currentFaceIndex, currentRotation, isReversed, isDrawn, isRevealed, sortOrder, linkedTableId, $selectCardsNotes, last_drawn_at FROM cards
        """.trimIndent())

        database.execSQL("DROP TABLE cards")
        database.execSQL("ALTER TABLE cards_new RENAME TO cards")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_stackId` ON `cards` (`stackId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_originDeckId` ON `cards` (`originDeckId`)")

        // 3. Re-recreate dependent tables to ensure FKs remain intact and consistent
        
        // A. session_deck_refs
        database.execSQL("CREATE TABLE IF NOT EXISTS `session_deck_refs_new` (`sessionId` INTEGER NOT NULL, `stackId` INTEGER NOT NULL, `drawModeOverride` TEXT, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`sessionId`, `stackId`), FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`stackId`) REFERENCES `card_stacks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        database.execSQL("INSERT INTO session_deck_refs_new SELECT * FROM session_deck_refs")
        database.execSQL("DROP TABLE session_deck_refs")
        database.execSQL("ALTER TABLE session_deck_refs_new RENAME TO session_deck_refs")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_session_deck_refs_stackId` ON `session_deck_refs` (`stackId`)")

        // B. session_table_refs
        database.execSQL("CREATE TABLE IF NOT EXISTS `session_table_refs_new` (`sessionId` INTEGER NOT NULL, `tableId` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`sessionId`, `tableId`), FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`tableId`) REFERENCES `random_tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        database.execSQL("INSERT INTO session_table_refs_new SELECT * FROM session_table_refs")
        database.execSQL("DROP TABLE session_table_refs")
        database.execSQL("ALTER TABLE session_table_refs_new RENAME TO session_table_refs")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_session_table_refs_tableId` ON `session_table_refs` (`tableId`)")

        // C. draw_events
        database.execSQL("CREATE TABLE IF NOT EXISTS `draw_events_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `cardId` INTEGER, `action` TEXT NOT NULL, `metadata` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`cardId`) REFERENCES `cards`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        database.execSQL("INSERT INTO draw_events_new SELECT * FROM draw_events")
        database.execSQL("DROP TABLE draw_events")
        database.execSQL("ALTER TABLE draw_events_new RENAME TO draw_events")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_draw_events_sessionId` ON `draw_events` (`sessionId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_draw_events_cardId` ON `draw_events` (`cardId`)")

        // D. table_roll_results
        database.execSQL("CREATE TABLE IF NOT EXISTS `table_roll_results_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tableId` INTEGER NOT NULL, `tableName` TEXT NOT NULL, `sessionId` INTEGER, `rollValue` INTEGER NOT NULL, `resolvedText` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, FOREIGN KEY(`tableId`) REFERENCES `random_tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        database.execSQL("INSERT INTO table_roll_results_new SELECT * FROM table_roll_results")
        database.execSQL("DROP TABLE table_roll_results")
        database.execSQL("ALTER TABLE table_roll_results_new RENAME TO table_roll_results")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_table_roll_results_sessionId` ON `table_roll_results` (`sessionId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_table_roll_results_tableId` ON `table_roll_results` (`tableId`)")

        // E. card_faces (Dependent on cards)
        database.execSQL("CREATE TABLE IF NOT EXISTS `card_faces_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cardId` INTEGER NOT NULL, `faceIndex` INTEGER NOT NULL, `name` TEXT NOT NULL, `imagePath` TEXT, `contentMode` TEXT NOT NULL, `zonesJson` TEXT NOT NULL, `reversedImagePath` TEXT, FOREIGN KEY(`cardId`) REFERENCES `cards`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        database.execSQL("INSERT INTO card_faces_new SELECT * FROM card_faces")
        database.execSQL("DROP TABLE card_faces")
        database.execSQL("ALTER TABLE card_faces_new RENAME TO card_faces")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_card_faces_cardId` ON `card_faces` (`cardId`)")

        // F. card_tags (Dependent on cards)
        database.execSQL("CREATE TABLE IF NOT EXISTS `card_tags_new` (`cardId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, PRIMARY KEY(`cardId`, `tagId`), FOREIGN KEY(`cardId`) REFERENCES `cards`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        database.execSQL("INSERT INTO card_tags_new SELECT * FROM card_tags")
        database.execSQL("DROP TABLE card_tags")
        database.execSQL("ALTER TABLE card_tags_new RENAME TO card_tags")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_card_tags_tagId` ON `card_tags` (`tagId`)")

        // G. encounters (Dependent on sessions)
        database.execSQL("CREATE TABLE IF NOT EXISTS `encounters_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `linkedSessionId` INTEGER, `isActive` INTEGER NOT NULL, `currentRound` INTEGER NOT NULL, `currentTurnIndex` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`linkedSessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
        database.execSQL("INSERT INTO encounters_new SELECT id, name, description, linkedSessionId, isActive, currentRound, currentTurnIndex, createdAt FROM encounters")
        database.execSQL("DROP TABLE encounters")
        database.execSQL("ALTER TABLE encounters_new RENAME TO encounters")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_encounters_linkedSessionId` ON `encounters` (`linkedSessionId`)")

        // H. encounter_creatures (Dependent on encounters)
        database.execSQL("CREATE TABLE IF NOT EXISTS `encounter_creatures_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `encounterId` INTEGER NOT NULL, `name` TEXT NOT NULL, `maxHp` INTEGER NOT NULL, `currentHp` INTEGER NOT NULL, `armorClass` INTEGER NOT NULL, `initiativeBonus` INTEGER NOT NULL, `initiativeRoll` INTEGER, `conditionsJson` TEXT NOT NULL, `notes` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, FOREIGN KEY(`encounterId`) REFERENCES `encounters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        database.execSQL("INSERT INTO encounter_creatures_new SELECT * FROM encounter_creatures")
        database.execSQL("DROP TABLE encounter_creatures")
        database.execSQL("ALTER TABLE encounter_creatures_new RENAME TO encounter_creatures")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_encounter_creatures_encounterId` ON `encounter_creatures` (`encounterId`)")

        // I. combat_log (Dependent on encounters)
        database.execSQL("CREATE TABLE IF NOT EXISTS `combat_log_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `encounterId` INTEGER NOT NULL, `message` TEXT NOT NULL, `type` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, FOREIGN KEY(`encounterId`) REFERENCES `encounters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        database.execSQL("INSERT INTO combat_log_new SELECT * FROM combat_log")
        database.execSQL("DROP TABLE combat_log")
        database.execSQL("ALTER TABLE combat_log_new RENAME TO combat_log")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_combat_log_encounterId` ON `combat_log` (`encounterId`)")

        database.execSQL("PRAGMA foreign_keys=ON")
    }
}
