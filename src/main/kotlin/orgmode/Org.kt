
package orgmode

abstract class Org(entities: Array<Org> = emptyArray()) {

    var entities: Array<Org> = entities
	get

    override fun toString(): String {
	var res: String = ""

	for(e in entities) {
	    res += e.toString();
	}

	return res
    }

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
    
}

class Paragraph(level: Int, entities: Array<Org> = emptyArray()) : Org(entities) {

    val level: Int = level
    
    override fun toString(): String {
	var res: String = ""

	for(e in entities) {

	    for(i in 1..level) res += "  "
	    
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

	return "\"paragraph\": { \"level\": $level, \"lines\": [$lines] }"
						 
    }

    override fun equals(other: Any?): Boolean {
	if(other !is Paragraph) return false
	return other.level == level && super.equals(other)
    }

}

open class Text(text: String) : Org() {

    var text: String = text
	get

    override fun toString(): String = text
    override fun toJson(): String = text

    override fun equals(other: Any?): Boolean {
	if(other !is Text) return false
	return other.text == text
    }

}

open class Section(text: String, level: Int, entities: Array<Org> = emptyArray()) : Org(entities) {

    var level: Int = level

    var text: String = text

    override fun toString(): String {
	var prefix: String = "\n"

	for(i in 1..level-1) {
	    prefix += "  "
	}

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

    override fun equals(other: Any?): Boolean {
	if(other !is Section) return false
	return other.text == text && other.level == level && super.equals(other)
    }
    
}

class Document(entities: Array<Org> = emptyArray()) : Section("", 0, entities) {

    override fun toString(): String {
	return (this as Org).toString()
    }
    
    override fun toJson(): String {
	var elements: String = ""

	for(i in entities.indices) {
	    if(i != 0) elements += ", "
	    elements += "{ " + entities[i].toJson() + " }"
	}

	return "{ \"document\": { \"elements\": [$elements] } }"
    }

    override fun equals(other: Any?): Boolean {
	if(other !is Document) return false
	return super.equals(other)
    }
}
