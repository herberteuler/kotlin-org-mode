package orgmode

enum class MARKUP_TYPE {
    REGULAR, EMPHASIS, PARAGRAPH, CODE, UNDERLINE, STRIKEOUT, ITALIC, TEXT, LINK, STATISTIC_COOKIE, KEYWORD, LINE_BREAK
}

open class MarkupText(entities: List<MarkupText> = emptyList(), other: MarkupText? = null) : Org() {

    open fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.REGULAR

    init {
        if (other != null) {
            for (e in other.entities) {
                add(e)
            }
        } else {
            for (e in entities) {
                add(e)
            }
        }
    }

    override fun toString(): String = entities.foldIndexed("") {
        i, acc, e ->
        if (i == 0 || (i > 0 && entities[i - 1] is LineBreak)) acc + e.toString() else acc + " " + e.toString()
    }
    override fun toMarkdown(): String = entities.foldIndexed("") {
        i, acc, e ->
        if (i == 0 || (i > 0 && entities[i - 1] is LineBreak)) acc + e.toMarkdown() else acc + " " + e.toMarkdown()
    }
    override fun toJson(): String = """{ "type": "markup", "markup_type": "${getMarkupType()}", "elements": [${
    entities.foldIndexed("") {
        i, acc, e ->
        if (i == 0) acc + e.toJson() else acc + ", " + e.toJson()
    }}] }"""
    override fun toHtml(): String = entities.foldIndexed("") {
        i, acc, e ->
        if (i == 0) acc + e.toHtml() else acc + " " + e.toHtml()
    }

    open fun isEmpty(): Boolean = entities.all { (it as MarkupText).isEmpty() }

    override fun equals(other: Any?): Boolean {
        if (other !is MarkupText) return false
        return super.equals(other)
    }


    // :FIXME: These functions are quite bad
    //         It's for minimizing nesting
    open fun add(element: Text): MarkupText = add(element as Org)

    open fun add(other: MarkupText): MarkupText {
        if (other.getMarkupType() == getMarkupType() ||
            (getMarkupType() == MARKUP_TYPE.PARAGRAPH && other.getMarkupType() == MARKUP_TYPE.REGULAR)
        ) {
            for (e in other.entities) {
                add(e)
            }
        } else {
            entities += other
        }
        return this
    }

    override fun add(element: Org): MarkupText {
        if (entities.isEmpty()) {
            entities += element
            return this
        }
        val last: Org = entities[entities.size - 1]
        if (element is Text && last is Text && element.getMarkupType() == MARKUP_TYPE.TEXT &&
            last.getMarkupType() == MARKUP_TYPE.TEXT
        ) {
            if (last.skipSpace) {
                last.text += element.text
            } else {
                last.text += " " + element.text
            }
            last.skipSpace = element.skipSpace
        } else {
            entities += element
        }
        return this
    }
}

open class Text(text: String, skipSpace: Boolean = false) : MarkupText() {

    var skipSpace: Boolean = skipSpace

    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.TEXT

    var text: String = text.trim()
        get

    override fun toString(): String = text
    override fun toMarkdown(): String = text
    override fun toJson(): String = "\"$text\""
    override fun toHtml(): String = text.htmlEscape()

    override fun equals(other: Any?): Boolean {
        if (other !is Text) return false
        return other.text == text
    }

    override fun isEmpty(): Boolean = text.isEmpty()
}

class StatisticCookie(text: String) : Text(text) {
    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.STATISTIC_COOKIE

    override fun toMarkdown(): String = "`${text}`"
    override fun toHtml(): String = "<code>${text.htmlEscape()}</code>"
    override fun toJson(): String = """{ "type": "markup", "markup_type": "statistic", "text": "$text"}"""

    override fun equals(other: Any?): Boolean {
        if (other !is StatisticCookie) return false
        return other.text == text
    }

    override fun isEmpty(): Boolean = text.isEmpty()
}

open class Paragraph(entities: List<MarkupText> = emptyList(), other: MarkupText? = null) : MarkupText(entities, other) {

    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.PARAGRAPH

    override fun toString(): String = "${super.toString()}\n"
    override fun toMarkdown(): String = "${super.toMarkdown()}\n"
    override fun toHtml(): String = "<p>${super.toHtml()}</p>"

    override fun equals(other: Any?): Boolean {
        if (other !is Paragraph) return false
        return super.equals(other)
    }
}
class Link(url: String, entities: List<MarkupText> = emptyList(), other: MarkupText? = null) : MarkupText(entities, other) {

    val url: String = url

    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.LINK

    override fun toString(): String {
        if (entities.isEmpty()) {
            return "[[$url]]"
        } else {
            return "[[$url][${super.toString()}]]"
        }
    }
    override fun toMarkdown(): String {
        if (entities.isEmpty()) {
            if(url.endsWith(".svg")) {
                return "![Badge]($url)"
            }
            if(url.startsWith("file:")) {
                return "[${url.substring(5)}]"
            }
            return "[$url]"
        } else {
            if(url.startsWith("file:")) {
                return "[${super.toMarkdown()}](${url.substring(5)})"
            }
            return "[${super.toMarkdown()}]($url)"
        }
    }
    override fun toHtml(): String {
        if (entities.isEmpty()) {
            return """<img src = "$url" />"""
        } else {
            return """<a href = "$url">${super.toHtml()}</a>"""
        }
    }

    override fun toJson(): String {
        if (entities.isEmpty()) {
            return """{ "type": "markup", "markup_type": "LINK", "url": "$url"}"""
        } else {
            return """{ "type": "markup", "markup_type": "LINK", "url": "$url", "elements": [${entities.foldIndexed("") {
                i, acc, e ->
                if (i == 0) acc + e.toJson() else acc + ", " + e.toJson()
            }}] }"""
        }
    }

    override fun isEmpty(): Boolean = false

    override fun equals(other: Any?): Boolean {
        if (other !is Link) return false
        return url == other.url && super.equals(other)
    }
}
class Code(text: String) : Text(text, false) {

    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.CODE

    override fun toString(): String = "=${this.text}="
    override fun toHtml(): String = "<code>${this.text.htmlEscape()}</code>"
    override fun toJson(): String = """{"type": "markup", "markup_type": "CODE", "code": ${super.toJson()}}"""
    override fun toMarkdown(): String = "`${super.toMarkdown()}`"

    override fun equals(other: Any?): Boolean {
        if (other !is Code) return false
        return text == other.text
    }
}
class Underline(entities: List<MarkupText> = emptyList(), other: MarkupText? = null) : MarkupText(entities, other) {

    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.UNDERLINE

    override fun toString(): String = "_${super.toString()}_"
    override fun toHtml(): String = "<u>${super.toHtml()}</u>"
    override fun toMarkdown(): String = "${super.toMarkdown()}"

    override fun equals(other: Any?): Boolean {
        if (other !is Underline) return false
        return super.equals(other)
    }
}
class Strikeout(entities: List<MarkupText> = emptyList(), other: MarkupText? = null) : MarkupText(entities, other) {

    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.STRIKEOUT

    override fun toString(): String = "+${super.toString()}+"
    override fun toHtml(): String = "<s>${super.toHtml()}</s>"
    override fun toMarkdown(): String = "~~${super.toMarkdown()}~~"

    override fun equals(other: Any?): Boolean {
        if (other !is Strikeout) return false
        return super.equals(other)
    }
}
class Italic(entities: List<MarkupText> = emptyList(), other: MarkupText? = null) : MarkupText(entities, other) {

    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.ITALIC

    override fun toString(): String = "/${super.toString()}/"
    override fun toMarkdown(): String = "*${super.toMarkdown()}*"
    override fun toHtml(): String = "<i>${super.toHtml()}</i>"

    override fun equals(other: Any?): Boolean {
        if (other !is Italic) return false
        return super.equals(other)
    }
}

class Emphasis(entities: List<MarkupText> = emptyList(), other: MarkupText? = null) : MarkupText(entities, other) {

    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.EMPHASIS

    override fun toString(): String = "*${super.toString()}*"
    override fun toMarkdown(): String = "**${super.toMarkdown()}**"
    override fun toHtml(): String = "<b>${super.toHtml()}</b>"

    override fun equals(other: Any?): Boolean {
        if (other !is Emphasis) return false
        return super.equals(other)
    }
}

class Keyword(var key: String, var value: String) : MarkupText(listOf(), null) {
    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.KEYWORD
    override fun toString(): String = "#+$key: $value"
    override fun toMarkdown(): String = ""
    override fun toJson(): String = """{"type": "keyword", "key": "$key", "value": "$value"}"""
    override fun toHtml(): String = ""
    override fun equals(other: Any?): Boolean {
        if (other !is Keyword) return false
        return key == other.key && value == other.value
    }
}

class LineBreak() : Text("\n") {
    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.LINE_BREAK
    override fun toHtml(): String = "</br>"
    override fun toJson(): String = """{"type": "line_break"}"""
    override fun toString(): String = "\\\\\n"
    override fun toMarkdown(): String = "\\\n"

    override fun isEmpty(): Boolean = true
}
