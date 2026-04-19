package com.deckapp.core.data.di

import android.content.Context
import androidx.room.Room
import com.deckapp.core.data.db.*
import com.deckapp.core.data.db.MIGRATION_6_7
import com.deckapp.core.data.db.MIGRATION_7_8
import com.deckapp.core.data.db.MIGRATION_8_9
import com.deckapp.core.data.db.MIGRATION_9_10
import com.deckapp.core.data.db.MIGRATION_10_11
import com.deckapp.core.data.db.MIGRATION_11_12
import com.deckapp.core.data.db.MIGRATION_12_13
import com.deckapp.core.data.db.MIGRATION_13_14
import com.deckapp.core.data.db.MIGRATION_14_15
import com.deckapp.core.data.db.MIGRATION_15_16
import com.deckapp.core.data.db.MIGRATION_16_17
import com.deckapp.core.data.db.MIGRATION_17_18
import com.deckapp.core.data.db.MIGRATION_18_19
import com.deckapp.core.data.db.MIGRATION_19_20
import com.deckapp.core.data.db.MIGRATION_20_21
import com.deckapp.core.data.db.MIGRATION_21_22
import com.deckapp.core.data.db.MIGRATION_22_23
import com.deckapp.core.data.db.MIGRATION_23_24
import com.deckapp.core.data.db.MIGRATION_24_25
import com.deckapp.core.data.db.MIGRATION_25_26
import com.deckapp.core.data.db.MIGRATION_26_27
import com.deckapp.core.data.db.MIGRATION_27_28
import com.deckapp.core.data.db.MIGRATION_28_29
import com.deckapp.core.data.db.MIGRATION_29_30
import com.deckapp.core.data.db.MIGRATION_30_31
import com.deckapp.core.data.repository.*
import com.deckapp.core.data.repository.WikiRepositoryImpl
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.CollectionRepository
import com.deckapp.core.domain.repository.NpcRepository
import com.deckapp.core.domain.repository.RecentFileRepository
import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.repository.OcrRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.repository.AiTableRepository
import com.deckapp.core.domain.repository.AiReferenceRepository
import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.domain.repository.SettingsRepository
import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.domain.repository.WikiRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDeckAppDatabase(@ApplicationContext context: Context): DeckAppDatabase =
        Room.databaseBuilder(context, DeckAppDatabase::class.java, DeckAppDatabase.DATABASE_NAME)
            .addMigrations(
                MIGRATION_6_7, 
                MIGRATION_7_8, 
                MIGRATION_8_9, 
                MIGRATION_9_10, 
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
                MIGRATION_19_20,
                MIGRATION_20_21,
                MIGRATION_21_22,
                MIGRATION_22_23,
                MIGRATION_23_24,
                MIGRATION_24_25,
                MIGRATION_25_26,
                MIGRATION_26_27,
                MIGRATION_27_28,
                MIGRATION_28_29,
                MIGRATION_29_30,
                MIGRATION_30_31
            )
            .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5)
            .build()

    @Provides fun provideCardStackDao(db: DeckAppDatabase) = db.cardStackDao()
    @Provides fun provideCardDao(db: DeckAppDatabase) = db.cardDao()
    @Provides fun provideCardFaceDao(db: DeckAppDatabase) = db.cardFaceDao()
    @Provides fun provideTagDao(db: DeckAppDatabase) = db.tagDao()
    @Provides fun provideSessionDao(db: DeckAppDatabase) = db.sessionDao()
    @Provides fun provideDrawEventDao(db: DeckAppDatabase) = db.drawEventDao()
    @Provides fun provideRandomTableDao(db: DeckAppDatabase) = db.randomTableDao()
    @Provides fun provideTableBundleDao(db: DeckAppDatabase) = db.tableBundleDao()
    @Provides fun provideTableRollResultDao(db: DeckAppDatabase) = db.tableRollResultDao()
    @Provides fun provideEncounterDao(db: DeckAppDatabase) = db.encounterDao()
    @Provides fun provideCombatLogDao(db: DeckAppDatabase) = db.combatLogDao()
    @Provides fun provideRecentFileDao(db: DeckAppDatabase) = db.recentFileDao()
    @Provides fun provideSearchDao(db: DeckAppDatabase) = db.searchDao()
    @Provides fun provideCollectionDao(db: DeckAppDatabase) = db.collectionDao()
    @Provides fun provideNpcDao(db: DeckAppDatabase) = db.npcDao()
    @Provides fun provideWikiDao(db: DeckAppDatabase) = db.wikiDao()
    @Provides fun provideReferenceTableDao(db: DeckAppDatabase) = db.referenceTableDao()
    @Provides fun provideSystemRuleDao(db: DeckAppDatabase) = db.systemRuleDao()
    @Provides fun provideBackupDao(db: DeckAppDatabase) = db.backupDao()
    @Provides fun provideHexDao(db: DeckAppDatabase) = db.hexDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: com.deckapp.core.data.repository.backup.BackupRepositoryImpl): com.deckapp.core.domain.repository.backup.BackupRepository

    @Binds
    @Singleton
    abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds
    @Singleton
    abstract fun bindTableRepository(impl: TableRepositoryImpl): TableRepository

    @Binds
    @Singleton
    abstract fun bindEncounterRepository(impl: EncounterRepositoryImpl): EncounterRepository

    @Binds
    @Singleton
    abstract fun bindRecentFileRepository(impl: RecentFileRepositoryImpl): RecentFileRepository

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(impl: CollectionRepositoryImpl): CollectionRepository

    @Binds
    @Singleton
    abstract fun bindAiTableRepository(impl: GeminiAiRepository): AiTableRepository

    @Binds
    @Singleton
    abstract fun bindAiReferenceRepository(impl: GeminiAiRepository): AiReferenceRepository

    @Binds
    @Singleton
    abstract fun bindReferenceRepository(impl: ReferenceRepositoryImpl): ReferenceRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindNpcRepository(impl: NpcRepositoryImpl): NpcRepository

    @Binds
    @Singleton
    abstract fun bindWikiRepository(impl: WikiRepositoryImpl): WikiRepository

    @Binds
    @Singleton
    abstract fun bindHexRepository(impl: HexRepositoryImpl): HexRepository
}
