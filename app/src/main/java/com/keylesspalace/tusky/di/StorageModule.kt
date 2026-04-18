/*
 * Warpdroid - a Warpnet Android client.
 * Copyright (C) 2026 Warpdroid contributors.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Provides [SharedPreferences] for UI toggles and an *in-memory* Room
 * [AppDatabase] used as a scratch cache by the timeline, notifications,
 * drafts and conversations code paths. The database is never persisted to
 * disk — `Room.inMemoryDatabaseBuilder` allocates everything in RAM and all
 * data is discarded when the process dies.
 */
package com.keylesspalace.tusky.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.keylesspalace.tusky.db.AppDatabase
import com.keylesspalace.tusky.db.Converters
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    fun providesSharedPreferences(@ApplicationContext appContext: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(appContext)

    @Provides
    @Singleton
    fun providesAppDatabase(
        @ApplicationContext appContext: Context,
        converters: Converters,
    ): AppDatabase = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java)
        .addTypeConverter(converters)
        .allowMainThreadQueries()
        .build()
}
