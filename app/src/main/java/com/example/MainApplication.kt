package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AppRepository

class MainApplication : Application() {
    
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "devil_gpt_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    val repository: AppRepository by lazy {
        AppRepository(database.dao())
    }
}
