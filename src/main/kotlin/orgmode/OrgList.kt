
package orgmode

enum class LIST_CHECKBOX {
    CHECKED {
        override fun toHtml(): String = """<input type="checkbox" checked disabled>"""
        override fun toString(): String = "[X]"
        override fun toMarkdown(): String = "[X]"
    },
    UNCHECKED {
        override fun toHtml(): String = """<input type="checkbox" disabled>"""
        override fun toString(): String = "[ ]"
        override fun toMarkdown(): String = "[ ]"
    },
    PARTIAL_CHECKED {
        override fun toHtml(): String = """<input type="checkbox" disabled>"""
        override fun toString(): String = "[-]"
        override fun toMarkdown(): String = "[ ]"
    },
    NONE {
        override fun toHtml(): String = ""
        override fun toString(): String = ""
        override fun toMarkdown(): String = ""
    };

    abstract fun toHtml(): String
    abstract fun toMarkdown(): String
}

class OrgList(entries: List<ListEntry> = listOf()) : Org(listOf()) {

    var entries: List<ListEntry> = entries

    var type: BULLET = BULLET.NOTSET

    fun add(entry: ListEntry) {
        entries += entry
        if (type == BULLET.NOTSET) parseType()
    }

    private fun parseType() {

        if (entries[0].bullet[0] in '0'..'9') {
            if (entries[0].bullet[entries[0].bullet.length - 1] == '.') {
                type = BULLET.NUM_DOT
            } else if (entries[0].bullet[entries[0].bullet.length - 1] == ')') {
                type = BULLET.NUM_PARENTHESIS
            } else throw ParserException("Unknow bullet type")
        } else if (entries[0].bullet[0] == '-') {
            type = BULLET.DASH
        } else if (entries[0].bullet[0] == '+') {
            type = BULLET.PLUS
        } else {
            throw ParserException("Unknow bullet type")
        }
    }

    init {
        if (!entries.isEmpty()) {
            parseType()
        }
    }

    override fun toJson(): String {
        return """{ "type": "list", "list_type": "$type", "entries": [${
        entries.foldIndexed("") {
            i, acc, e ->
            if (i != 0) acc + ", " + e.toJson() else e.toJson()
        }
        }] }"""
    }
    override fun toHtml(): String {
        val elements: String = entries.fold("") { acc, e -> acc + e.toHtml() }
        return when (type) {
            BULLET.NUM_DOT -> "<ol>$elements</ol>"
            BULLET.NUM_PARENTHESIS -> "<ol>$elements</ol>"
            BULLET.DASH -> "<ul>$elements</ul>"
            BULLET.PLUS -> "<ul>$elements</ul>"
            else -> throw OrgException("Unknown list type")
        }
    }
    override fun toString(): String {
        return entries.fold("") { acc, e -> acc + '\n' + e.toString() } + '\n'
    }
    override fun toMarkdown(): String {
        return entries.fold("") { acc, e -> acc + '\n' + e.toMarkdown() } + '\n'
    }

    override fun equals(other: Any?): Boolean {
        if (other !is OrgList) return false
        if (other.type != type) return false
        if (other.entries.size != entries.size) return false

        for (i in entries.indices) {
            if (other.entries[i] != entries[i]) return false
        }

        return true
    }

    public enum class BULLET {
        NUM_PARENTHESIS,
        NUM_DOT,
        DASH,
        PLUS,
        NOTSET
    }
}

class ListEntry(
    val text: MarkupText,
    bullet: String = "-",
    val indent: Int = 0,
    entities: List<Org> = emptyList(),
    var checkbox: LIST_CHECKBOX = LIST_CHECKBOX.NONE,
    var counter: Int? = null
) : Org(entities) {

    public val bullet: String = bullet

    override fun toJson(): String = """{ "type": "list_entry", "text": ${text.toJson()}, "entities": [${super.toJson()}]}"""
    override fun toHtml(): String {
        return """<li>${
        if (checkbox != LIST_CHECKBOX.NONE) " " + checkbox.toHtml() + " " else ""
        }${text.toHtml()}</br>${super.toHtml()}</li>"""
    }
    override fun toString(): String {
        return "${" ".repeat(indent)}$bullet${if (checkbox != LIST_CHECKBOX.NONE) " " + checkbox.toString() else ""} ${text.toString()}\n" +
        entities.fold("") {acc, e -> acc + " ".repeat(bullet.length + 1) + e.toString()}
    }
    override fun toMarkdown(): String {
        return "${" ".repeat(indent)}$bullet${if (checkbox != LIST_CHECKBOX.NONE) " " + checkbox.toMarkdown() else ""} ${text.toMarkdown()}\n" +
        entities.fold("") {acc, e -> acc + " ".repeat(bullet.length + 1) + e.toMarkdown()}
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ListEntry) return false
        return other.text == this.text && checkbox == other.checkbox && super.equals(other)
    }
}
