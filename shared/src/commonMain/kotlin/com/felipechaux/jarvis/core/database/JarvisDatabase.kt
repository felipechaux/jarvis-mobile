package com.felipechaux.jarvis.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.felipechaux.jarvis.feature.chat.data.local.dao.ChatDao
import com.felipechaux.jarvis.feature.chat.data.local.entity.ConversationEntity
import com.felipechaux.jarvis.feature.chat.data.local.entity.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [ConversationEntity::class, MessageEntity::class], version = 1)
@ConstructedBy(JarvisDatabaseConstructor::class)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}

// Room generates the actual implementations.
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object JarvisDatabaseConstructor : RoomDatabaseConstructor<JarvisDatabase> {
    override fun initialize(): JarvisDatabase
}

internal const val DATABASE_FILE = "jarvis.db"

/** Finish a platform builder (see the platform modules) with the shared settings. */
fun RoomDatabase.Builder<JarvisDatabase>.buildJarvisDatabase(): JarvisDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
