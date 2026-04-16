package com.deckapp.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CardStackEntity::class,
        CardEntity::class,
        CardFaceEntity::class,
        TagEntity::class,
        CardStackTagCrossRef::class,
        CardTagCrossRef::class,
        SessionEntity::class,
        SessionDeckRefEntity::class,
        DrawEventEntity::class,
        RandomTableEntity::class,
        TableEntryEntity::class,
        TableRollResultEntity::class,
        SessionTableRefEntity::class,
        RandomTableTagCrossRef::class,
        EncounterEntity::class,
        EncounterCreatureEntity::class,
        CombatLogEntryEntity::class,
        RecentFileRecord::class
    ],
    version = 19,
    exportSchema = true
)
abstract class DeckAppDatabase : RoomDatabase() {
    abstract fun cardStackDao(): CardStackDao
    abstract fun cardDao(): CardDao
    abstract fun cardFaceDao(): CardFaceDao
    abstract fun tagDao(): TagDao
    abstract fun sessionDao(): SessionDao
    abstract fun drawEventDao(): DrawEventDao
    abstract fun randomTableDao(): RandomTableDao
    abstract fun tableRollResultDao(): TableRollResultDao
    abstract fun encounterDao(): EncounterDao
    abstract fun combatLogDao(): CombatLogDao
    abstract fun recentFileDao(): RecentFileDao

    companion object {
        const val DATABASE_NAME = "deckapp.db"

        val MIGRATION_10_11 = com.deckapp.core.data.db.MIGRATION_10_11
        val MIGRATION_11_12 = com.deckapp.core.data.db.MIGRATION_11_12
        val MIGRATION_12_13 = com.deckapp.core.data.db.MIGRATION_12_13
        val MIGRATION_13_14 = com.deckapp.core.data.db.MIGRATION_13_14
        val MIGRATION_14_15 = com.deckapp.core.data.db.MIGRATION_14_15
        val MIGRATION_15_16 = com.deckapp.core.data.db.MIGRATION_15_16
        val MIGRATION_16_17 = com.deckapp.core.data.db.MIGRATION_16_17
        val MIGRATION_17_18 = com.deckapp.core.data.db.MIGRATION_17_18
        val MIGRATION_18_19 = com.deckapp.core.data.db.MIGRATION_18_19
    }
}
