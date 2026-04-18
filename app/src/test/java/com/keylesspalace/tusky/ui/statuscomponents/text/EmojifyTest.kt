package com.keylesspalace.tusky.ui.statuscomponents.text

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keylesspalace.tusky.entity.Emoji
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class EmojifyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val emojis = listOf(
        Emoji(
            shortcode = "test",
            url = "https://some.url/test.png",
            staticUrl = "https://some.url/test.png",
            category = "test"
        ),
        Emoji(
            shortcode = "blobfox",
            url = "https://some.url/blob.png",
            staticUrl = "https://some.url/blob.png",
            category = "test"
        ),
    )

    @Test
    fun `empty string`() {
        composeTestRule.setContent {
            assertEquals(AnnotatedString(""), "".emojify(emojis))
        }
    }

    @Test
    fun `string without emojis`() {
        composeTestRule.setContent {
            assertEquals(AnnotatedString("This is a test"), "This is a test".emojify(emojis))
        }
    }

    @Test
    fun `string with one emoji`() {
        composeTestRule.setContent {
            assertEquals(
                buildAnnotatedString {
                    append("This is a ")
                    withAnnotation(
                        tag = "androidx.compose.foundation.text.inlineContent",
                        annotation = "test"
                    ) {
                        append(":test:")
                    }
                },
                "This is a :test:".emojify(emojis)
            )
        }
    }

    @Test
    fun `string with one emoji and duplicated emojis in input array`() {
        composeTestRule.setContent {
            assertEquals(
                buildAnnotatedString {
                    append("This is a ")
                    withAnnotation(
                        tag = "androidx.compose.foundation.text.inlineContent",
                        annotation = "test"
                    ) {
                        append(":test:")
                    }
                },
                "This is a :test:".emojify(emojis + emojis)
            )
        }
    }

    @Test
    fun `string with multiple emojis`() {
        composeTestRule.setContent {
            assertEquals(
                buildAnnotatedString {
                    append("This is a ")
                    withAnnotation(
                        tag = "androidx.compose.foundation.text.inlineContent",
                        annotation = "test"
                    ) {
                        append(":test:")
                    }
                    append("! ")
                    withAnnotation(
                        tag = "androidx.compose.foundation.text.inlineContent",
                        annotation = "test"
                    ) {
                        append(":test:")
                    }
                    append(" blobfox ")
                    withAnnotation(
                        tag = "androidx.compose.foundation.text.inlineContent",
                        annotation = "blobfox"
                    ) {
                        append(":blobfox:")
                    }
                    append(" :blobfoxlol:")
                },
                "This is a :test:! :test: blobfox :blobfox: :blobfoxlol:".emojify(emojis)
            )
        }
    }
}
