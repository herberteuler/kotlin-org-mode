
package orgmode

val HTML_SPECIAL = mapOf(
    '<' to "&lt",
    '>' to "&gt"
)
fun String.htmlEscape(): String = fold("") { acc, e -> acc + HTML_SPECIAL.getOrDefault(e, e.toString()) }

abstract class Org(entities: List<Org> = emptyList()) {

    var entities: List<Org> = entities
        get

    override fun toString(): String = entities.fold("") { acc, e -> acc + e.toString() }

    open fun add(element: Org): Org {
        entities += element
        return this
    }

    override operator fun equals(other: Any?): Boolean {
        if (other !is Org) return false
        if (other.entities.size != entities.size) return false
        for (i in entities.indices) {
            if (other.entities[i] != entities[i]) return false
        }

        return true
    }

    open fun toJson(): String = entities.foldIndexed("") {
        i, acc, e ->
        if (i != 0) acc + ", " + e.toJson() else e.toJson()
    }
    open fun toHtml(): String = entities.fold("") { acc, e -> acc + e.toHtml() }

    open fun toMarkdown(): String = entities.foldIndexed("") { i, acc, e -> if (i != 0) acc + ' ' + e.toMarkdown() else acc + e.toMarkdown()}
}

abstract class Block(var lines: List<String> = listOf()) : Org(listOf()) {

    override fun equals(other: Any?): Boolean {
        if (other !is Block) return false
        if (lines.size != other.lines.size) return false
        for (i in lines.indices) {
            if (lines[i] != other.lines[i]) return false
        }
        return true
    }

    fun add(line: String) {
        lines += line
    }
}

class CodeBlock(lines: List<String> = listOf()) : Block(lines) {
    override fun toJson(): String = """{ "type": "code_block", "lines": [${
    lines.foldIndexed("") {i, acc, e -> acc + (if (i != 0) ", \"" else "") + e + '"'}
    }]}"""
    override fun toHtml(): String = """<pre><code>${lines.fold("") {acc, e -> acc + e + "\n"}}</code></pre>"""
    override fun toString(): String = """#+BEGIN_SRC${lines.fold("") {acc, e -> acc + "\n" + e}}#+END_SRC"""
    override fun toMarkdown(): String = """```${'\n'}${lines.fold("") {acc, e -> acc + e + '\n'}}```"""
}
