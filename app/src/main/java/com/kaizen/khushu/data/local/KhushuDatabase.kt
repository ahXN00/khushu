package com.kaizen.khushu.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kaizen.khushu.data.local.entities.AyahEntity
import com.kaizen.khushu.data.local.entities.HadithEntity
import com.kaizen.khushu.data.local.entities.SurahEntity
import com.kaizen.khushu.data.local.entities.TranslationEntity

@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        HadithEntity::class,
        TranslationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KhushuDatabase : RoomDatabase() {
    abstract fun ayahDao(): AyahDao
    abstract fun hadithDao(): HadithDao

    companion object {
        @Volatile private var INSTANCE: KhushuDatabase? = null

        fun getInstance(context: Context): KhushuDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, KhushuDatabase::class.java, "khushu.db")
                    .createFromAsset("khushu.db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
