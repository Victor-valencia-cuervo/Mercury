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

// ---- The Mercury account's own sync ----------------------------------------

/** What is written to `public.favorites` for one liked song. `user_id` rides along as the conflict target. */
@Serializable
data class FavoriteWrite(
    @SerialName("user_id") val userId: String,
    @SerialName("video_id") val videoId: String,
    val title: String,
    val artist: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String?,
)

/** Row of `public.favorites`, only the columns the app restores a like from. */
@Serializable
data class FavoriteRow(
    @SerialName("video_id") val videoId: String,
    val title: String,
    val artist: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
)

/** One of the Mercury account's playlists, mapped to the device's by [externalId]. */
@Serializable
data class PlaylistRow(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("external_id") val externalId: String,
    val name: String,
)

/** What is written to `public.playlist_tracks` for one track, in playlist order. */
@Serializable
data class PlaylistTrackWrite(
    @SerialName("playlist_id") val playlistId: String,
    val position: Int,
    @SerialName("video_id") val videoId: String,
    val title: String,
    val artist: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String?,
)

/** Row of `public.playlist_tracks`. */
@Serializable
data class PlaylistTrackRow(
    @SerialName("playlist_id") val playlistId: String,
    val position: Int,
    @SerialName("video_id") val videoId: String,
    val title: String,
    val artist: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
)

/** Row of `public.settings`: the whole synced-preferences snapshot as one JSON object. */
@Serializable
data class SettingsRow(
    val data: kotlinx.serialization.json.JsonObject? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** What is written to `public.settings`: the same snapshot, sent whole. */
@Serializable
data class SettingsWrite(
    @SerialName("user_id") val userId: String,
    val data: kotlinx.serialization.json.JsonObject,
)
