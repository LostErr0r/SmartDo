package com.example.myapplication.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Простое перетаскивание элементов внутри LazyColumn «за ручку».
 *
 * @param draggableRange диапазон LAZY-индексов, внутри которого разрешено перетаскивание
 *        (например, если перед элементами есть шапки).
 * @param onMove вызывается с индексами ОТНОСИТЕЛЬНО начала диапазона (0-based в данных).
 */
class ReorderListState(
    val listState: LazyListState,
    private val draggableRange: () -> IntRange,
    private val onMove: (from: Int, to: Int) -> Unit
) {
    var draggingKey by mutableStateOf<Any?>(null)
        private set
    var offsetY by mutableFloatStateOf(0f)
        private set

    private var draggingIndex: Int? = null

    private fun itemInfo(index: Int): LazyListItemInfo? =
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }

    fun onDragStart(index: Int, key: Any) {
        draggingIndex = index
        draggingKey = key
        offsetY = 0f
    }

    fun onDrag(deltaY: Float) {
        val range = draggableRange()
        offsetY += deltaY
        val currentIndex = draggingIndex ?: return
        val current = itemInfo(currentIndex) ?: return
        val middle = current.offset + offsetY + current.size / 2f
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.index != currentIndex &&
                item.index in range &&
                middle.toInt() in item.offset..(item.offset + item.size)
        } ?: return
        onMove(currentIndex - range.first, target.index - range.first)
        offsetY += current.offset - target.offset
        draggingIndex = target.index
    }

    fun onDragEnd() {
        draggingIndex = null
        draggingKey = null
        offsetY = 0f
    }
}

@Composable
fun rememberReorderListState(
    listState: LazyListState,
    draggableRange: () -> IntRange,
    onMove: (from: Int, to: Int) -> Unit
): ReorderListState = remember(listState) {
    ReorderListState(listState, draggableRange, onMove)
}

/** Модификатор для иконки-ручки: тащит элемент с данным lazy-индексом и ключом. */
fun Modifier.dragHandle(
    state: ReorderListState,
    index: Int,
    key: Any,
    onDragEnd: () -> Unit
): Modifier = this.pointerInput(key) {
    detectDragGestures(
        onDragStart = { state.onDragStart(index, key) },
        onDrag = { change, amount ->
            change.consume()
            state.onDrag(amount.y)
        },
        onDragEnd = {
            state.onDragEnd()
            onDragEnd()
        },
        onDragCancel = {
            state.onDragEnd()
            onDragEnd()
        }
    )
}
