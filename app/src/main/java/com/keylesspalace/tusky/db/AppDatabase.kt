/*
 * Warpdroid - a Warpnet Android client.
 * Copyright (C) 2026 Warpdroid contributors.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Room-based scratch cache. Warpdroid never ships this database to disk;
 * [com.keylesspalace.tusky.di.StorageModule] builds it via
 * `Room.inMemoryDatabaseBuilder`, so the entire schema lives in RAM and is
 * wiped on process exit — matching Warpdroid's "no persistence" stance.
 * AccountEntity is intentionally omitted: account state is held by the
 * in-memory [AccountManager], not by Room.
 */
package com.keylesspalace.tusky.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.keylesspalace.tusky.components.conversation.ConversationEntity
import com.keylesspalace.tusky.db.dao.DraftDao
import com.keylesspalace.tusky.db.dao.InstanceDao
import com.keylesspalace.tusky.db.dao.NotificationPolicyDao
import com.keylesspalace.tusky.db.dao.NotificationsDao
import com.keylesspalace.tusky.db.dao.TimelineAccountDao
import com.keylesspalace.tusky.db.dao.TimelineDao
import com.keylesspalace.tusky.db.dao.TimelineStatusDao
import com.keylesspalace.tusky.db.entity.DraftEntity
import com.keylesspalace.tusky.db.entity.HomeTimelineEntity
import com.keylesspalace.tusky.db.entity.InstanceEntity
import com.keylesspalace.tusky.db.entity.NotificationEntity
import com.keylesspalace.tusky.db.entity.NotificationPolicyEntity
import com.keylesspalace.tusky.db.entity.NotificationReportEntity
import com.keylesspalace.tusky.db.entity.TimelineAccountEntity
import com.keylesspalace.tusky.db.entity.TimelineStatusEntity

@Database(
    entities = [
        DraftEntity::class,
        InstanceEntity::class,
        TimelineStatusEntity::class,
        TimelineAccountEntity::class,
        ConversationEntity::class,
        NotificationEntity::class,
        NotificationReportEntity::class,
        HomeTimelineEntity::class,
        NotificationPolicyEntity::class,
    ],
    version = 1,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun instanceDao(): InstanceDao
    abstract fun conversationDao(): ConversationsDao
    abstract fun timelineDao(): TimelineDao
    abstract fun draftDao(): DraftDao
    abstract fun notificationsDao(): NotificationsDao
    abstract fun timelineStatusDao(): TimelineStatusDao
    abstract fun timelineAccountDao(): TimelineAccountDao
    abstract fun notificationPolicyDao(): NotificationPolicyDao
}
