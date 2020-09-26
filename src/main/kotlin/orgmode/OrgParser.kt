
package orgmode

class OrgParser(src: Source) : AbstractParser<Org>(src) {


    override fun parse(): Org {
	var root: Section = Document()
	parseSection(root)
	return root
    }
    
    fun parseSection(root: Section): Section? {

	var lines: List<MarkupText> = emptyList()

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
	    } else if(element is MarkupText){
		lines += element as MarkupText
	    } else {
		throw ParserException("Unknown type when parsenf section")
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
	if(testRange('0'..'9') || test('-') || test('+')) {
	    return tryParseListEntry(c, indent)
	}
	
	return parseMarkup()
    }

    class MarkupStack(list: MutableList<Char> = mutableListOf()) {

	var list: MutableList<Pair<Char, Int>> = mutableListOf()
	var cnt: Int = 0

	init {
	    for(c in list) {
		push(c)
	    }
	}
	
	fun pop() {
	    list.removeAt(list.size - 1)
	}
	fun push(c: Char): Int {
	    list.add(list.size, Pair(c, cnt))
	    cnt++
	    return cnt - 1
	}
	fun has(c: Char): Boolean {
	    return list.any { it.first == c }
	}
	fun has(id: Int): Boolean {
	    return list.any { it.second == id }
	}

	fun popIfHas(c: Char): Boolean {
	    if(has(c)) {
		while(list[list.size - 1].first != c) {
		    pop()
		}
		pop()
		return true
	    } else {
		return false
	    }
	}

	fun popUntil(id: Int): Boolean {
	    if(has(id)) {
		while(list[list.size - 1].second != id) {
		    pop()
		}
		pop()
		return true
	    } else {
		return false
	    }
	}
	
    }

    var markup_symbols: List<Char> = listOf('*', '/', '+', '=', '_')
    
    fun getMarkup(symbol: Char, text: MarkupText): MarkupText {
	return when(symbol) {
	    '*' -> ::Emphasis
	    '/' -> ::Italic
	    '+' -> ::Strikeout
	    '=' -> ::Code
	    '_' -> ::Underline
	    else -> ::MarkupText
	}(listOf(), text)
    }

    fun parseMarkupWithSymbol(symbol: Char, prefix: String = ""): MarkupText {
	var stack: MarkupStack = MarkupStack()
	var root: MarkupText = MarkupText()
	
	val id: Int = stack.push(symbol)

	var res = parseMarkup(stack = stack)

	if(stack.has(id)) {
	    root.add(Text(prefix + symbol, skipSpace = true))
	    root.add(res)
	} else {
	    if(prefix != "") root.add(Text(prefix, skipSpace = true))
	    root.add(res)
	    res = parseMarkup()
	    root.add(res)
	}
	
	return root
    }
    
    fun parseMarkup(root: MarkupText = MarkupText(), stack: MarkupStack = MarkupStack(), prefix: String = ""): MarkupText {

	var word: String = prefix

	var beginOfLine: Boolean = true

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

	    if(beginOfLine || test(' ')) {
		beginOfLine = false
		if(!word.isEmpty()) root.add(Text(word))
		word = ""

		var symbol: Char? = null

		for(s in markup_symbols) {
		    if(test(s)) {
			symbol = s
			break
		    }
		}
		
		if(symbol != null) {
		    if(!test(' ')) {
			val id: Int = stack.push(symbol)
			var content: MarkupText = parseMarkup(stack = stack)
			if(stack.has(id)) {
			    stack.popUntil(id)
			    root.add(Text(symbol.toString(), skipSpace = true))
			}
			root.add(content)
		    } else {
			root.add(Text(symbol.toString()))
		    }
		}

	    } else {
		var symbol: Char? = null

		for(s in markup_symbols) {
		    if(test(s)) {
			symbol = s
			break
		    }
		}

		if(symbol != null) {
		    if(src.getChar() == ' ' || src.getChar() == '\n' || src.isEof() || src.getChar() in markup_symbols) {
			if(stack.popIfHas(symbol)) {
			    if(!word.isEmpty()) root.add(Text(word))
			    return getMarkup(symbol, root)
			} else {
			    root.add(Text(word + symbol, skipSpace = true))
			    word = ""
			}
		    } else {
			word += symbol
		    }
		} else {
		    word += src.getChar()
		    src.nextChar()
		}
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
	    for(i in 1..level-1) prefix += '*'
	    parseMarkupWithSymbol('*', prefix)
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

	    if(!test('.')) return parseMarkup(prefix = bullet)
	    bullet += "."
	}
	
	
	if(!test(' ')) {
	    if(firstChar in markup_symbols) {
		return parseMarkupWithSymbol(firstChar)
	    }

	    return parseMarkup(root = MarkupText(listOf(Text(firstChar.toString()))))
	}

	return ListEntry(parseMarkup(), bullet, indent)

    }

    data class ListResult(var entry: Org, var indent: Int, var list: OrgList, var emptyLines: Int)
    
    /* FIXME i am ugly */
    fun parseList(entryVal: ListEntry): ListResult {

	var entry: ListEntry = entryVal
	var list: OrgList = OrgList(emptyList())
	var lines: List<MarkupText> = emptyList()

	var skip: Boolean = false
	var nextEntry: Org? = null
	var nextIndent: Int = 0

	var emptyLines: Int = 0
	
	while(!src.isEof()) {
	    
	    if(!skip) {
		nextIndent = skipWhitespaces()
		var firstChar: Char = src.getChar()
		if(testRange('0'..'9') || test('-') || test('+')) {
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
		    } else if(nextEntry is MarkupText) {
			emptyLines = 0
			lines += nextEntry
		    } else {
			throw ParserException("Unknown type when parsing list")
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
