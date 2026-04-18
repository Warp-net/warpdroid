package com.keylesspalace.tusky.ui.statuscomponents

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keylesspalace.tusky.entity.Emoji
import com.keylesspalace.tusky.ui.statuscomponents.text.emojify
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class VisibleLengthTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should count custom emojis as 2 characters`() {
        composeTestRule.setContent {
            assertEquals(4, AnnotatedString("test").visibleLength())

            assertEquals(
                10,
                ":test: test :test:".emojify(
                    listOf(
                        Emoji(
                            shortcode = "test",
                            url = "https://some.url",
                            staticUrl = "https://some.url",
                            category = null
                        )
                    )
                ).visibleLength()
            )
        }
    }
}
