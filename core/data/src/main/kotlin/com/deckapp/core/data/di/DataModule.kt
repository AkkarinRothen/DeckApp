package com.deckapp.core.data.di

import android.content.Context
import androidx.room.Room
import com.deckapp.core.data.db.*
import com.deckapp.core.data.db.MIGRATION_6_7
import com.deckapp.core.data.db.MIGRATION_7_8
import com.deckapp.core.data.db.MIGRATION_8_9
import com.deckapp.core.data.db.MIGRATION_9_10
import com.deckapp.core.data.db.MIGRATION_11_12
import com.deckapp.core.data.db.MIGRATION_12_13
import com.deckapp.core.data.db.MIGRATION_13_14
import com.deckapp.core.data.db.MIGRATION_14_15
import com.deckapp.core.data.db.MIGRATION_15_16
import com.deckapp.core.data.db.MIGRATION_16_17
import com.deckapp.core.data.db.MIGRATION_17_18
import com.deckapp.core.data.repository.CardRepositoryImpl
import com.deckapp.core.data.repository.EncounterRepositoryImpl
import com.deckapp.core.data.repository.FileRepositoryImpl
import com.deckapp.core.data.repository.OcrRepositoryImpl
import com.deckapp.core.data.repository.SessionRepositoryImpl
import com.deckapp.core.data.repository.TableRepositoryImpl
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.repository.OcrRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.repository.TableRepository
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
                MIGRATION_17_18
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
    @Provides fun provideTableRollResultDao(db: DeckAppDatabase) = db.tableRollResultDao()
    @Provides fun provideEncounterDao(db: DeckAppDatabase) = db.encounterDao()
    @Provides fun provideCombatLogDao(db: DeckAppDatabase) = db.combatLogDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

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
}

/**
 * Módulo separado para [OcrRepository] porque su implementación es
 * [ActivityRetainedScoped] — no puede vivir en [SingletonComponent].
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class OcrModule {

    @Binds
    @dagger.hilt.android.scopes.ActivityRetainedScoped
    abstract fun bindOcrRepository(impl: OcrRepositoryImpl): OcrRepository
}
