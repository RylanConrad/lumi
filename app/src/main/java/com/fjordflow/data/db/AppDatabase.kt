package com.fjordflow.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fjordflow.data.db.dao.FlashCardDao
import com.fjordflow.data.db.dao.RoadmapDao
import com.fjordflow.data.db.dao.WordDao
import com.fjordflow.data.db.entity.FlashCardEntity
import com.fjordflow.data.db.entity.RoadmapNodeEntity
import com.fjordflow.data.db.entity.WordEntity
import com.fjordflow.data.seed.frenchRoadmapNodes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [WordEntity::class, FlashCardEntity::class, RoadmapNodeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao
    abstract fun flashCardDao(): FlashCardDao
    abstract fun roadmapDao(): RoadmapDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fjordflow.db"
                )
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
