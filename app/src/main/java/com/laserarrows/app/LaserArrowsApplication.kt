package com.laserarrows.app

import android.app.Application
import com.laserarrows.app.audio.SfxEngine
import com.laserarrows.app.data.ProgressStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LaserArrowsApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var progressStore: ProgressStore
        private set
    lateinit var sfxEngine: SfxEngine
        private set

    override fun onCreate() {
        super.onCreate()
        progressStore = ProgressStore(this)
        sfxEngine = SfxEngine(this, applicationScope)
    }
}
