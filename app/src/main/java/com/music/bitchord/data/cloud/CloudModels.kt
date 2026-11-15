package com.music.bitchord.data.cloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row of `public.profiles`, only the columns the app reads. */
@Serializable
data class ProfileRow(
    val id: String,
    @SerialName("display_name") val displayName: String?,
)

/** What is written to `profiles` when the name changes. Only these columns, so nothing else is overwritten. */
@Serializable
data class ProfileNameWrite(
    val id: String,
    @SerialName("display_name") val displayName: String,
)

/** A counted listen, appended to `public.listening_history`. */
@Serializable
data class HistoryWrite(
    @SerialName("user_id") val userId: String,
    @SerialName("video_id") val videoId: String,
    val title: String,
    val artist: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String?,
)
