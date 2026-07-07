package com.meetnote.storage

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.String

public class MeetNoteDatabaseQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAll(mapper: (
    id: String,
    title: String,
    status: String,
    processing_mode: String,
    processing_policy: String,
    processing_tier: String,
    source: String,
    audio_file_path: String?,
  ) -> T): Query<T> = Query(264_814_812, arrayOf("meeting_session"), driver, "MeetNoteDatabase.sq",
      "selectAll",
      "SELECT meeting_session.id, meeting_session.title, meeting_session.status, meeting_session.processing_mode, meeting_session.processing_policy, meeting_session.processing_tier, meeting_session.source, meeting_session.audio_file_path FROM meeting_session") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)
    )
  }

  public fun selectAll(): Query<Meeting_session> = selectAll { id, title, status, processing_mode,
      processing_policy, processing_tier, source, audio_file_path ->
    Meeting_session(
      id,
      title,
      status,
      processing_mode,
      processing_policy,
      processing_tier,
      source,
      audio_file_path
    )
  }

  public fun insertSession(
    id: String,
    title: String,
    status: String,
    processing_mode: String,
    processing_policy: String,
    processing_tier: String,
    source: String,
    audio_file_path: String?,
  ) {
    driver.execute(-322_983_948, """
        |INSERT INTO meeting_session(
        |    id,
        |    title,
        |    status,
        |    processing_mode,
        |    processing_policy,
        |    processing_tier,
        |    source,
        |    audio_file_path
        |)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 8) {
          bindString(0, id)
          bindString(1, title)
          bindString(2, status)
          bindString(3, processing_mode)
          bindString(4, processing_policy)
          bindString(5, processing_tier)
          bindString(6, source)
          bindString(7, audio_file_path)
        }
    notifyQueries(-322_983_948) { emit ->
      emit("meeting_session")
    }
  }

  public fun updateStatus(status: String, id: String) {
    driver.execute(1_079_521_508, """
        |UPDATE meeting_session
        |SET status = ?
        |WHERE id = ?
        """.trimMargin(), 2) {
          bindString(0, status)
          bindString(1, id)
        }
    notifyQueries(1_079_521_508) { emit ->
      emit("meeting_session")
    }
  }

  public fun attachAudioFile(audio_file_path: String?, id: String) {
    driver.execute(-1_045_921_212, """
        |UPDATE meeting_session
        |SET audio_file_path = ?
        |WHERE id = ?
        """.trimMargin(), 2) {
          bindString(0, audio_file_path)
          bindString(1, id)
        }
    notifyQueries(-1_045_921_212) { emit ->
      emit("meeting_session")
    }
  }
}
