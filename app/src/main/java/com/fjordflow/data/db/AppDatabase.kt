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
import com.fjordflow.data.db.dao.PageDao
import com.fjordflow.data.db.dao.RoadmapDao
import com.fjordflow.data.db.dao.WordDao
import com.fjordflow.data.db.entity.BookEntity
import com.fjordflow.data.db.entity.FlashCardEntity
import com.fjordflow.data.db.entity.PageEntity
import com.fjordflow.data.db.entity.RoadmapNodeEntity
import com.fjordflow.data.db.entity.WordEntity
import com.fjordflow.data.seed.frenchRoadmapNodes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [WordEntity::class, FlashCardEntity::class, RoadmapNodeEntity::class, BookEntity::class, PageEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao
    abstract fun flashCardDao(): FlashCardDao
    abstract fun roadmapDao(): RoadmapDao
    abstract fun bookDao(): BookDao
    abstract fun pageDao(): PageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookId` INTEGER NOT NULL,
                        `pageNumber` INTEGER NOT NULL,
                        `content` TEXT NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pages_bookId` ON `pages` (`bookId`)")
                // Migrate existing book content into pages
                db.execSQL("""
                    INSERT INTO pages (bookId, pageNumber, content, addedAt)
                    SELECT id, 1, content, addedAt FROM books WHERE content != ''
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
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
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
