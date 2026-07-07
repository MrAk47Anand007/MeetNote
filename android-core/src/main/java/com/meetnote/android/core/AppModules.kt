package com.meetnote.android.core

import android.content.Context
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.meetnote.shared.domain.repository.SessionRepository
import com.meetnote.shared.domain.usecase.CreateManualSessionUseCase
import com.meetnote.shared.storage.QueryToFlow
import com.meetnote.shared.storage.SessionStorage
import com.meetnote.shared.storage.SqlDelightSessionRepository
import com.meetnote.storage.MeetNoteDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

private val coreModule = module {
    single<SqlDriver> { provideSqlDriver(get()) }
    single { MeetNoteDatabase(get()) }
    single { SessionStorage(get()) }
    single<QueryToFlow> { AndroidQueryToFlow }
    single<SessionRepository> { SqlDelightSessionRepository(get(), get()) }
    single { CreateManualSessionUseCase(get()) }
}

val appModules: List<Module> = listOf(coreModule)

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
