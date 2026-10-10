package com.pocketsparrow

import android.app.Application
import com.pocketsparrow.core.NativeBridge
import com.pocketsparrow.data.AppDatabase

class SparrowApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Initialize SQLCipher native binaries
        try {
            System.loadLibrary("sqlcipher")
        } catch (_: Throwable) {}

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
