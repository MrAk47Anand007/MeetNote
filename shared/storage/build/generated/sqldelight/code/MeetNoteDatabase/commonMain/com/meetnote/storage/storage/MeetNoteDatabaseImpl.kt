package com.meetnote.storage.storage

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.meetnote.storage.MeetNoteDatabase
import com.meetnote.storage.MeetNoteDatabaseQueries
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<MeetNoteDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = MeetNoteDatabaseImpl.Schema

internal fun KClass<MeetNoteDatabase>.newInstance(driver: SqlDriver): MeetNoteDatabase =
    MeetNoteDatabaseImpl(driver)

private class MeetNoteDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver), MeetNoteDatabase {
  override val meetNoteDatabaseQueries: MeetNoteDatabaseQueries = MeetNoteDatabaseQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE meeting_session (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    title TEXT NOT NULL,
          |    status TEXT NOT NULL,
          |    processing_mode TEXT NOT NULL,
          |    processing_policy TEXT NOT NULL,
          |    processing_tier TEXT NOT NULL,
          |    source TEXT NOT NULL,
          |    audio_file_path TEXT
          |)
          """.trimMargin(), 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
