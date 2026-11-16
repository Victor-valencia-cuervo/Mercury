package com.music.bitchord.data

import android.content.Context
import android.content.SharedPreferences
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * The likes that belong to this device rather than to a YouTube Music account:
 * a heart tapped while nobody is signed in has to land somewhere, and this is
 * it — one JSON document, read once at start and rewritten off the main thread
 * after every change.
 *
 * It is the base of a later sync, not just a cache: every song here carries a
 * `pushed` flag, cleared when it is liked and set once the rating has been
 * delivered to an account (YouTube Music on sign-in, the cloud store on
 * account creation). A like made offline is therefore uploaded exactly once,
 * and a song unliked before any account existed is simply gone rather than
 * resurrected by the next sign-in.
 *
 * Songs are held by [Song.videoId]; the whole row is kept so the Liked page
 * and the Library's card can be drawn without a lookup.
 */
object LocalLikes {

    /** The "Música que te gusta" page this store is, on the detail stack. */
    const val BROWSE_ID = "local:likes"

    private const val PREF_NAME = "bitchord_local_likes"
    private const val KEY_SONGS = "songs_json"

    private lateinit var prefs: SharedPreferences
    private val lock = Any()
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())

    /** The liked tracks, newest like first. */
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _ids = MutableStateFlow<Set<String>>(emptySet())

    /** The liked [Song.videoId]s, for a heart to be drawn without a list read. */
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    fun init(context: Context) {
        if (this::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        publish(load())
        val loaded = _songs.value
        io.launch {
            songs.filter { it !== loaded }.conflate().collect { persist(it) }
        }
    }

    fun isLiked(videoId: String): Boolean = videoId in _ids.value

    /** Likes or unlikes a streamed track; returns whether it is liked now. */
    fun toggleLike(song: Song): Boolean {
        val key = song.videoId
        var nowLiked = false
        mutate { list ->
            nowLiked = key !in _ids.value
            if (nowLiked) {
                listOf(song.asLocalLike(pushed = false)) + list.filterNot { it.videoId == key }
            } else {
                list.filterNot { it.videoId == key }
            }
        }
        return nowLiked
    }

    /** Adds a like that came back from the cloud — confirmed already, so no pending push. */
    fun restore(song: Song) {
        if (song.videoId.isBlank() || isLiked(song.videoId)) return
        mutate { list -> listOf(song.asLocalLike(pushed = true)) + list }
    }

    fun remove(videoId: String) {
        if (videoId !in _ids.value) return
        mutate { list -> list.filterNot { it.videoId == videoId } }
    }

    /** The likes whose rating no account has confirmed yet — the sign-in push works off this. */
    fun pendingSongs(): List<Song> = _songs.value.filter { !it.pushed }

    /** Marks a song's like as delivered, so the next push doesn't send it again. */
    fun markPushed(videoId: String) {
        mutate { list -> list.map { if (it.videoId == videoId) it.copy(pushed = true) else it } }
    }

    private inline fun mutate(change: (List<LocalLike>) -> List<LocalLike>) {
        synchronized(lock) { publish(change(_songs.value)) }
    }

    private fun publish(list: List<LocalLike>) {
        _songs.value = list
        _ids.value = list.mapTo(HashSet()) { it.videoId }
    }

    private fun load(): List<LocalLike> {
        val raw = prefs.getString(KEY_SONGS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i -> likeFrom(array.getJSONObject(i)) }
        }.getOrElse {
            DebugLog.w("BitChord", "Local likes unreadable: ${it.message}")
            emptyList()
        }
    }

    private fun persist(list: List<LocalLike>) {
        runCatching {
            val array = JSONArray()
            list.forEach { array.put(it.toJson()) }
            prefs.edit().putString(KEY_SONGS, array.toString()).apply()
        }.onFailure { DebugLog.w("BitChord", "Local likes not saved: ${it.message}") }
    }

    /** A liked track with the sync bookkeeping that travels with it. */
    private data class LocalLike(
        val song: Song,
        /** False until a signed-in push or a cloud sync has confirmed the rating. */
        val pushed: Boolean,
    )

    private fun LocalLike.toJson(): JSONObject = song.toJson().put("pushed", pushed)

    private fun likeFrom(obj: JSONObject): LocalLike = LocalLike(
        song = obj.toSong(),
        pushed = obj.optBoolean("pushed", false),
    )

    private fun Song.asLocalLike(pushed: Boolean): LocalLike = LocalLike(
        song = Song(
            videoId = videoId,
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            durationText = durationText,
            artistId = artistId,
            albumId = albumId,
            albumName = albumName,
            isVideo = isVideo,
        ),
        pushed = pushed,
    )

    private fun Song.toJson(): JSONObject = JSONObject().apply {
        put("videoId", videoId)
        put("title", title)
        put("artist", artist)
        putOpt("thumbnailUrl", thumbnailUrl)
        putOpt("durationText", durationText)
        putOpt("artistId", artistId)
        putOpt("albumId", albumId)
        putOpt("albumName", albumName)
        put("isVideo", isVideo)
    }

    private fun JSONObject.toSong(): Song = Song(
        videoId = optString("videoId"),
        title = optString("title"),
        artist = optString("artist"),
        thumbnailUrl = optString("thumbnailUrl").takeIf { it.isNotEmpty() && it != "null" },
        durationText = optString("durationText").takeIf { it.isNotEmpty() && it != "null" },
        artistId = optString("artistId").takeIf { it.isNotEmpty() && it != "null" },
        albumId = optString("albumId").takeIf { it.isNotEmpty() && it != "null" },
        albumName = optString("albumName").takeIf { it.isNotEmpty() && it != "null" },
        isVideo = optBoolean("isVideo", false),
    )
}
