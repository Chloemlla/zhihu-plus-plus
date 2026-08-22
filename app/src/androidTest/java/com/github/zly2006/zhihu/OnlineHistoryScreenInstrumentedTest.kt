/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.OnlineHistoryScreen
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.OnlineHistoryViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnlineHistoryScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule()

    private val onlineHistoryViewModel: OnlineHistoryViewModel
        get() = ViewModelProvider(composeRule.activity)[OnlineHistoryViewModel::class.java]

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        clearDisplayItems()
    }

    @After
    fun tearDown() {
        clearDisplayItems()
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/562
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/604
     */
    @Test
    fun historyItemOverflowOffersSingleDeleteAction() {
        setOnlineHistoryScreen()

        // The toolbar is the first "æ´å¤éé¡¹" node; the next one belongs to the first visible
        // history card. Single-record deletion lives in that existing card menu rather than the
        // global toolbar menu.
        composeRule.onAllNodesWithContentDescription("æ´å¤éé¡¹")[1].performClick()
        composeRule.onNodeWithText("å é¤è¯¥æ¡åå²è®°å½").assertIsDisplayed()
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/562
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/604
     */
    @Test
    fun singleDeletePostsDecodedPairAndRemovesRowAfterSuccess() {
        val client = HttpClient(
            MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("https://api.zhihu.com/read_history/batch_del", request.url.toString())
                assertEquals(
                    Json.parseToJsonElement(
                        """{"pairs":[{"content_token":"123","content_type":"answer"}],"clear":false}""",
                    ),
                    Json.parseToJsonElement((request.body as TextContent).text),
                )
                respond("", HttpStatusCode.OK)
            },
        )
        val response = ZhihuJson.json
            .parseToJsonElement(
                """
                {
                  "data": [
                    {
                      "card_type": "single_card",
                      "data": {
                        "header": {
                          "icon": "https://example.com/icon.png",
                          "title": "å¾å é¤çå¨çº¿åå²"
                        },
                        "action": {
                          "type": "router",
                          "url": "zhihu://answers/123"
                        },
                        "extra": {
                          "content_token": "123",
                          "content_type": "answer",
                          "read_time": 1716120000,
                          "question_token": "456"
                        }
                      }
                    }
                  ],
                  "paging": {
                    "is_end": true,
                    "is_start": true,
                    "totals": 1
                  }
                }
                """.trimIndent(),
            ).jsonObject
        val environment = object : PaginationEnvironment {
            override fun httpClient() = client

            override fun authenticatedCookies() = emptyMap<String, String>()

            override suspend fun fetchJson(url: String, include: String): JsonObject = response

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
        }
        val viewModel = OnlineHistoryViewModel()

        composeRule.activity.runOnUiThread {
            viewModel.refresh(environment)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.displayItems.singleOrNull()?.title == "å¾å é¤çå¨çº¿åå²"
        }

        runBlocking {
            viewModel.deleteItem(environment, viewModel.displayItems.single())
        }

        assertEquals(0, viewModel.displayItems.size)
    }

    private fun setOnlineHistoryScreen(itemCount: Int = 24): RecordingNavigator {
        seedDisplayItems(itemCount)
        return composeRule.setScreenContent {
            OnlineHistoryScreen()
        }
    }

    private fun seedDisplayItems(itemCount: Int) {
        composeRule.activity.runOnUiThread {
            onlineHistoryViewModel.displayItems.clear()
            onlineHistoryViewModel.displayItems.addAll(
                List(itemCount) { index ->
                    FeedDisplayItem(
                        title = seedTitle(index + 1),
                        summary = "ç¨äº OnlineHistoryScreen ä»ªå¨æµè¯çåºå®æè¦ ${index + 1}",
                        details = "åºå®è¯¦æ ${index + 1}",
                        feed = null,
                        authorName = "ä½è ${index + 1}",
                    )
                },
            )
        }
        composeRule.waitForIdle()
    }

    private fun clearDisplayItems() {
        composeRule.activity.runOnUiThread {
            onlineHistoryViewModel.displayItems.clear()
        }
        composeRule.waitForIdle()
    }

    private companion object {
        const val LIST_TAG = "online_history_list"

        fun seedTitle(index: Int) = "åºå®å¨çº¿åå²æ¡ç® $index"
    }
}
