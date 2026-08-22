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
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CommentHolder
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.shared.data.MobileNotificationContent
import com.github.zly2006.zhihu.shared.data.MobileNotificationTimelineItem
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.NotificationScreen
import com.github.zly2006.zhihu.viewmodel.MobileNotificationCategory
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/490
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/503
     */
    @Test
    fun notificationScreen_showsCategoryUnreadCountBadge() {
        /*
         * Expected behavior:
         * 1. The test preloads per-category unread counts into the screen ViewModel.
         * 2. The top category row should render that count as a visible badge on the matching category.
         * 3. The badge should be part of the category button, not a separate toolbar count.
         */
        composeRule.setScreenContent {
            NotificationScreen()
        }
        composeRule.seedNotificationViewModel(
            unreadCounts = mapOf(MobileNotificationCategory.Like to 2),
        )

        composeRule.onNodeWithText("2").assertIsDisplayed()
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/569
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/606
     */
    @Test
    fun notificationScreen_fourObservedCommentActionsNavigateWithCommentAnchors() {
        val notifications = listOf(
            notificationFixture(
                id = "comment-content",
                title = "å«äººè¯è®ºæçåå®¹",
                subTitle = "è¯è®ºäºä½ çåç­",
                targetLink = "zhihu://comment/list/answer/2?anchor_comment_id=3&is_child=false",
            ),
            notificationFixture(
                id = "reply-comment",
                title = "å«äººåå¤æçè¯è®º",
                subTitle = "åå¤äºæ³æ³ä¸ä½ çè¯è®º",
                targetLink = "zhihu://comment/list/pin/4?anchor_comment_id=5&is_child=true",
            ),
            notificationFixture(
                id = "like-root-comment",
                title = "å«äººç¹èµæçæ ¹è¯è®º",
                subTitle = "åæ¬¢äºä½ çè¯è®º",
                targetLink = "zhihu://comment/list/article/6?anchor_comment_id=7&is_child=false",
            ),
            notificationFixture(
                id = "like-child-comment",
                title = "å«äººç¹èµæçæ¥¼ä¸­æ¥¼è¯è®º",
                subTitle = "åæ¬¢äºä½ çè¯è®º",
                targetLink = "zhihu://comment/list/pin/8?anchor_comment_id=9&is_child=false",
            ),
        )
        val scrollGuardNotifications = List(8) { index ->
            notificationFixture(
                id = "scroll-guard-$index",
                title = "å ä½éç¥ $index",
                subTitle = "æµè¯å ä½",
            )
        }
        val recordingNavigator = setNotificationScreenContent(notifications + scrollGuardNotifications)
        val notificationList = composeRule.onNode(hasScrollAction())

        notifications.forEach { notification ->
            val title = notification.content!!.title
            notificationList.performScrollToNode(hasText(title))
            composeRule.onNodeWithText(title).assertIsDisplayed().performClick()
        }

        assertEquals(4, recordingNavigator.destinations.size)
        val commentedAnswerHolder = recordingNavigator.destinations[0] as CommentHolder
        assertEquals("3", commentedAnswerHolder.commentId)
        val commentedAnswer = commentedAnswerHolder.article as Article
        assertEquals(ArticleType.Answer, commentedAnswer.type)
        assertEquals(2L, commentedAnswer.id)
        val repliedPinHolder = recordingNavigator.destinations[1] as CommentHolder
        assertEquals("5", repliedPinHolder.commentId)
        val repliedPin = repliedPinHolder.article as Pin
        assertEquals(4L, repliedPin.id)
        val likedArticleCommentHolder = recordingNavigator.destinations[2] as CommentHolder
        assertEquals("7", likedArticleCommentHolder.commentId)
        val likedArticleComment = likedArticleCommentHolder.article as Article
        assertEquals(ArticleType.Article, likedArticleComment.type)
        assertEquals(6L, likedArticleComment.id)
        val likedChildPinCommentHolder = recordingNavigator.destinations[3] as CommentHolder
        assertEquals("9", likedChildPinCommentHolder.commentId)
        val likedChildPinComment = likedChildPinCommentHolder.article as Pin
        assertEquals(8L, likedChildPinComment.id)
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/569
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/606
     */
    @Test
    fun realAccountCommentLinkSnapshot_all212OccurrencesResolveToTheirAnchors() {
        // 2026-07-30 ä»å½åè´¦å· comment/like éç¥ååä¸¤é¡µåï¼ä¿çç»æä¸æ°éï¼ID å¨é¨æ¿æ¢ä¸ºæµè¯å¼ã
        val fixtures = buildList {
            val groups = listOf(
                ObservedCommentLinkGroup("pin", occurrences = 163, uniqueLinks = 163),
                ObservedCommentLinkGroup("answer", occurrences = 22, uniqueLinks = 22),
                ObservedCommentLinkGroup("article", occurrences = 4, uniqueLinks = 4),
                ObservedCommentLinkGroup("pin", occurrences = 1, uniqueLinks = 1, isChild = true),
                ObservedCommentLinkGroup("question", occurrences = 1, uniqueLinks = 1),
                // like ååºæ 20 æ¬¡è·³è½¬ï¼èåå° 7 æ¡å¯ä¸é¾æ¥ï¼3 æ¡æ ¹è¯è®ºã4 æ¡æ¥¼ä¸­æ¥¼è¯è®ºã
                ObservedCommentLinkGroup("answer", occurrences = 5, uniqueLinks = 3),
                ObservedCommentLinkGroup("article", occurrences = 1, uniqueLinks = 1),
                ObservedCommentLinkGroup("pin", occurrences = 14, uniqueLinks = 3),
            )
            groups.forEachIndexed { groupIndex, group ->
                repeat(group.occurrences) { occurrence ->
                    val uniqueIndex = occurrence % group.uniqueLinks
                    val contentId = 100_000L + groupIndex * 10_000L + uniqueIndex
                    val anchorId = (1_000_000L + groupIndex * 10_000L + uniqueIndex).toString()
                    add(
                        ObservedCommentLink(
                            url = "zhihu://comment/list/${group.contentType}/$contentId?anchor_comment_id=$anchorId&is_child=${group.isChild}",
                            contentType = group.contentType,
                            contentId = contentId,
                            anchorId = anchorId,
                        ),
                    )
                }
            }
            add(
                ObservedCommentLink(
                    url = "zhihu://comment/list/answer/900000?anchor_comment_id=1900000&list_height_ratio=0.66&dragIconVisible=true&segment=%7B%22id%22%3A1%7D",
                    contentType = "answer",
                    contentId = 900_000L,
                    anchorId = "1900000",
                ),
            )
        }

        assertEquals(212, fixtures.size)
        assertEquals(199, fixtures.map { it.url }.distinct().size)
        fixtures.forEach { fixture ->
            val holder = resolveContent(fixture.url) as? CommentHolder
                ?: throw AssertionError("æ æ³è§£æçå®è¯è®ºè·³è½¬ç»æï¼${fixture.url}")
            assertEquals(fixture.anchorId, holder.commentId)
            when (val destination = holder.article) {
                is Article -> {
                    assertEquals(fixture.contentType, destination.type.toString())
                    assertEquals(fixture.contentId, destination.id)
                }

                is Pin -> {
                    assertEquals("pin", fixture.contentType)
                    assertEquals(fixture.contentId, destination.id)
                }

                is Question -> {
                    assertEquals("question", fixture.contentType)
                    assertEquals(fixture.contentId, destination.questionId)
                }

                else -> throw AssertionError("æ æ³è§£æçå®è¯è®ºè·³è½¬ç»æï¼${fixture.url}")
            }
        }
    }

    private fun setNotificationScreenContent(
        notifications: List<MobileNotificationTimelineItem> = listOf(notificationFixture()),
    ): RecordingNavigator {
        val recordingNavigator = composeRule.setScreenContent {
            NotificationScreen()
        }
        composeRule.seedNotificationViewModel(notifications = notifications)
        return recordingNavigator
    }

    private fun notificationFixture(
        id: String = "local-notification",
        title: String = "æµè¯ç¨æ· åå¤äºåç­ä¸ä½ çè¯è®º",
        subTitle: String = "è¯è®ºååå¤",
        targetLink: String = "zhihu://comment/list/answer/2?anchor_comment_id=3&is_child=false",
    ) = MobileNotificationTimelineItem(
        id = id,
        type = "aggregate_notification",
        isRead = true,
        created = 1_713_420_000L,
        content = MobileNotificationContent(
            title = title,
            subTitle = subTitle,
            targetLink = targetLink,
        ),
    )

    private fun MainActivityComposeRule.seedNotificationViewModel(
        unreadCounts: Map<MobileNotificationCategory, Int> = emptyMap(),
        notifications: List<MobileNotificationTimelineItem> = listOf(notificationFixture()),
    ) {
        waitUntil(
            "Notification screen did not finish its initial refresh",
            timeoutMillis = 5_000,
        ) {
            ViewModelProvider(activity)[NotificationViewModel::class.java].let { viewModel ->
                !viewModel.isLoading && viewModel.isEnd
            }
        }
        activity.runOnUiThread {
            val viewModel = ViewModelProvider(activity)[NotificationViewModel::class.java]
            viewModel.allData.clear()
            viewModel.allData += notifications
            if (unreadCounts.isNotEmpty()) {
                viewModel.categoryUnreadCounts.putAll(unreadCounts)
            }
        }
        waitForIdle()
    }

    private data class ObservedCommentLink(
        val url: String,
        val contentType: String,
        val contentId: Long,
        val anchorId: String,
    )

    private data class ObservedCommentLinkGroup(
        val contentType: String,
        val occurrences: Int,
        val uniqueLinks: Int,
        val isChild: Boolean = false,
    )
}
