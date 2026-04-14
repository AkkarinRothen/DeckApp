package com.deckapp.core.data.di

import android.content.Context
import androidx.room.Room
import com.deckapp.core.data.db.*
import com.deckapp.core.data.repository.CardRepositoryImpl
import com.deckapp.core.data.repository.FileRepositoryImpl
import com.deckapp.core.data.repository.SessionRepositoryImpl
import com.deckapp.core.data.repository.TableRepositoryImpl
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.repository.TableRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
            .fallbackToDestructiveMigration()   // dev only — reemplazar con migraciones en prod
            .build()

    @Provides fun provideCardStackDao(db: DeckAppDatabase) = db.cardStackDao()
    @Provides fun provideCardDao(db: DeckAppDatabase) = db.cardDao()
    @Provides fun provideCardFaceDao(db: DeckAppDatabase) = db.cardFaceDao()
    @Provides fun provideTagDao(db: DeckAppDatabase) = db.tagDao()
    @Provides fun provideSessionDao(db: DeckAppDatabase) = db.sessionDao()
    @Provides fun provideDrawEventDao(db: DeckAppDatabase) = db.drawEventDao()
    @Provides fun provideRandomTableDao(db: DeckAppDatabase) = db.randomTableDao()
    @Provides fun provideTableRollResultDao(db: DeckAppDatabase) = db.tableRollResultDao()
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
}
