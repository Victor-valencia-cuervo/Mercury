package com.music.bitchord.ui.screens

import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.ShelfLayout
import java.util.Locale

internal data class HomeFeedSections(
    val continueListening: HomeShelf?,
    val popularReleases: HomeShelf?,
    val similarToYourTaste: HomeShelf?,
    val popularArtists: HomeShelf?,
    val suggestions: HomeShelf?,
    val musicVideos: HomeShelf?,
)

internal fun organizeHomeFeed(
    shelves: List<HomeShelf>,
    continueListeningTitle: String,
    popularReleasesTitle: String,
    similarTitle: String,
    popularArtistsTitle: String,
    suggestionsTitle: String,
    musicVideosTitle: String,
): HomeFeedSections {
    val used = BooleanArray(shelves.size)

    fun take(predicate: (HomeShelf) -> Boolean): List<HomeShelf> =
        shelves.mapIndexedNotNull { index, shelf ->
            if (used[index] || !predicate(shelf)) null
            else shelf.also { used[index] = true }
        }

    val continueListening = mergeHomeShelves(
        continueListeningTitle,
        take { it.isContinueListeningShelf() },
        ShelfLayout.LIST,
    )
    val similar = mergeHomeShelves(
        similarTitle,
        take { it.layout != ShelfLayout.VIDEO && it.isSimilarShelf() },
    )
    val artists = mergeHomeShelves(
        popularArtistsTitle,
        take { it.layout != ShelfLayout.VIDEO && it.isArtistShelf() },
    )
    val releases = mergeHomeShelves(
        popularReleasesTitle,
        take { it.layout != ShelfLayout.VIDEO && it.isPopularReleaseShelf() },
    )
    val videos = mergeHomeShelves(
        musicVideosTitle,
        take { it.layout == ShelfLayout.VIDEO },
        ShelfLayout.VIDEO,
    )
    val suggestions = mergeHomeShelves(
        suggestionsTitle,
        take { true },
    )
    return HomeFeedSections(continueListening, releases, similar, artists, suggestions, videos)
}

private fun mergeHomeShelves(
    title: String,
    sources: List<HomeShelf>,
    layout: ShelfLayout? = null,
): HomeShelf? {
    if (sources.isEmpty()) return null
    val items = sources.flatMap { it.items }.distinctBy(ShelfItem::homeIdentity)
    if (items.isEmpty()) return null
    val mergedLayout = layout ?: if (sources.all { it.layout == ShelfLayout.LIST }) {
        ShelfLayout.LIST
    } else {
        ShelfLayout.CARDS
    }
    return HomeShelf(
        title = title,
        items = items,
        subtitle = sources.firstOrNull { it.subtitle.isNotBlank() }?.subtitle.orEmpty(),
        layout = mergedLayout,
    )
}

private fun ShelfItem.homeIdentity(): String = when {
    videoId != null -> "video:$videoId"
    browseId != null -> "browse:$browseId"
    else -> "item:$title:$subtitle"
}

internal fun HomeShelf.isContinueListeningShelf(): Boolean = title.containsAny(
    "recent",
    "listen again",
    "continue listening",
    "reciente",
    "escuchar de nuevo",
    "seguir escuchando",
)

private fun HomeShelf.isSimilarShelf(): Boolean = title.containsAny(
    "similar",
    "related",
    "because you listen",
    "based on",
    "inspired by",
    "parecid",
    "relacionad",
    "basado",
)

private fun HomeShelf.isArtistShelf(): Boolean =
    title.containsAny("artist", "artista", "singer", "cantante", "nghệ sĩ", "アーティスト") ||
        items.any { it.browseId?.startsWith("UC") == true }

private fun HomeShelf.isPopularReleaseShelf(): Boolean =
    title.containsAny(
        "album",
        "single",
        "release",
        "chart",
        "popular",
        "trending",
        "hit",
        "novedad",
        "lanzamiento",
        "álbum",
        "sencillo",
        "éxito",
        "tendencia",
    ) || items.any { it.browseId?.startsWith("MPRE") == true }

private fun String.containsAny(vararg terms: String): Boolean {
    val normalized = lowercase(Locale.ROOT)
    return terms.any(normalized::contains)
}
