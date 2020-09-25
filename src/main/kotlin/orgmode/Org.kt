
package orgmode

abstract class Org(entities: List<Org> = emptyList()) {

    var entities: List<Org> = entities
	get

    override fun toString(): String = entities.fold("") {acc, e -> acc + e.toString()}

    fun add(element: Org): Unit {
	entities += element
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

class Paragraph(entities: List<Org> = emptyList()) : Org(entities) {

    override fun toString(): String {
	return entities.fold("") {acc, e -> acc + ' ' + e.toString()}
    }

    override fun toJson(): String {

	var lines: String = ""
	
	for(i in entities.indices) {
	    if(i != 0) {
		lines += ", "
	    }
	    lines += entities[i].toJson()
	}

	return "{ \"type\": \"paragraph\", \"lines\": [$lines] }"
						 
    }

    override fun equals(other: Any?): Boolean {
	if(other !is Paragraph) return false
	return super.equals(other)
    }

    override fun toHtml(): String {
	var innerHtml: String = entities.fold("") {acc, e -> acc + ' ' + e.toHtml()}
	return "<p>$innerHtml</p>"
    }

}

open class Text(text: String) : Org() {

    var text: String = text
	get

    override fun toString(): String = text
    override fun toJson(): String = "\"" + text + "\""
    override fun toHtml(): String = if(text[text.length - 1] == '\n') text.dropLast(1) + "</br>" else text

    override fun equals(other: Any?): Boolean {
	if(other !is Text) return false
	return other.text == text
    }

    fun isEmptyLine(): Boolean = text.isEmpty()

}

open class Section(text: String, level: Int, entities: List<Org> = emptyList()) : Org(entities) {

    var level: Int = level

    var text: String = text

    override fun toString(): String {
	var prefix: String = "\n"

	for(i in 1..level) {
	    prefix += '*'
	}

	return prefix + ' ' + text + '\n' + super.toString()
    }

    override fun toJson(): String {
	var elements: String = ""

	for(i in entities.indices) {
	    if(i != 0) elements += ", "
	    elements += entities[i].toJson()
	}

	return "{ \"type\": \"section\", \"header\": \"$text\", \"level\": $level, \"elements\": [$elements] }"
    }

    override fun toHtml(): String {
	var innerHtml: String = super.toHtml()
	return "<h$level>$text</h$level>$innerHtml"
    }

    override fun equals(other: Any?): Boolean {
	if(other !is Section) return false
	return other.text == text && other.level == level && super.equals(other)
    }
    
}

class Document(entities: List<Org> = emptyList()) : Section("", 0, entities) {

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
	    else -> throw OrgException("Unknown list type")
	}
    }
    override fun toString(): String {
	return entries.fold("") {acc, e -> acc + '\n' + e.toString()}
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
	NOTSET
    }
}

class ListEntry(val text:String, bullet: String = "-", val indent: Int = 0, entities: List<Org> = emptyList()): Org(entities) {

    public val bullet: String = bullet
    
    override fun toJson(): String = "{ \"type\": \"list_entry\", \"text\": \"$text\", \"entities\": [${super.toJson()}]}"
    override fun toHtml(): String {
	return "<li>$text</br>${super.toHtml()}</li>"
    }
    override fun toString(): String {
	var prefix: String = " ".repeat(indent)
	return "$prefix$bullet $text\n" + entities.fold("") {acc, e -> acc + " ".repeat(bullet.length + 1) + e.toString()}
    }

    override fun equals(other: Any?): Boolean {
	if(other !is ListEntry) return false
	return other.text == this.text && super.equals(other)
    }
    
}
