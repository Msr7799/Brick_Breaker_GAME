/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/UiConfirmation.kt
 * الوظيفة: تنسيق رسائل تأكيد إجراءات الواجهة.
 * الحسابات: تحديد انتقالات التأكيد والإلغاء.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.example.brick_breaker_ball.assets.GameAssets

enum class UiConfirmationPurpose { PURCHASE, RESTART, QUIT, NEW_GAME, RESET }

data class UiConfirmation(
    val purpose: UiConfirmationPurpose,
    val description: String,
    val itemName: String? = null,
    val costLabel: String? = null,
    val returnContext: String? = null,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit = {}
)

/** A pending destructive action is consumed before invoking its callback. */
class ConfirmationCoordinator {
    var pending: UiConfirmation? = null
        private set
    var isBusy: Boolean = false
        private set

    fun request(value: UiConfirmation): Boolean {
        if (pending != null || isBusy) return false
        pending = value
        return true
    }

    fun cancel(): Boolean {
        val value = pending ?: return false
        pending = null
        value.onCancel()
        return true
    }

    fun confirm(): Boolean {
        val value = pending ?: return false
        pending = null
        isBusy = true
        try {
            value.onConfirm()
        } finally {
            isBusy = false
        }
        return true
    }
}

data class ConfirmationHitboxes(val yes: Rectangle, val no: Rectangle)

/** The YES/NO artwork is already part of the board; its atlas crops define hit areas only. */
object BoardConfirmationRenderer {
    fun draw(
        batch: SpriteBatch,
        assets: GameAssets,
        font: BitmapFont,
        screen: Rectangle,
        confirmation: UiConfirmation
    ): ConfirmationHitboxes {
        batch.color = Color(0f, 0f, 0f, .72f)
        batch.draw(assets.solidUiRegion, screen.x, screen.y, screen.width, screen.height)
        batch.color = Color.WHITE

        val maxWidth = minOf(screen.width * .88f, 760f)
        val maxHeight = minOf(screen.height * .54f, 480f)
        val board = assets.boards.contain(
            "confirmation_dialog",
            Rectangle(
                screen.x + (screen.width - maxWidth) * .5f,
                screen.y + (screen.height - maxHeight) * .5f + 50f,
                maxWidth,
                maxHeight
            )
        )
        batch.draw(assets.boards.requireRegion("confirmation_dialog"), board.x, board.y, board.width, board.height)

        val text = listOfNotNull(confirmation.itemName, confirmation.costLabel, confirmation.description)
            .filter(String::isNotBlank).joinToString("  •  ")
        val captionWidth = minOf(screen.width * .86f, 900f)
        val caption = Rectangle(
            screen.x + (screen.width - captionWidth) * .5f,
            board.y - 112f,
            captionWidth,
            100f
        )
        batch.color = Color(.07f, .035f, .025f, .96f)
        batch.draw(assets.solidUiRegion, caption.x, caption.y, caption.width, caption.height)
        batch.color = Color.WHITE
        font.draw(
            batch,
            text,
            caption.x + 18f,
            caption.y + caption.height - 19f,
            caption.width - 36f,
            Align.center,
            true
        )

        return ConfirmationHitboxes(
            yes = assets.boards.placement("confirmation_dialog", "confirmation_yes_button", board),
            no = assets.boards.placement("confirmation_dialog", "confirmation_no_button", board)
        )
    }
}
