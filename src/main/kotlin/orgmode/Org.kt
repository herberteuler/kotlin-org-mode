
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
    
    abstract fun toJson(): String
    open fun toHtml(): String = entities.fold("") {acc, e -> acc + e.toHtml()}
    
}

class Paragraph(entities: List<Org> = emptyList()) : Org(entities) {

    override fun toString(): String {
	var res: String = ""

	for(e in entities) {
	    res += e.toString() + '\n'
	}

	return res
    }

    override fun toJson(): String {

	var lines: String = ""
	
	for(i in entities.indices) {
	    if(i != 0) {
		lines += ", "
	    }
	    lines += "\"" + entities[i].toJson() + "\""
	}

	return "\"paragraph\": { \"lines\": [$lines] }"
						 
    }

    override fun equals(other: Any?): Boolean {
	if(other !is Paragraph) return false
	return super.equals(other)
    }

    override fun toHtml(): String {
	var innerHtml: String = super.toHtml()
	return "<p>$innerHtml</p>"
    }

}

open class Text(text: String) : Org() {

    var text: String = text
	get

    override fun toString(): String = text
    override fun toJson(): String = text
    override fun toHtml(): String = text

    override fun equals(other: Any?): Boolean {
	if(other !is Text) return false
	return other.text == text
    }

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
	    elements += "{ " + entities[i].toJson() + " }"
	}

	return "\"section\": { \"header\": \"$text\", \"level\": $level, \"elements\": [$elements] }"
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
	    elements += "{ " + entities[i].toJson() + " }"
	}

	return "{ \"document\": { \"elements\": [$elements] } }"
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

    fun add(entry: ListEntry) { entries += entry }
    
    override fun toJson(): String = "NOT IMPLEMENTED"
    override fun toHtml(): String = "NOT IMPLEMENTED"
    override fun toString(): String {
	return entries.fold("") {acc, e -> acc + e.toString()}
    }
}

class ListEntry(val indent: Int, val bullet: String, val text:String, entities: List<Org> = emptyList()): Org(entities) {
    
    override fun toJson(): String = "NOT IMPLEMENTED"
    override fun toHtml(): String = "NOT IMPLEMENTED"
    override fun toString(): String {
	var prefix: String = " ".repeat(indent)
	return "$prefix$bullet $text\n" + entities.fold("") {acc, e -> acc + " ".repeat(bullet.length + 1) + e.toString()}
    }
    
}
