package com.deckapp.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Historial de migraciones de la base de datos DeckApp.
 * 
 * Versión 10: Introducción de Random Tables.
 * Versión 11: Añade category a RandomTable.
 * Versión 12: Añade timestamp a TableRollResult.
 * Versión 13: Añade isPinned a RandomTable.
 * Versión 14: Añade confidence a TableEntry.
 * Versión 15: Módulo de Combate (Encounters, Creatures, Log).
 * Versión 16: Tabla recent_files para SAF.
 * Versión 17: Relación Session <-> RandomTable (SessionTableRef).
 * Versión 18: Tabla table_bundles para agrupar tablas.
 * Versión 19: Relación RandomTable -> TableBundle (bundleId).
 * Versión 20: Reestructuración de FKs en tablas aleatorias para mayor seguridad.
 * Versión 21: Añade isNoRepeat a RandomTable.
 */

val MIGRATION_6_7 = object : Migration(6, 7) { override fun migrate(database: SupportSQLiteDatabase) {} }
val MIGRATION_7_8 = object : Migration(7, 8) { override fun migrate(database: SupportSQLiteDatabase) {} }
val MIGRATION_8_9 = object : Migration(8, 9) { override fun migrate(database: SupportSQLiteDatabase) {} }
val MIGRATION_9_10 = object : Migration(9, 10) { override fun migrate(database: SupportSQLiteDatabase) {} }

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `random_tables` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'General'")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `table_roll_results` ADD COLUMN `timestamp` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `random_tables` ADD COLUMN `isPinned` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `table_entries` ADD COLUMN `confidence` REAL NOT NULL DEFAULT 1.0")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
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
                `createdAt` INTEGER NOT NULL, 
                FOREIGN KEY(`linkedSessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL 
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_encounters_linkedSessionId` ON `encounters` (`linkedSessionId`)")

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
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_encounter_creatures_encounterId` ON `encounter_creatures` (`encounterId`)")

        // 3. Crear tabla combat_log
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

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `recent_files` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `uri` TEXT NOT NULL, 
                `name` TEXT NOT NULL, 
                `type` TEXT NOT NULL, 
                `lastAccessed` INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recent_files_uri` ON `recent_files` (`uri`)")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `session_table_refs` (
                `sessionId` INTEGER NOT NULL, 
                `tableId` INTEGER NOT NULL, 
                `sortOrder` INTEGER NOT NULL, 
                PRIMARY KEY(`sessionId`, `tableId`), 
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                FOREIGN KEY(`tableId`) REFERENCES `random_tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_session_table_refs_tableId` ON `session_table_refs` (`tableId`)")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `table_bundles` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `description` TEXT NOT NULL, 
                `sourceUri` TEXT, 
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `random_tables` ADD COLUMN `bundleId` INTEGER REFERENCES `table_bundles`(`id`) ON DELETE SET NULL")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_random_tables_bundleId` ON `random_tables` (`bundleId`)")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Recrear table_bundles (asegurar schema limpio)
        database.execSQL("DROP TABLE IF EXISTS `table_bundles`")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `table_bundles` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `description` TEXT NOT NULL, 
                `sourceUri` TEXT, 
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())

        // 2. Recrear random_tables para añadir bundleId y FK
        database.execSQL("PRAGMA foreign_keys=OFF")
        
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `random_tables_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `bundleId` INTEGER, 
                `name` TEXT NOT NULL, 
                `description` TEXT NOT NULL, 
                `rollFormula` TEXT NOT NULL, 
                `rollMode` TEXT NOT NULL, 
                `isPinned` INTEGER NOT NULL, 
                `sourceType` TEXT NOT NULL, 
                `sourceName` TEXT, 
                `isBuiltIn` INTEGER NOT NULL, 
                `createdAt` INTEGER NOT NULL, 
                FOREIGN KEY(`bundleId`) REFERENCES `table_bundles`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL 
            )
        """.trimIndent())

        // Copiar datos existentes (bundleId será NULL)
        database.execSQL("""
            INSERT INTO random_tables_new (id, bundleId, name, description, rollFormula, rollMode, isPinned, sourceType, sourceName, isBuiltIn, createdAt)
            SELECT id, NULL, name, description, rollFormula, rollMode, isPinned, sourceType, sourceName, isBuiltIn, createdAt FROM random_tables
        """.trimIndent())

        database.execSQL("DROP TABLE random_tables")
        database.execSQL("ALTER TABLE random_tables_new RENAME TO random_tables")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_random_tables_bundleId` ON `random_tables` (`bundleId`)")

        // 3. Recrear dependencias de random_tables para mantener integridad de FKs
        
        // A. table_entries
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `table_entries_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `tableId` INTEGER NOT NULL, 
                `minRoll` INTEGER NOT NULL, 
                `maxRoll` INTEGER NOT NULL, 
                `weight` INTEGER NOT NULL, 
                `text` TEXT NOT NULL, 
                `subTableRef` TEXT, 
                `subTableId` INTEGER, 
                `sortOrder` INTEGER NOT NULL, 
                FOREIGN KEY(`tableId`) REFERENCES `random_tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("INSERT INTO table_entries_new SELECT * FROM table_entries")
        database.execSQL("DROP TABLE table_entries")
        database.execSQL("ALTER TABLE table_entries_new RENAME TO table_entries")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_table_entries_tableId` ON `table_entries` (`tableId`)")

        // B. random_table_tags
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `random_table_tags_new` (
                `tableId` INTEGER NOT NULL, 
                `tagId` INTEGER NOT NULL, 
                PRIMARY KEY(`tableId`, `tagId`), 
                FOREIGN KEY(`tableId`) REFERENCES `random_tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("INSERT INTO random_table_tags_new SELECT * FROM random_table_tags")
        database.execSQL("DROP TABLE random_table_tags")
        database.execSQL("ALTER TABLE random_table_tags_new RENAME TO random_table_tags")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_random_table_tags_tagId` ON `random_table_tags` (`tagId`)")

        // C. table_roll_results
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `table_roll_results_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `tableId` INTEGER NOT NULL, 
                `tableName` TEXT NOT NULL, 
                `sessionId` INTEGER, 
                `rollValue` INTEGER NOT NULL, 
                `resolvedText` TEXT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                FOREIGN KEY(`tableId`) REFERENCES `random_tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("INSERT INTO table_roll_results_new SELECT * FROM table_roll_results")
        database.execSQL("DROP TABLE table_roll_results")
        database.execSQL("ALTER TABLE table_roll_results_new RENAME TO table_roll_results")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_table_roll_results_sessionId` ON `table_roll_results` (`sessionId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_table_roll_results_tableId` ON `table_roll_results` (`tableId`)")

        // D. session_table_refs
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `session_table_refs_new` (
                `sessionId` INTEGER NOT NULL, 
                `tableId` INTEGER NOT NULL, 
                `sortOrder` INTEGER NOT NULL, 
                PRIMARY KEY(`sessionId`, `tableId`), 
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
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

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `random_tables` ADD COLUMN `isNoRepeat` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. FTS para Cards
        database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `cards_fts` USING FTS4(`title`, `dm_notes`, content=`cards`)")
        
        // 2. FTS para Card Faces
        database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `card_faces_fts` USING FTS4(`name`, content=`card_faces`)")
        
        // 3. FTS para Random Tables
        database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `random_tables_fts` USING FTS4(`name`, `description`, content=`random_tables`)")
        
        // 4. FTS para Table Entries
        database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `table_entries_fts` USING FTS4(`text`, content=`table_entries`)")
        
        // Sincronizar contenido inicial
        database.execSQL("INSERT INTO `cards_fts`(`cards_fts`) VALUES('rebuild')")
        database.execSQL("INSERT INTO `card_faces_fts`(`card_faces_fts`) VALUES('rebuild')")
        database.execSQL("INSERT INTO `random_tables_fts`(`random_tables_fts`) VALUES('rebuild')")
        database.execSQL("INSERT INTO `table_entries_fts`(`table_entries_fts`) VALUES('rebuild')")
    }
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Crear tabla collections
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `collections` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `description` TEXT NOT NULL, 
                `color` INTEGER NOT NULL, 
                `iconName` TEXT NOT NULL, 
                `created_at` INTEGER NOT NULL
            )
        """.trimIndent())

        // 2. Crear tabla collection_resource_refs
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `collection_resource_refs` (
                `collectionId` INTEGER NOT NULL, 
                `resourceId` INTEGER NOT NULL, 
                `resourceType` TEXT NOT NULL, 
                PRIMARY KEY(`collectionId`, `resourceId`, `resourceType`),
                FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_collection_resource_refs_collectionId` ON `collection_resource_refs` (`collectionId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_collection_resource_refs_resourceId_resourceType` ON `collection_resource_refs` (`resourceId`, `resourceType`)")
    }
}


val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Crear tabla npcs
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `npcs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `imagePath` TEXT,
                `maxHp` INTEGER NOT NULL,
                `currentHp` INTEGER NOT NULL,
                `armorClass` INTEGER NOT NULL,
                `initiativeBonus` INTEGER NOT NULL,
                `notes` TEXT NOT NULL,
                `isMonster` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())

        // 2. Crear tabla npc_tags
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `npc_tags` (
                `npcId` INTEGER NOT NULL,
                `tagId` INTEGER NOT NULL,
                PRIMARY KEY(`npcId`, `tagId`),
                FOREIGN KEY(`npcId`) REFERENCES `npcs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE ,
                FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_npc_tags_tagId` ON `npc_tags` (`tagId`)")

        // 3. Alterar encounter_creatures para añadir npcId e imagePath
        database.execSQL("ALTER TABLE `encounter_creatures` ADD COLUMN `npcId` INTEGER DEFAULT NULL")
        database.execSQL("ALTER TABLE `encounter_creatures` ADD COLUMN `imagePath` TEXT DEFAULT NULL")

        // 4. FTS para Collections (faltaba en migración anterior)
        database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `collections_fts` USING FTS4(`name`, `description`, content=`collections`)")
        database.execSQL("INSERT INTO `collections_fts`(`collections_fts`) VALUES('rebuild')")
    }
}

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Recrear tabla de sesiones (para eliminar isActive y asegurar esquema exacto)
        database.execSQL("ALTER TABLE `sessions` RENAME TO `sessions_old`")
        
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `sessions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `status` TEXT NOT NULL, 
                `scheduledDate` INTEGER, 
                `summary` TEXT, 
                `createdAt` INTEGER NOT NULL, 
                `endedAt` INTEGER, 
                `showCardTitles` INTEGER NOT NULL, 
                `dm_notes` TEXT
            )
        """.trimIndent())

        // Migrar datos de la vieja a la nueva
        database.execSQL("""
            INSERT INTO `sessions` (id, name, status, createdAt, endedAt, showCardTitles, dm_notes)
            SELECT id, name, 
                CASE WHEN isActive = 1 THEN 'ACTIVE' ELSE 'COMPLETED' END, 
                createdAt, endedAt, showCardTitles, dm_notes
            FROM `sessions_old`
        """.trimIndent())

        database.execSQL("DROP TABLE `sessions_old`")

        // 2. Autocuración de encounter_creatures (si se saltó la migración 23-24)
        try {
            database.execSQL("ALTER TABLE `encounter_creatures` ADD COLUMN `npcId` INTEGER DEFAULT NULL")
        } catch (e: Exception) { /* Ignorar si ya existe */ }
        
        try {
            database.execSQL("ALTER TABLE `encounter_creatures` ADD COLUMN `imagePath` TEXT DEFAULT NULL")
        } catch (e: Exception) { /* Ignorar si ya existe */ }

        // 3. Recreación de NPCs y npc_tags (para sanear estados corruptos columns={})
        database.execSQL("DROP TABLE IF EXISTS `npc_tags`")
        database.execSQL("DROP TABLE IF EXISTS `npcs`")
        
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `npcs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `description` TEXT NOT NULL, 
                `imagePath` TEXT, 
                `maxHp` INTEGER NOT NULL, 
                `currentHp` INTEGER NOT NULL, 
                `armorClass` INTEGER NOT NULL, 
                `initiativeBonus` INTEGER NOT NULL, 
                `notes` TEXT NOT NULL, 
                `isMonster` INTEGER NOT NULL, 
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `npc_tags` (
                `npcId` INTEGER NOT NULL, 
                `tagId` INTEGER NOT NULL, 
                PRIMARY KEY(`npcId`, `tagId`), 
                FOREIGN KEY(`npcId`) REFERENCES `npcs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_npc_tags_tagId` ON `npc_tags` (`tagId`)")

        // 4. Tablas de Wiki
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `wiki_categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `iconName` TEXT NOT NULL
            )
        """.trimIndent())

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `wiki_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `title` TEXT NOT NULL, 
                `content` TEXT NOT NULL, 
                `categoryId` INTEGER NOT NULL, 
                `imagePath` TEXT, 
                `lastUpdated` INTEGER NOT NULL, 
                FOREIGN KEY(`categoryId`) REFERENCES `wiki_categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_wiki_entries_categoryId` ON `wiki_entries` (`categoryId`)")
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `card_stacks` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `random_tables` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Añadir gameSystemsJson a sessions
        database.execSQL("""ALTER TABLE `sessions` ADD COLUMN `gameSystemsJson` TEXT NOT NULL DEFAULT '["General"]'""")

        // 2. Tablas de referencia
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `reference_tables` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL DEFAULT '',
                `gameSystem` TEXT NOT NULL DEFAULT 'General',
                `category` TEXT NOT NULL DEFAULT 'General',
                `columnsJson` TEXT NOT NULL DEFAULT '[]',
                `isPinned` INTEGER NOT NULL DEFAULT 0,
                `sortOrder` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `reference_rows` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `tableId` INTEGER NOT NULL,
                `cellsJson` TEXT NOT NULL DEFAULT '[]',
                `sortOrder` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`tableId`) REFERENCES `reference_tables`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_reference_rows_tableId` ON `reference_rows` (`tableId`)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `reference_table_tags` (
                `tableId` INTEGER NOT NULL,
                `tagId` INTEGER NOT NULL,
                PRIMARY KEY(`tableId`, `tagId`),
                FOREIGN KEY(`tableId`) REFERENCES `reference_tables`(`id`) ON DELETE CASCADE,
                FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_reference_table_tags_tagId` ON `reference_table_tags` (`tagId`)")

        // 3. Reglas de sistema
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `system_rules` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `content` TEXT NOT NULL DEFAULT '',
                `gameSystem` TEXT NOT NULL DEFAULT 'General',
                `category` TEXT NOT NULL DEFAULT 'General',
                `isPinned` INTEGER NOT NULL DEFAULT 0,
                `sortOrder` INTEGER NOT NULL DEFAULT 0,
                `lastUpdated` INTEGER NOT NULL
            )
        """.trimIndent())

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `system_rule_tags` (
                `ruleId` INTEGER NOT NULL,
                `tagId` INTEGER NOT NULL,
                PRIMARY KEY(`ruleId`, `tagId`),
                FOREIGN KEY(`ruleId`) REFERENCES `system_rules`(`id`) ON DELETE CASCADE,
                FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_system_rule_tags_tagId` ON `system_rule_tags` (`tagId`)")

        // 4. FTS
        database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `reference_tables_fts` USING FTS4(`name`, `description`, content=`reference_tables`)")
        database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `system_rules_fts` USING FTS4(`title`, `content`, content=`system_rules`)")
    }
}

val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Columnas de Starter Pack
        database.execSQL("ALTER TABLE `reference_tables` ADD COLUMN `sourcePack` TEXT")
        database.execSQL("ALTER TABLE `system_rules` ADD COLUMN `sourcePack` TEXT")
        database.execSQL("ALTER TABLE `random_tables` ADD COLUMN `sourcePack` TEXT")

        // 2. Autocuración de la tabla npcs (Corrige IllegalStateException columns={})
        val cursor = database.query("PRAGMA table_info(`npcs`)")
        val columnCount = cursor.count
        cursor.close()

        if (columnCount == 0) {
            database.execSQL("DROP TABLE IF EXISTS `npc_tags`")
            database.execSQL("DROP TABLE IF EXISTS `npcs`")
            
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `npcs` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `description` TEXT NOT NULL, 
                    `imagePath` TEXT, 
                    `maxHp` INTEGER NOT NULL, 
                    `currentHp` INTEGER NOT NULL, 
                    `armorClass` INTEGER NOT NULL, 
                    `initiativeBonus` INTEGER NOT NULL, 
                    `notes` TEXT NOT NULL, 
                    `isMonster` INTEGER NOT NULL, 
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())

            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `npc_tags` (
                    `npcId` INTEGER NOT NULL, 
                    `tagId` INTEGER NOT NULL, 
                    PRIMARY KEY(`npcId`, `tagId`), 
                    FOREIGN KEY(`npcId`) REFERENCES `npcs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_npc_tags_tagId` ON `npc_tags` (`tagId`)")
        }
    }
}

val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `random_tables` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'General'")
    }
}

val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `hex_maps` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `rows` INTEGER NOT NULL,
                `cols` INTEGER NOT NULL,
                `sessionId` INTEGER,
                `hexStyle` TEXT NOT NULL DEFAULT 'FLAT_TOP',
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `hex_tiles` (
                `mapId` INTEGER NOT NULL,
                `q` INTEGER NOT NULL,
                `r` INTEGER NOT NULL,
                `terrainCost` INTEGER NOT NULL DEFAULT 1,
                `terrainLabel` TEXT NOT NULL DEFAULT '',
                `terrainColor` INTEGER NOT NULL DEFAULT -2183045,
                `dmNotes` TEXT NOT NULL DEFAULT '',
                `playerNotes` TEXT NOT NULL DEFAULT '',
                `isExplored` INTEGER NOT NULL DEFAULT 0,
                `isReconnoitered` INTEGER NOT NULL DEFAULT 0,
                `isMapped` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`mapId`, `q`, `r`),
                FOREIGN KEY(`mapId`) REFERENCES `hex_maps`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_hex_tiles_mapId` ON `hex_tiles` (`mapId`)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `hex_pois` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `mapId` INTEGER NOT NULL,
                `tileQ` INTEGER NOT NULL,
                `tileR` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `description` TEXT NOT NULL DEFAULT '',
                `encounterId` INTEGER,
                `tableId` INTEGER,
                FOREIGN KEY(`mapId`) REFERENCES `hex_maps`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_hex_pois_mapId` ON `hex_pois` (`mapId`)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `hex_days` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `mapId` INTEGER NOT NULL,
                `dayNumber` INTEGER NOT NULL,
                `activitiesLog` TEXT NOT NULL DEFAULT '[]',
                `notes` TEXT NOT NULL DEFAULT '',
                FOREIGN KEY(`mapId`) REFERENCES `hex_maps`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_hex_days_mapId` ON `hex_days` (`mapId`)")
    }
}

val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `hex_maps` ADD COLUMN `partyQ` INTEGER DEFAULT NULL")
        database.execSQL("ALTER TABLE `hex_maps` ADD COLUMN `partyR` INTEGER DEFAULT NULL")
    }
}

val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `hex_maps` ADD COLUMN `isRadial` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `hex_maps` ADD COLUMN `maxActivitiesPerDay` INTEGER NOT NULL DEFAULT 8")
        database.execSQL("ALTER TABLE `hex_maps` ADD COLUMN `mapNotes` TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `hex_maps` ADD COLUMN `weatherTableId` INTEGER DEFAULT NULL")
        database.execSQL("ALTER TABLE `hex_maps` ADD COLUMN `travelEventTableId` INTEGER DEFAULT NULL")
        database.execSQL("ALTER TABLE `hex_maps` ADD COLUMN `terrainTableConfig` TEXT NOT NULL DEFAULT '{}'")
    }
}

val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. table_entries.confidence (missing from recreate in 19-20)
        try {
            database.execSQL("ALTER TABLE `table_entries` ADD COLUMN `confidence` REAL NOT NULL DEFAULT 1.0")
        } catch (e: Exception) { /* already exists */ }

        // 2. hex_maps columns (missing from some v32 states)
        try {
            database.execSQL("ALTER TABLE `hex_maps` ADD COLUMN `isRadial` INTEGER NOT NULL DEFAULT 0")
        } catch (e: Exception) { /* already exists */ }
        
        try {
            database.execSQL("ALTER TABLE `hex_maps` ADD COLUMN `maxActivitiesPerDay` INTEGER NOT NULL DEFAULT 8")
        } catch (e: Exception) { /* already exists */ }
        
        try {
            database.execSQL("ALTER TABLE `hex_maps` ADD COLUMN `mapNotes` TEXT NOT NULL DEFAULT ''")
        } catch (e: Exception) { /* already exists */ }

        // 3. random_tables.sourcePack (missing from some v27+ states)
        try {
            database.execSQL("ALTER TABLE `random_tables` ADD COLUMN `sourcePack` TEXT")
        } catch (e: Exception) { /* already exists */ }
    }
}


