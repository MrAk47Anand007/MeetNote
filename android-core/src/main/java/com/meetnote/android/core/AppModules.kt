package com.meetnote.android.core

import android.content.Context
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.meetnote.android.asr.androidAsrModule
import com.meetnote.android.capture.MeetingRecorder
import com.meetnote.android.capture.MicOnlyMeetingRecorder
import com.meetnote.android.capture.InMemoryPlaybackCaptureAuthorizationStore
import com.meetnote.android.capture.PlaybackAudioRecorder
import com.meetnote.android.capture.PlaybackCaptureAuthorizationStore
import com.meetnote.shared.domain.repository.SessionRepository
import com.meetnote.shared.domain.usecase.CreateManualSessionUseCase
import com.meetnote.shared.storage.QueryToFlow
import com.meetnote.shared.storage.SessionStorage
import com.meetnote.shared.storage.SqlDelightSessionRepository
import com.meetnote.storage.MeetNoteDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val MICROPHONE_RECORDER_QUALIFIER = "microphoneRecorder"
const val PLAYBACK_RECORDER_QUALIFIER = "playbackRecorder"

private val coreModule = module {
    single<SqlDriver> { provideSqlDriver(get()) }
    single { MeetNoteDatabase(get()) }
    single { SessionStorage(get()) }
    single<QueryToFlow> { AndroidQueryToFlow }
    single<SessionRepository> { SqlDelightSessionRepository(get(), get()) }
    single { CreateManualSessionUseCase(get()) }
    single<PlaybackCaptureAuthorizationStore> { InMemoryPlaybackCaptureAuthorizationStore() }
    single<MeetingRecorder>(named(MICROPHONE_RECORDER_QUALIFIER)) {
        MicOnlyMeetingRecorder(get(), get())
    }
    single<MeetingRecorder>(named(PLAYBACK_RECORDER_QUALIFIER)) {
        PlaybackAudioRecorder(get(), get(), get())
    }
}

val appModules: List<Module> = listOf(coreModule, androidAsrModule)

private fun provideSqlDriver(context: Context): SqlDriver =
    AndroidSqliteDriver(
        schema = MeetNoteDatabase.Schema,
        context = context,
        name = "meetnote.db"
    )

private object AndroidQueryToFlow : QueryToFlow {
    override fun <T : Any> asFlow(query: app.cash.sqldelight.Query<T>) =
        query.asFlow().mapToList(Dispatchers.IO)
}
