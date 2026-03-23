package com.yueliangmanle.danci.core.releasenotes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesCatalogTest {
    @Test
    fun parseJson_highlightsInstalledVersion() {
        val catalog = ReleaseNotesCatalog(
            assetLoader = {
                """
                    {
                      "releases": [
                        {
                          "version": "1.8",
                          "date": "2026-03-23",
                          "sections": [
                            {
                              "title": "Added",
                              "items": ["新增 MiMo TTS"]
                            }
                          ]
                        },
                        {
                          "version": "1.7",
                          "date": "2026-03-22",
                          "sections": [
                            {
                              "title": "Fixed",
                              "items": ["修复动态复习问题"]
                            }
                          ]
                        }
                      ]
                    }
                """.trimIndent()
            },
        )

        val notes = catalog.load(currentVersion = "1.7")

        assertEquals(2, notes.size)
        assertEquals("1.7", notes.first { it.isCurrent }.version)
        assertTrue(notes.first().sections.first().items.isNotEmpty())
    }
}
