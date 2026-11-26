package com.music.bitchord.data.innertube

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class MusicChartsTest {
    @Test
    fun `selects the Top 100 playlist and parses available chart regions`() {
        val response = Json.parseToJsonElement(
            """
            {
              "contents": {
                "singleColumnBrowseResultsRenderer": {
                  "tabs": [{
                    "tabRenderer": {
                      "content": {
                        "sectionListRenderer": {
                          "contents": [{
                            "musicCarouselShelfRenderer": {
                              "header": {
                                "musicCarouselShelfBasicHeaderRenderer": {
                                  "title": {"runs": [{"text": "Video charts"}]}
                                }
                              },
                              "contents": [
                                {"musicTwoRowItemRenderer": {
                                  "title": {"runs": [{"text": "Daily Top Music Videos - Spain"}]},
                                  "navigationEndpoint": {"browseEndpoint": {"browseId": "VLPLDAILY"}}
                                }},
                                {"musicTwoRowItemRenderer": {
                                  "title": {"runs": [{"text": "Top 100 Music Videos Spain"}]},
                                  "navigationEndpoint": {"browseEndpoint": {"browseId": "VLPLTOP100"}}
                                }}
                              ]
                            }
                          }]
                        }
                      }
                    }
                  }]
                }
              },
              "frameworkUpdates": {
                "entityBatchUpdate": {
                  "mutations": [
                    {"payload": {"musicFormBooleanChoice": {"opaqueToken": "ES"}}},
                    {"payload": {"musicFormBooleanChoice": {"opaqueToken": "ZZ"}}},
                    {"payload": {"musicFormBooleanChoice": {"opaqueToken": "ES"}}}
                  ]
                }
              }
            }
            """.trimIndent(),
        ).jsonObject

        assertEquals("VLPLTOP100", InnertubeParser.parseTopSongsChartBrowseId(response))
        assertEquals(listOf("ES", "ZZ"), InnertubeParser.parseChartRegionCodes(response))
    }

    @Test
    fun `falls back to the available chart playlist when Top 100 is absent`() {
        val response = Json.parseToJsonElement(
            """
            {
              "contents": {
                "singleColumnBrowseResultsRenderer": {
                  "tabs": [{
                    "tabRenderer": {
                      "content": {
                        "sectionListRenderer": {
                          "contents": [{
                            "musicCarouselShelfRenderer": {
                              "header": {"musicCarouselShelfBasicHeaderRenderer": {}},
                              "contents": [
                                {"musicTwoRowItemRenderer": {
                                  "title": {"runs": [{"text": "Daily Music Chart"}]},
                                  "navigationEndpoint": {"browseEndpoint": {"browseId": "VLPLDAILY"}}
                                }}
                              ]
                            }
                          }]
                        }
                      }
                    }
                  }]
                }
              }
            }
            """.trimIndent(),
        ).jsonObject

        assertEquals("VLPLDAILY", InnertubeParser.parseTopSongsChartBrowseId(response))
    }
}
