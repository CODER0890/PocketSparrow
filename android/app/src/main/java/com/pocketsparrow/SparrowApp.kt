package com.pocketsparrow

import android.app.Application
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.data.AppDatabase
import net.sqlcipher.database.SQLiteDatabase

class SparrowApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Initialize SQLCipher native binaries
        SQLiteDatabase.loadLibs(this)

        // 2. Initialize Native Two-Tier Detection Engine
        NativeBridge.init()

        // 3. Eagerly warm-up encrypted database
        AppDatabase.getDatabase(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        NativeBridge.shutdown()
    }
}
