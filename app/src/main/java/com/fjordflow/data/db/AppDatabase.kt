package com.fjordflow.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fjordflow.data.db.dao.BookDao
import com.fjordflow.data.db.dao.FlashCardDao
import com.fjordflow.data.db.dao.RoadmapDao
import com.fjordflow.data.db.dao.WordDao
import com.fjordflow.data.db.entity.BookEntity
import com.fjordflow.data.db.entity.FlashCardEntity
import com.fjordflow.data.db.entity.RoadmapNodeEntity
import com.fjordflow.data.db.entity.WordEntity
import com.fjordflow.data.seed.frenchRoadmapNodes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [WordEntity::class, FlashCardEntity::class, RoadmapNodeEntity::class, BookEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao
    abstract fun flashCardDao(): FlashCardDao
    abstract fun roadmapDao(): RoadmapDao
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `books` (
                        `id` INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `author` TEXT NOT NULL, 
                        `content` TEXT NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `progress` REAL NOT NULL, 
                        `addedAt` INTEGER NOT NULL, 
                        `lastReadAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fjordflow.db"
                )
                .fallbackToDestructiveMigration() // Simpler for development
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed the roadmap on first launch
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.roadmapDao()?.insertNodes(frenchRoadmapNodes)
                        }
                    }
                })
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
