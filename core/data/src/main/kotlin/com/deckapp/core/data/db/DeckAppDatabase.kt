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
        TableRollResultEntity::class
    ],
    version = 5,
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

    companion object {
        const val DATABASE_NAME = "deckapp.db"
    }
}
