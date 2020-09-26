
package orgmode


val HTML_SPECIAL = mapOf('<' to "&lt",
			 '>' to "&gt")
fun String.htmlEscape(): String = fold("") {acc, e -> acc + HTML_SPECIAL.getOrDefault(e, e.toString())}

abstract class Org(entities: List<Org> = emptyList()) {

    var entities: List<Org> = entities
	get

    override fun toString(): String = entities.fold("") {acc, e -> acc + e.toString()}

    open fun add(element: Org): Org {
	entities += element
	return this
    }

    override operator fun equals(other: Any?): Boolean {
	if (other !is Org) return false
	if(other.entities.size != entities.size) return false
	for(i in entities.indices) {
	    if(other.entities[i] != entities[i]) return false;
	}

	return true;
    }
    
    open fun toJson(): String {
	var res: String = ""

	for(i in entities.indices) {
	    if(i != 0) {
		res += ", "
	    }
	    res += entities[i].toJson()
	}

	return res
    }
    open fun toHtml(): String = entities.fold("") {acc, e -> acc + e.toHtml()}
    
}

enum class MARKUP_TYPE {
    REGULAR, EMPHASIS, PARAGRAPH, CODE, UNDERLINE, STRIKEOUT, ITALIC, TEXT
}

class Paragraph(entities: List<MarkupText> = emptyList(), other: MarkupText? = null): MarkupText(entities, other) {

    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.PARAGRAPH
    
    override fun toString(): String = "${super.toString()}\n"
    override fun toHtml(): String = "<p>${super.toHtml()}</p>"

    override fun equals(other: Any?): Boolean {
	if(other !is Paragraph) return false
	return super.equals(other)
    }

}
class Code(text: String): Text(text, false) {

    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.CODE
    
    override fun toString(): String = "=${this.text}="
    override fun toHtml(): String = "<code>${this.text.htmlEscape()}</code>"
    override fun toJson(): String = "{\"type\": \"markup\", \"markup_type\": \"CODE\", \"code\": ${super.toJson()}}"

    override fun equals(other: Any?): Boolean {
	if(other !is Code) return false
	return text == other.text
    }

}
class Underline(entities: List<MarkupText> = emptyList(), other: MarkupText? = null): MarkupText(entities, other) {

    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.UNDERLINE
    
    override fun toString(): String = "_${super.toString()}_"
    override fun toHtml(): String = "<u>${super.toHtml()}</u>"

    override fun equals(other: Any?): Boolean {
	if(other !is Underline) return false
	return super.equals(other)
    }

}
class Strikeout(entities: List<MarkupText> = emptyList(), other: MarkupText? = null): MarkupText(entities, other) {

    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.STRIKEOUT
    
    override fun toString(): String = "+${super.toString()}+"
    override fun toHtml(): String = "<s>${super.toHtml()}</s>"

    override fun equals(other: Any?): Boolean {
	if(other !is Strikeout) return false
	return super.equals(other)
    }

}
class Italic(entities: List<MarkupText> = emptyList(), other: MarkupText? = null): MarkupText(entities, other) {

    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.ITALIC
    
    override fun toString(): String = "/${super.toString()}/"
    override fun toHtml(): String = "<i>${super.toHtml()}</i>"

    override fun equals(other: Any?): Boolean {
	if(other !is Italic) return false
	return super.equals(other)
    }

}

class Emphasis(entities: List<MarkupText> = emptyList(), other: MarkupText? = null): MarkupText(entities, other) {

    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.EMPHASIS
    
    override fun toString(): String = "*${super.toString()}*"
    override fun toHtml(): String = "<b>${super.toHtml()}</b>"

    override fun equals(other: Any?): Boolean {
	if(other !is Emphasis) return false
	return super.equals(other)
    }

}

class LineBreak() : Text("\n") {
    override fun toHtml(): String = "</br>"
    override fun toJson(): String = "{\"type\": \"line_break\"}"

    override fun isEmpty(): Boolean = true
}

open class MarkupText(entities: List<MarkupText> = emptyList(), other: MarkupText? = null): Org() {

    open fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.REGULAR

    init {
	if(other != null) {
	    for(e in other.entities) {
		add(e)
	    }
	} else {
	    for(e in entities) {
		add(e)
	    }
	}
    }

    override fun toString(): String = entities.foldIndexed("") {
	i, acc, e -> if(i == 0 || (i > 0 && entities[i - 1] is LineBreak)) acc + e.toString() else acc + " " + e.toString()
    }
    override fun toJson(): String = "{ \"type\": \"markup\", \"markup_type\": \"${getMarkupType()}\", \"elements\": [" + entities.foldIndexed("") {
	i, acc, e -> if(i == 0) acc + e.toJson() else acc + ", " + e.toJson()
    } + "] }"
    override fun toHtml(): String = entities.foldIndexed("") {
	i, acc, e -> if(i == 0) acc + e.toHtml() else acc + " " + e.toHtml()
    }

    open fun isEmpty(): Boolean = entities.all { (it as MarkupText).isEmpty() }
    
    override fun equals(other: Any?): Boolean {
	if(other !is MarkupText) return false
	return super.equals(other)
    }

    open fun add(other: MarkupText): MarkupText {
	if(other.getMarkupType() == getMarkupType() || (getMarkupType() == MARKUP_TYPE.PARAGRAPH && other.getMarkupType() == MARKUP_TYPE.REGULAR)) {
	    for(e in other.entities) {
		add(e)
	    }
	} else {
	    entities += other
	}
	return this
    }

    override fun add(element: Org): MarkupText {
	if(entities.size == 0) {
	    entities += element
	} else {
	    val last: Org = entities[entities.size - 1]
	    if(last is Text && element is Text && last.skipSpace) {
		last.text += element.text
		last.skipSpace = element.skipSpace
	    } else {
		entities += element
	    }
	}
	return this
    }
}

open class Text(text: String, skipSpace: Boolean = false): MarkupText() {

    var skipSpace: Boolean = skipSpace

    override fun getMarkupType(): MARKUP_TYPE = MARKUP_TYPE.TEXT
    
    var text: String = text
	get

    override fun toString(): String = text
    override fun toJson(): String = "\"" + text + "\""
    override fun toHtml(): String = text.htmlEscape()

    override fun equals(other: Any?): Boolean {
	if(other !is Text) return false
	return other.text == text
    }

    override fun isEmpty(): Boolean = text.isEmpty()

}

open class Section(text: MarkupText, level: Int, entities: List<Org> = emptyList()) : Org(entities) {

    var level: Int = level

    var text: MarkupText = text

    override fun toString(): String {
	var prefix: String = "\n"

	for(i in 1..level) {
	    prefix += '*'
	}

	return "$prefix ${text.toString()}\n${super.toString()}"
    }

    override fun toJson(): String {
	var elements: String = ""

	for(i in entities.indices) {
	    if(i != 0) elements += ", "
	    elements += entities[i].toJson()
	}

	return "{ \"type\": \"section\", \"header\": ${text.toJson()}, \"level\": $level, \"elements\": [$elements] }"
    }

    override fun toHtml(): String {
	var innerHtml: String = super.toHtml()
	return "<h$level>${text.toHtml()}</h$level>$innerHtml"
    }

    override fun equals(other: Any?): Boolean {
	if(other !is Section) return false
	return other.text == text && other.level == level && super.equals(other)
    }
    
}

class Document(entities: List<Org> = emptyList()) : Section(Text(""), 0, entities) {

    override fun toString(): String {
	// return (this as Org).toString() FIXME
	return entities.fold("") {acc, e -> acc + e.toString()}
    }
    
    override fun toJson(): String {
	var elements: String = ""

	for(i in entities.indices) {
	    if(i != 0) elements += ", "
	    elements += entities[i].toJson()
	}

	return "{ \"type\": \"document\", \"elements\": [$elements] }"
    }

    override fun toHtml(): String {
	// val innerHtml: String = super.toHtml() FIXME
	var innerHtml: String = entities.fold("") {acc, e -> acc + e.toHtml()}
	return "<html><head></head><body>$innerHtml</body></html>"
    }

    override fun equals(other: Any?): Boolean {
	if(other !is Document) return false
	return super.equals(other)
    }
}

class OrgList(entries: List<ListEntry>): Org(emptyList()) {

    var entries: List<ListEntry> = entries

    var type: BULLET = BULLET.NOTSET
    
    fun add(entry: ListEntry) {
	entries += entry
	if (type == BULLET.NOTSET) parseType()
    }

    private fun parseType() {

	if(entries[0].bullet[0] in '0'..'9') {
	    if(entries[0].bullet[entries[0].bullet.length - 1] == '.') {
		type = BULLET.NUM_DOT
	    } else throw ParserException("Unknow bullet type")
	} else if(entries[0].bullet[0] == '-') {
	    type = BULLET.DASH
	} else if(entries[0].bullet[0] == '+') {
	    type = BULLET.PLUS
	} else {
	    throw ParserException("Unknow bullet type")
	}
	
    }

    init {
	if(!entries.isEmpty()) {
	    parseType()
	}
    }

    override fun toJson(): String {
	var ents: String = ""

	for(i in entries.indices) {
	    if(i != 0) {
		ents += ", "
	    }
	    ents += entries[i].toJson()
	}
	
	return "{ \"type\": \"list\", \"list_type\": \"${type}\", \"entries\": [$ents] }"
    }
    override fun toHtml(): String {
	val elements: String = entries.fold("") {acc, e -> acc + e.toHtml()}
	return when(type) {
	    BULLET.NUM_DOT -> "<ol>$elements</ol>"
	    BULLET.DASH -> "<ul>$elements</ul>"
	    BULLET.PLUS -> "<ul>$elements</ul>"
	    else -> throw OrgException("Unknown list type")
	}
    }
    override fun toString(): String {
	return entries.fold("") {acc, e -> acc + '\n' + e.toString()} + "\n"
    }

    override fun equals(other: Any?): Boolean {
	if(other !is OrgList) return false
	if(other.type != type) return false
	if(other.entries.size != entries.size) return false

	for(i in entries.indices) {
	    if(other.entries[i] != entries[i]) return false
	}

	return true
    }

    public enum class BULLET {
	NUM_DOT,
	DASH,
	PLUS,
	NOTSET
    }
}

class ListEntry(val text: MarkupText, bullet: String = "-", val indent: Int = 0, entities: List<Org> = emptyList()): Org(entities) {

    public val bullet: String = bullet
    
    override fun toJson(): String = "{ \"type\": \"list_entry\", \"text\": ${text.toJson()}, \"entities\": [${super.toJson()}]}"
    override fun toHtml(): String {
	return "<li>${text.toHtml()}</br>${super.toHtml()}</li>"
    }
    override fun toString(): String {
	var prefix: String = " ".repeat(indent)
	return "$prefix$bullet ${text.toString()}\n" + entities.fold("") {acc, e -> acc + " ".repeat(bullet.length + 1) + e.toString()}
    }

    override fun equals(other: Any?): Boolean {
	if(other !is ListEntry) return false
	return other.text == this.text && super.equals(other)
    }
    
}
