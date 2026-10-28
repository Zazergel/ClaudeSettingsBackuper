package app

/**
 * Иммутабельные операции над JSON-деревом для UI-формы редактора.
 * Правки возвращают новое дерево; writer читает только значения ключей,
 * поэтому у созданных узлов фиктивные offsets (-1) — подсветка работает по тексту, не по дереву.
 */

fun valueAt(root: JsonNode.Obj, path: List<String>): JsonNode? {
    val entry = root.entries.firstOrNull { it.first.value == path.first() } ?: return null
    if (path.size == 1) return entry.second
    return (entry.second as? JsonNode.Obj)?.let { valueAt(it, path.drop(1)) }
}

fun setValue(root: JsonNode.Obj, path: List<String>, value: JsonNode): JsonNode.Obj {
    if (path.size == 1) {
        val entries = root.entries.toMutableList()
        val idx = entries.indexOfFirst { it.first.value == path[0] }
        if (idx >= 0) entries[idx] = entries[idx].first to value
        else entries.add(JsonNode.Str(path[0], -1, path[0].length + 2) to value)
        return JsonNode.Obj(entries.toList(), root.offset, root.length)
    }
    val child = root.entries.firstOrNull { it.first.value == path[0] }?.second as? JsonNode.Obj
        ?: JsonNode.Obj(emptyList(), -1, 0)
    return setValue(root, listOf(path[0]), setValue(child, path.drop(1), value))
}

fun removeValue(root: JsonNode.Obj, path: List<String>): JsonNode.Obj {
    if (path.size == 1) {
        return JsonNode.Obj(root.entries.filterNot { it.first.value == path[0] }, root.offset, root.length)
    }
    val child = root.entries.firstOrNull { it.first.value == path[0] }?.second as? JsonNode.Obj ?: return root
    return setValue(root, listOf(path[0]), removeValue(child, path.drop(1)))
}
