package com.meetnote.android

import android.app.Application
import com.meetnote.android.core.appModules
import com.meetnote.android.ui.session.androidUiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MeetNoteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MeetNoteApplication)
            modules(appModules + androidUiModule)
        }
    }
}
