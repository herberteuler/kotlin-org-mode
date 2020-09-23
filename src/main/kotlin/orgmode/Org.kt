
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
    
}

class Paragraph(level: Int, entities: Array<Org> = emptyArray()) : Org(entities) {

    val level: Int = level
    
    override fun toString(): String {
	var res: String = ""

	
	for(e in entities) {

	    for(i in 1..level) {
		res += "  "
	    }

	    res += e.toString() + '\n'
	}

	return res
    }
    
}

open class Text(text: String) : Org() {

    var text: String = text
	get

    override fun toString(): String = text

}

class Section(text: String, level: Int, entities: Array<Org> = emptyArray()) : Org(entities) {

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
}

class Document(entities: Array<Org> = emptyArray()) : Org(entities)
