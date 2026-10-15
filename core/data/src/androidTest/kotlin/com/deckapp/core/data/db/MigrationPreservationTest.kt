package com.deckapp.core.data.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.deckapp.core.data.di.DatabaseModule
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MigrationPreservationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext.also {
        check(it.packageName != "com.deckapp.ttrpg") { "Migration tests must never use the user's app data" }
    }

    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
        DeckAppDatabase::class.java.canonicalName!!, FrameworkSQLiteOpenHelperFactory())

    private val tables = listOf("card_stacks", "cards", "card_faces", "tags", "card_stack_tags", "card_tags",
        "table_bundles", "random_tables", "table_entries", "random_table_tags", "sessions", "session_deck_refs",
        "session_table_refs", "collections", "collection_resource_refs", "npcs", "npc_tags", "wiki_categories", "wiki_entries")

    @After fun cleanupIsolatedDatabase() { context.deleteDatabase(DeckAppDatabase.DATABASE_NAME) }

    @Test fun upgrade37To41PreservesResourcesAndRelations() = verifyUpgrade(37)
    @Test fun upgrade40To41PreservesResourcesAndRelations() = verifyUpgrade(40)

    @Test fun productionConfigurationPreservesCurrentDatabaseWhenReopened() {
        context.deleteDatabase(DeckAppDatabase.DATABASE_NAME)
        val original = DatabaseModule.provideDeckAppDatabase(context)
        val before = seed(original.openHelper.writableDatabase)
        original.close()
        val reopened = DatabaseModule.provideDeckAppDatabase(context)
        try { assertPreserved(before, reopened.openHelper.writableDatabase) } finally { reopened.close() }
    }

    @Test fun unsupportedUpgradeFailsWithoutErasingExistingDatabase() {
        context.deleteDatabase(DeckAppDatabase.DATABASE_NAME)
        context.openOrCreateDatabase(DeckAppDatabase.DATABASE_NAME, 0, null).use { old ->
            old.execSQL("CREATE TABLE preserved_material (name TEXT NOT NULL)")
            old.execSQL("INSERT INTO preserved_material VALUES ('keep me')")
            old.version = 5
        }
        val room = DatabaseModule.provideDeckAppDatabase(context)
        try {
            try { room.openHelper.writableDatabase; fail("Unsupported migration must fail closed") }
            catch (expected: IllegalStateException) { assertTrue(expected.message.orEmpty().contains("migration", true)) }
        } finally { room.close() }
        context.openOrCreateDatabase(DeckAppDatabase.DATABASE_NAME, 0, null).use { old ->
            assertEquals(5, old.version)
            old.rawQuery("SELECT name FROM preserved_material", null).use { rows ->
                assertTrue(rows.moveToFirst()); assertEquals("keep me", rows.getString(0))
            }
        }
    }

    @Test fun registryHasEveryUpgradeRoute() {
        val registry = RoomDatabase.MigrationContainer().apply { addMigrations(*ALL_MIGRATIONS) }
        for (version in 6..40) assertNotNull("Missing route $version → 41", registry.findMigrationPath(version, 41))
    }

    private fun verifyUpgrade(version: Int) {
        val name = "preservation-$version"
        val before = helper.createDatabase(name, version).use { seed(it) }
        helper.runMigrationsAndValidate(name, 41, true, *ALL_MIGRATIONS).use { assertPreserved(before, it) }
    }

    private fun seed(db: SupportSQLiteDatabase): Map<String, Map<String, String?>> {
        tables.forEach { table ->
            val values = ContentValues()
            db.query("PRAGMA table_info(`$table`)").use { columns ->
                while (columns.moveToNext()) {
                    val name = columns.getString(1)
                    val type = columns.getString(2)
                    val nullable = columns.getInt(3) == 0 && columns.getInt(5) == 0
                    when {
                        name == "originDeckId" || name == "bundleId" -> values.put(name, 1L)
                        name.endsWith("Path") || name == "imageUrl" -> values.put(name, "preserved/$table/$name.jpg")
                        nullable -> values.putNull(name)
                        type == "INTEGER" -> values.put(name, 1L)
                        type == "REAL" -> values.put(name, 1.0)
                        else -> values.put(name, "preserved-$table-$name")
                    }
                }
            }
            db.insert(table, SQLiteDatabase.CONFLICT_ABORT, values)
        }
        return tables.associateWith { table -> db.query("SELECT * FROM `$table`").use { row ->
            check(row.moveToFirst())
            row.columnNames.associateWith { column -> row.getColumnIndexOrThrow(column).let { if (row.isNull(it)) null else row.getString(it) } }
        } }
    }

    private fun assertPreserved(before: Map<String, Map<String, String?>>, db: SupportSQLiteDatabase) {
        before.forEach { (table, values) -> db.query("SELECT * FROM `$table`").use { row ->
            assertEquals("Row count in $table", 1, row.count)
            assertTrue(row.moveToFirst())
            values.forEach { (column, value) ->
                val index = row.getColumnIndexOrThrow(column)
                assertEquals("$table.$column", value, if (row.isNull(index)) null else row.getString(index))
            }
        } }
        db.query("PRAGMA foreign_key_check").use { assertEquals("Broken resource relations", 0, it.count) }
    }
}
