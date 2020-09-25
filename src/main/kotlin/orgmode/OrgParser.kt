
package orgmode

class OrgParser(src: Source) : AbstractParser<Org>(src) {


    override fun parse(): Org {
	var root: Section = Document()
	parseSection(root)
	return root
    }
    
    fun parseSection(root: Section): Section? {

	var lines: List<Org> = emptyList()

	var skip: Boolean = false

	var element: Org = Text("No way")
	
	while(!src.isEof()) {

	    if (!skip) {
		element = parseLine()
	    }
	    skip = false

	    if(element is Section) {
		if (!lines.isEmpty()) root.add(Paragraph(lines))

		if(element.level > root.level) {
		    var section: Section? = parseSection(element)
		    while(true) {
			root.add(element)
			
			if(section == null || section.level <= root.level) {
			    return section
			}
			
			element = section
			section = parseSection(element)
		    }
		} else return element
	    } else if(element is ListEntry) {
		if(!lines.isEmpty()) {
		    root.add(Paragraph(lines))
		    lines = emptyList()
		}
		var (newElement, _, list, _)  = parseList(element)
		root.add(list)
		skip = true
		element = newElement
	    } else if(element is MarkupText && element.isEmpty()) {
		if(!lines.isEmpty()) {
		    root.add(Paragraph(lines))
		    lines = emptyList()
		}
	    } else {
		lines += element
	    }
	    
	}
	if(!lines.isEmpty()) root.add(Paragraph(lines))
	return null

    }

    fun parseLine(): Org {

	if(test('*')) {
	    return tryParseHeader();
	}

	val indent: Int = skipWhitespaces()
	val c: Char = src.getChar()
	if(testRange('0'..'9') || test('-')) {
	    return tryParseListEntry(c, indent)
	}
	
	return parseMarkup()
    }

    class MarkupStack(var list: MutableList<String> = mutableListOf()) {

	fun pop() = list.removeAt(list.size - 1)
	fun push(s: String) = list.add(list.size, s)
	fun has(s: String): Boolean {
	    for(e in list) {
		if(e == s) return true
	    }
	    return false
	}

	fun popIfHas(s: String): Boolean {
	    val res: Boolean = has(s)
	    if(res) {
		while(list[list.size - 1] != s) {
		    pop()
		}
	    }
	    return true
	}
	
    }
    
    public fun parseMarkup(root: MarkupText = MarkupText(), stack: MarkupStack = MarkupStack()): MarkupText {

	var word: String = ""

	while(!src.isEof() && !test('\n')) {
	    if(test('\\')) {
		if(test('\\')) {
		    if(test('\n')) {
			if(!word.isEmpty()) root.add(Text(word))
			root.add(LineBreak())
			return root
		    } else {
			word += '\\'
		    }
		} else {
		    word += src.getChar()
		    src.nextChar()
		}
		continue
	    }
	    
	    if(test(' ')) {
		if(!word.isEmpty()) root.add(Text(word))
		word = ""
	    } else {
		word += src.getChar()
		src.nextChar()
	    }
	}
	if(!word.isEmpty()) root.add(Text(word))

	return root
    }

    fun tryParseHeader(): Org {

	var level: Int = 1
	while(test('*')) level++

	return if(!test(' ')) {
	    var prefix: String = ""
	    for(i in 1..level) prefix += '*'
	    parseMarkup(MarkupText(listOf(Text(prefix))))
	} else Section(parseMarkup(), level)
    }
    
    fun tryParseListEntry(firstChar: Char, indent: Int): Org {
	var bullet: String =  "" + firstChar


	if(firstChar in '0'..'9') {
	    var c: Char = src.getChar()
	    while(testRange('0'..'9')) {
		bullet += c
		c = src.getChar()
	    }

	    if(!test('.')) return parseMarkup(MarkupText(listOf(Text(bullet))))
	    bullet += "."
	}
	
	
	if(!test(' ')) {
	    return parseMarkup(MarkupText(listOf(Text(bullet))))
	}

	return ListEntry(parseMarkup(), bullet, indent)

    }

    data class ListResult(var entry: Org, var indent: Int, var list: OrgList, var emptyLines: Int)
    
    /* FIXME i am ugly */
    fun parseList(entryVal: ListEntry): ListResult {

	var entry: ListEntry = entryVal
	var list: OrgList = OrgList(emptyList())
	var lines: List<Org> = emptyList()

	var skip: Boolean = false
	var nextEntry: Org? = null
	var nextIndent: Int = 0

	var emptyLines: Int = 0
	
	while(!src.isEof()) {
	    
	    if(!skip) {
		nextIndent = skipWhitespaces()
		var firstChar: Char = src.getChar()
		if(testRange('0'..'9') || test('-')) {
		    nextEntry = tryParseListEntry(firstChar, nextIndent)
		} else {
		    if(nextIndent != 0) {
			nextEntry = parseMarkup()
		    } else {
			nextEntry = parseLine()
		    }
		}
	    }
	    skip = false

	    nextEntry ?: throw ParserException("Skipped with null value")

	    if(nextEntry is MarkupText && nextEntry.isEmpty()) nextIndent = entry.indent + 1

	    if(nextIndent == entry.indent) {
		if(!lines.isEmpty()) entry.add(Paragraph(lines))
		lines = emptyList()
		list.add(entry)
		if(nextEntry is ListEntry) {
		    entry = nextEntry
		} else {
		    return ListResult(nextEntry, nextIndent, list, emptyLines)
		}
	    } else if(nextIndent < entry.indent)  {
		if(!lines.isEmpty()) entry.add(Paragraph(lines))
		list.add(entry)
		return ListResult(nextEntry, nextIndent, list, emptyLines)
	    } else {
		if(nextEntry is ListEntry) {
		    if(!lines.isEmpty()) entry.add(Paragraph(lines))
		    lines = emptyList()
		    val (a, b, c, d) = parseList(nextEntry)
		    nextEntry = a
		    nextIndent = b
		    emptyLines = d
		    entry.add(c)
		    skip = true
		} else {
		    if(nextEntry is MarkupText && nextEntry.isEmpty()) {
			emptyLines++
			if(!lines.isEmpty()) entry.add(Paragraph(lines))
			lines = emptyList()
		    } else {
			emptyLines = 0
			lines += nextEntry
		    }
		    if(emptyLines >= 2) {
			if(!lines.isEmpty()) entry.add(Paragraph(lines))
			list.add(entry)
			return ListResult(nextEntry, nextIndent, list, emptyLines)
		    }
		}
	    }	    
	}
	//ilya gay ©Arseniy
	if(!lines.isEmpty()) {
	    entry.add(Paragraph(lines))
	}
	list.add(entry)
	return ListResult(entry, entry.indent, list, emptyLines)
    }
    
}
