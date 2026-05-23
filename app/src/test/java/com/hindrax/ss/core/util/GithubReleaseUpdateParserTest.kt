package com.hindrax.ss.core.util

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GithubReleaseUpdateParserTest {
    @Test
    fun createsFallbackDownloadUrlWhenNewReleaseHasNoAssetsYet() {
        val release = JSONObject(
            """
            {
              "tag_name": "v1.36",
              "draft": false,
              "prerelease": false,
              "name": "Release v1.36",
              "html_url": "https://github.com/stredes/Hindrax_ss/releases/tag/v1.36",
              "published_at": "2026-05-23T22:25:32Z",
              "assets": []
            }
            """.trimIndent()
        )

        val info = GithubReleaseUpdateParser.updateInfoFromRelease(
            release = release,
            currentVersion = "1.33",
            githubRepo = "stredes/Hindrax_ss"
        )

        assertNotNull(info)
        assertEquals("1.36", info!!.version)
        assertEquals("hindrax-v1.36.apk", info.assetName)
        assertEquals(
            "https://github.com/stredes/Hindrax_ss/releases/download/v1.36/hindrax-v1.36.apk",
            info.url
        )
    }
}
