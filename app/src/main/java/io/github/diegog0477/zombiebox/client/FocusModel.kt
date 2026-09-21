package io.github.diegog0477.zombiebox.client

/** Stable semantic keys survive catalog refreshes, translated labels and reordered rows. */
class FocusModel {
    data class Row(val id: String, val keys: List<String>)
    private var rows = emptyList<Row>()
    private val remembered = HashMap<String, String>()
    var selected: String? = null
        private set

    fun rebuild(value: List<Row>) {
        val oldRow = rows.indexOfFirst { selected in it.keys }.coerceAtLeast(0)
        val oldColumn = rows.getOrNull(oldRow)?.keys?.indexOf(selected)?.coerceAtLeast(0) ?: 0
        require(value.map { it.id }.distinct().size == value.size)
        val keys = value.flatMap { it.keys }
        require(keys.distinct().size == keys.size)
        rows = value.filter { it.keys.isNotEmpty() }
        remembered.keys.retainAll(rows.map { it.id }.toSet())
        if (selected !in keys) {
            val row = rows.getOrNull(oldRow.coerceAtMost(rows.lastIndex.coerceAtLeast(0)))
            selected = row?.keys?.get(oldColumn.coerceAtMost(row.keys.lastIndex))
        }
        selected?.let { select(it) }
    }

    fun select(key: String) {
        val row = rows.firstOrNull { key in it.keys } ?: return
        selected = key
        remembered[row.id] = key
    }

    fun move(dx: Int, dy: Int): String? {
        val index = rows.indexOfFirst { selected in it.keys }
        if (index < 0) return selected
        val row = rows[index]
        val column = row.keys.indexOf(selected)
        val target = rows[(index + dy).coerceIn(0, rows.lastIndex)]
        val key = if (dy == 0) target.keys[(column + dx).coerceIn(0, target.keys.lastIndex)]
        else remembered[target.id]?.takeIf { it in target.keys } ?: target.keys[column.coerceAtMost(target.keys.lastIndex)]
        select(key)
        return key
    }
}
