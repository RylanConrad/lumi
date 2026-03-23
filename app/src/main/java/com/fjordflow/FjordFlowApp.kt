package com.fjordflow

import android.app.Application
import com.fjordflow.data.db.AppDatabase

class FjordFlowApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
