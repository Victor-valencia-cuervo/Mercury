package com.music.bitchord.ui.screens

import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.ShelfLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HomeFeedSectionsTest {
    private fun item(title: String, id: String) = ShelfItem(
        title = title,
        subtitle = "Artist",
        thumbnailUrl = null,
        videoId = id,
        browseId = null,
    )

    @Test
    fun `organizes known shelves and keeps unclassified suggestions`() {
        val feed = listOf(
            HomeShelf("Listen again", listOf(item("Recent", "recent")), layout = ShelfLayout.LIST),
            HomeShelf("New releases", listOf(item("Release", "release"))),
            HomeShelf("Similar to your taste", listOf(item("Similar", "similar"))),
            HomeShelf(
                "Artists for you",
                listOf(item("Artist", "artist").copy(videoId = null, browseId = "UCartist")),
            ),
            HomeShelf("Recommended music videos", listOf(item("Video", "video")), layout = ShelfLayout.VIDEO),
            HomeShelf("Discover something new", listOf(item("Discovery", "discovery"))),
        )

        val sections = organizeHomeFeed(
            shelves = feed,
            continueListeningTitle = "Continue listening",
            popularReleasesTitle = "Popular releases",
            similarTitle = "Similar to your taste",
            popularArtistsTitle = "Popular artists",
            suggestionsTitle = "Suggestions for today",
            musicVideosTitle = "Recommended videos",
        )

        assertEquals(listOf("Recent"), sections.continueListening?.items?.map { it.title })
        assertEquals(listOf("Release"), sections.popularReleases?.items?.map { it.title })
        assertEquals(listOf("Similar"), sections.similarToYourTaste?.items?.map { it.title })
        assertEquals(listOf("Artist"), sections.popularArtists?.items?.map { it.title })
        assertEquals(listOf("Video"), sections.musicVideos?.items?.map { it.title })
        assertEquals(listOf("Discovery"), sections.suggestions?.items?.map { it.title })
    }

    @Test
    fun `merges duplicate continuation shelves while retaining distinct items`() {
        val sections = organizeHomeFeed(
            shelves = listOf(
                HomeShelf("Recents", listOf(item("One", "one")), layout = ShelfLayout.LIST),
                HomeShelf("Listen again", listOf(item("One again", "one"), item("Two", "two"))),
            ),
            continueListeningTitle = "Continue listening",
            popularReleasesTitle = "Popular releases",
            similarTitle = "Similar",
            popularArtistsTitle = "Popular artists",
            suggestionsTitle = "Suggestions",
            musicVideosTitle = "Videos",
        )

        val continueListening = sections.continueListening
        assertNotNull(continueListening)
        assertEquals(ShelfLayout.LIST, continueListening?.layout)
        assertEquals(listOf("one", "two"), continueListening?.items?.map { it.videoId })
    }
}
