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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.Collection
import com.github.zly2006.zhihu.ui.CollectionBrowseScreen
import com.github.zly2006.zhihu.ui.CollectionScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/525
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/607
     */
    @Test
    fun createActionOpensExistingCollectionDialog() {
        setCollectionScreen(seedCollections(count = 1))

        composeRule.onNodeWithTag(COLLECTION_SCREEN_CREATE_BUTTON_TAG).performClick()

        composeRule.onNodeWithTag(CREATE_COLLECTION_DIALOG_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CREATE_COLLECTION_TITLE_INPUT_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("æ°å»ºæ¶èå¤¹").assertIsDisplayed()
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/525
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/607
     */
    @Test
    fun onlySelectedNonDefaultCollectionOffersDeleteConfirmation() {
        val defaultCollection = Collection(
            id = "default-collection",
            title = "é»è®¤æ¶èå¤¹",
            isDefault = true,
        )
        val selectedCollection = Collection(
            id = "selected-collection",
            title = "å¾å é¤æ¶èå¤¹",
        )
        setCollectionScreen(listOf(defaultCollection, selectedCollection))

        composeRule
            .onNodeWithTag(collectionDeleteButtonTag(defaultCollection.id))
            .assertDoesNotExist()
        composeRule
            .onNodeWithTag(collectionDeleteButtonTag(selectedCollection.id))
            .performClick()

        composeRule
            .onNodeWithTag(collectionDeleteDialogTag(selectedCollection.id))
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(collectionDeleteConfirmTag(selectedCollection.id))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("å é¤åæ æ³æ¢å¤ï¼ç¡®è®¤å é¤æ¶èå¤¹â${selectedCollection.title}âåï¼")
            .assertIsDisplayed()
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/609
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/611
     */
    @Test
    fun directBrowseSupportsPullRefreshAndDeleteConfirmation() {
        val defaultCollection = Collection(
            id = "direct-default",
            title = "ç´è¾¾é»è®¤æ¶èå¤¹",
            isDefault = true,
        )
        val deletableCollection = Collection(
            id = "direct-deletable",
            title = "ç´è¾¾å¾å é¤æ¶èå¤¹",
        )
        composeRule.setScreenContent {
            CollectionBrowseScreen(
                urlToken = "offline-test-user",
                testCollections = listOf(defaultCollection, deletableCollection),
            )
        }

        composeRule.onNodeWithTag(COLLECTION_BROWSE_PULL_TO_REFRESH_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_BROWSE_MODE_BUTTON_TAG).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("å½åä¸ºé¡ºåºæ¨¡å¼ï¼ç¹å»åæ¢ä¸ºéæºæ¨¡å¼").assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_BROWSE_RANDOM_REFRESH_BUTTON_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(COLLECTION_BROWSE_MODE_BUTTON_TAG).performClick()
        composeRule.onNodeWithContentDescription("å½åä¸ºéæºæ¨¡å¼ï¼ç¹å»åæ¢ä¸ºé¡ºåºæ¨¡å¼").assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_BROWSE_RANDOM_REFRESH_BUTTON_TAG).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(COLLECTION_BROWSE_MODE_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(COLLECTION_BROWSE_RANDOM_REFRESH_BUTTON_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(COLLECTION_BROWSE_FOLDER_SWITCH_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(collectionBrowseDeleteButtonTag(defaultCollection.id)).assertDoesNotExist()
        composeRule.onNodeWithTag(collectionBrowseDeleteButtonTag(deletableCollection.id)).performClick()

        composeRule.onNodeWithTag(collectionBrowseDeleteDialogTag(deletableCollection.id)).assertIsDisplayed()
        composeRule.onNodeWithTag(collectionBrowseDeleteConfirmTag(deletableCollection.id)).assertIsDisplayed()
        composeRule
            .onNodeWithText("å é¤åæ æ³æ¢å¤ï¼ç¡®è®¤å é¤æ¶èå¤¹â${deletableCollection.title}âåï¼")
            .assertIsDisplayed()
    }

    private fun setCollectionScreen(testCollections: List<Collection>) = composeRule.setScreenContent {
        CollectionScreen(
            urlToken = "offline-test-user",
            testCollections = testCollections,
        )
    }

    private fun seedCollections(count: Int): List<Collection> = List(count) { index ->
        val collectionIndex = index + 1
        Collection(
            id = "collection-$collectionIndex",
            title = "åºå®æ¶èå¤¹ $collectionIndex",
            description = "ç¨äº CollectionScreen ä»ªå¨æµè¯çåºå®æ¶èå¤¹ $collectionIndex",
            itemCount = collectionIndex * 3,
            likeCount = collectionIndex * 5,
            commentCount = collectionIndex,
        )
    }

    private companion object {
        const val COLLECTION_SCREEN_TITLE_TAG = "collection_screen_title"
        const val COLLECTION_SCREEN_BACK_BUTTON_TAG = "collection_screen_back_button"
        const val COLLECTION_SCREEN_LIST_TAG = "collection_screen_list"
        const val COLLECTION_SCREEN_CREATE_BUTTON_TAG = "collection_screen_create_button"
        const val CREATE_COLLECTION_DIALOG_TAG = "create_collection_dialog"
        const val CREATE_COLLECTION_TITLE_INPUT_TAG = "create_collection_title_input"
        const val COLLECTION_BROWSE_PULL_TO_REFRESH_TAG = "collection_browse_pull_to_refresh"
        const val COLLECTION_BROWSE_FOLDER_SWITCH_BUTTON_TAG = "collection_browse_folder_switch_button"
        const val COLLECTION_BROWSE_MODE_BUTTON_TAG = "collection_browse_mode_button"
        const val COLLECTION_BROWSE_RANDOM_REFRESH_BUTTON_TAG = "collection_browse_random_refresh_button"

        fun collectionItemTag(collectionId: String) = "collection_screen_item_$collectionId"

        fun collectionDeleteButtonTag(collectionId: String) = "collection_screen_delete_button_$collectionId"

        fun collectionDeleteDialogTag(collectionId: String) = "collection_screen_delete_dialog_$collectionId"

        fun collectionDeleteConfirmTag(collectionId: String) = "collection_screen_delete_confirm_$collectionId"

        fun collectionBrowseDeleteButtonTag(collectionId: String) = "collection_browse_delete_button_$collectionId"

        fun collectionBrowseDeleteDialogTag(collectionId: String) = "collection_browse_delete_dialog_$collectionId"

        fun collectionBrowseDeleteConfirmTag(collectionId: String) = "collection_browse_delete_confirm_$collectionId"
    }
}
