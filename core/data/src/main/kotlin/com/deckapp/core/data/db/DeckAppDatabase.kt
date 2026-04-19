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
        RecentFileRecord::class,
        TableBundleEntity::class,
        CardFtsEntity::class,
        CardFaceFtsEntity::class,
        RandomTableFtsEntity::class,
        TableEntryFtsEntity::class,
        CollectionEntity::class,
        CollectionResourceCrossRef::class,
        CollectionFtsEntity::class,
        NpcEntity::class,
        NpcTagCrossRef::class,
        WikiCategoryEntity::class,
        WikiEntryEntity::class,
        ReferenceTableEntity::class,
        ReferenceRowEntity::class,
        ReferenceTableTagCrossRef::class,
        SystemRuleEntity::class,
        SystemRuleTagCrossRef::class,
        ReferenceTableFtsEntity::class,
        SystemRuleFtsEntity::class,
        HexMapEntity::class,
        HexTileEntity::class,
        HexPoiEntity::class,
        HexDayEntity::class
    ],
    version = 34,
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
    abstract fun tableBundleDao(): TableBundleDao
    abstract fun tableRollResultDao(): TableRollResultDao
    abstract fun encounterDao(): EncounterDao
    abstract fun combatLogDao(): CombatLogDao
    abstract fun recentFileDao(): RecentFileDao
    abstract fun searchDao(): SearchDao
    abstract fun collectionDao(): CollectionDao
    abstract fun npcDao(): NpcDao
    abstract fun wikiDao(): WikiDao
    abstract fun referenceTableDao(): ReferenceTableDao
    abstract fun hexDao(): HexDao
    abstract fun systemRuleDao(): SystemRuleDao
    abstract fun backupDao(): com.deckapp.core.data.db.backup.BackupDao

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
        val MIGRATION_19_20 = com.deckapp.core.data.db.MIGRATION_19_20
        val MIGRATION_20_21 = com.deckapp.core.data.db.MIGRATION_20_21
        val MIGRATION_21_22 = com.deckapp.core.data.db.MIGRATION_21_22
        val MIGRATION_22_23 = com.deckapp.core.data.db.MIGRATION_22_23
        val MIGRATION_23_24 = com.deckapp.core.data.db.MIGRATION_23_24
        val MIGRATION_24_25 = com.deckapp.core.data.db.MIGRATION_24_25
        val MIGRATION_25_26 = com.deckapp.core.data.db.MIGRATION_25_26
        val MIGRATION_26_27 = com.deckapp.core.data.db.MIGRATION_26_27
        val MIGRATION_27_28 = com.deckapp.core.data.db.MIGRATION_27_28
        val MIGRATION_28_29 = com.deckapp.core.data.db.MIGRATION_28_29
        val MIGRATION_29_30 = com.deckapp.core.data.db.MIGRATION_29_30
        val MIGRATION_30_31 = com.deckapp.core.data.db.MIGRATION_30_31
        val MIGRATION_31_32 = com.deckapp.core.data.db.MIGRATION_31_32
        val MIGRATION_32_33 = com.deckapp.core.data.db.MIGRATION_32_33
        val MIGRATION_33_34 = com.deckapp.core.data.db.MIGRATION_33_34
    }
}
