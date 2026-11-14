/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/gameplay/VolleyOpeningSalvo.kt
 * الوظيفة: تهيئة الضربة الافتتاحية لكل مرحلة.
 * الحسابات: توزيع الكرات ومسارات الإطلاق الأولى.
 */

package com.example.brick_breaker_ball.gameplay

import com.example.brick_breaker_ball.campaign.VolleyAuthoredBrickSeed

/**
 * Mystery Forest stage 1 exported from the web Volley editor.
 *
 * The editor uses an 11x9 authoring canvas, while the campaign keeps its canonical 9x8 logical
 * grid. These entries therefore carry exact game-world bounds and source cell coordinates; the
 * simulation consumes the bounds directly without changing the grid contract for other stages.
 */
object VolleyOpeningSalvo {
    private fun brick(
        row: Int,
        column: Int,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        type: BrickType,
        health: Int,
        shape: VolleyPieceShape,
        atlasSpriteName: String
    ) = VolleyAuthoredBrickSeed(
        sourceRow = row,
        sourceColumn = column,
        x = x,
        y = y,
        width = width,
        height = height,
        type = type,
        health = health,
        shape = shape,
        atlasSpriteName = atlasSpriteName.ifBlank { null }
    )

    val bricks = listOf(
        brick(0, 0, 38f, 1298f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_005.png"),
        brick(0, 1, 113.27272727f, 1298f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_040.png"),
        brick(0, 2, 188.54545455f, 1298f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_023.png"),
        brick(0, 3, 263.81818182f, 1298f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_005.png"),
        brick(0, 4, 339.09090909f, 1298f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_016.png"),
        brick(0, 6, 489.63636364f, 1298f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_016.png"),
        brick(0, 7, 564.90909091f, 1298f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_005.png"),
        brick(0, 8, 640.18181818f, 1298f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_038.png"),
        brick(0, 9, 715.45454545f, 1298f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_039.png"),
        brick(0, 10, 790.72727273f, 1298f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_005.png"),
        brick(1, 0, 38f, 1228f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_040.png"),
        brick(1, 1, 113.27272727f, 1228f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_023.png"),
        brick(1, 2, 188.54545455f, 1228f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_005.png"),
        brick(1, 3, 263.81818182f, 1228f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_016.png"),
        brick(1, 4, 339.09090909f, 1228f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_005.png"),
        brick(1, 6, 489.63636364f, 1228f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_005.png"),
        brick(1, 7, 564.90909091f, 1228f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_016.png"),
        brick(1, 8, 640.18181818f, 1228f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_005.png"),
        brick(1, 9, 715.45454545f, 1228f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_038.png"),
        brick(1, 10, 790.72727273f, 1228f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_039.png"),
        brick(2, 0, 38f, 1158f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 9, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_061.png"),
        brick(2, 1, 113.27272727f, 1158f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_016.png"),
        brick(2, 2, 188.54545455f, 1158f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_016.png"),
        brick(2, 3, 263.81818182f, 1158f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_005.png"),
        brick(2, 7, 564.90909091f, 1158f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_005.png"),
        brick(2, 8, 640.18181818f, 1158f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_016.png"),
        brick(2, 9, 715.45454545f, 1158f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_005.png"),
        brick(2, 10, 790.72727273f, 1158f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 9, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_061.png"),
        brick(4, 0, 38f, 1018f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_004.png"),
        brick(4, 1, 113.27272727f, 1018f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_004.png"),
        brick(4, 9, 715.45454545f, 1018f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_004.png"),
        brick(4, 10, 790.72727273f, 1018f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_004.png"),
        brick(5, 0, 38f, 948f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_004.png"),
        brick(5, 1, 113.27272727f, 948f, 71.27272727f, 66f, BrickType.CRYSTAL_BLUE, 3, VolleyPieceShape.RECTANGLE, ""),
        brick(5, 3, 263.81818182f, 948f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_004.png"),
        brick(5, 4, 339.09090909f, 948f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_004.png"),
        brick(5, 5, 414.36363636f, 948f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_004.png"),
        brick(5, 6, 489.63636364f, 948f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_004.png"),
        brick(5, 7, 564.90909091f, 948f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_004.png"),
        brick(5, 9, 715.45454545f, 948f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_012.png"),
        brick(5, 10, 790.72727273f, 948f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_004.png"),
        brick(6, 0, 38f, 878f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_004.png"),
        brick(6, 1, 113.27272727f, 878f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_016.png"),
        brick(6, 3, 263.81818182f, 878f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_041.png"),
        brick(6, 4, 339.09090909f, 878f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_004.png"),
        brick(6, 5, 414.36363636f, 878f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_050.png"),
        brick(6, 6, 489.63636364f, 878f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_004.png"),
        brick(6, 7, 564.90909091f, 878f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_040.png"),
        brick(6, 9, 715.45454545f, 878f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_016.png"),
        brick(6, 10, 790.72727273f, 878f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 7, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_004.png"),
        brick(7, 0, 38f, 808f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_016.png"),
        brick(7, 1, 113.27272727f, 808f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_040.png"),
        brick(7, 4, 339.09090909f, 808f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_041.png"),
        brick(7, 5, 414.36363636f, 808f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_061.png"),
        brick(7, 6, 489.63636364f, 808f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_040.png"),
        brick(7, 9, 715.45454545f, 808f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_027.png"),
        brick(7, 10, 790.72727273f, 808f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_016.png"),
        brick(8, 0, 38f, 738f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_040.png"),
        brick(8, 10, 790.72727273f, 738f, 71.27272727f, 66f, BrickType.CRYSTAL_GREEN, 3, VolleyPieceShape.RECTANGLE, "spritesheet_c1842d55_027.png")
    )
}
