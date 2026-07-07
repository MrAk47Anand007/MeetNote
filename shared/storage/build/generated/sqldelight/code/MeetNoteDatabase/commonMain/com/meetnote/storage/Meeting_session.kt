package com.meetnote.storage

import kotlin.String

public data class Meeting_session(
  public val id: String,
  public val title: String,
  public val status: String,
  public val processing_mode: String,
  public val processing_policy: String,
  public val processing_tier: String,
  public val source: String,
  public val audio_file_path: String?,
)
