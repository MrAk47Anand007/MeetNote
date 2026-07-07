package com.meetnote.storage

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.meetnote.storage.storage.newInstance
import com.meetnote.storage.storage.schema
import kotlin.Unit

public interface MeetNoteDatabase : Transacter {
  public val meetNoteDatabaseQueries: MeetNoteDatabaseQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = MeetNoteDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): MeetNoteDatabase =
        MeetNoteDatabase::class.newInstance(driver)
  }
}
