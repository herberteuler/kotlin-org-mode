
package orgmode

class OrgParser(src: Source) : AbstractParser<Org>(src) {


    override fun parse(): Org {
	var root: Org = Document()
	parseSection(root)
	return root
    }
    
    fun parseSection(root: Org): Org? {

	var lines: Array<Org> = emptyArray()
	val level: Int = if (root is Document) 0 else (root as Section).level
	
	while(!src.isEof()) {

	    var element = parseLine()

	    if(element is Section) {
		root.add(Paragraph(level, lines))
		lines = emptyArray()
		if(root is Document || element.level > (root as Section).level) {
		    var section = parseSection(element)
		    while(true) {
			root.add(element)
			
			if(section != null && section is Section) {
			    if(root is Section && section.level <= root.level) {
				return section
			    } else {
				element = section
				section = parseSection(element)
			    }
			} else return null
		    }
		} else return element
	    } else {
		lines += element
	    }
	    
	}
	if(!lines.isEmpty()) root.add(Paragraph(level, lines))
	return null

    }

    fun parseLine(): Org {

	var buf: String = ""

	if(test('*')) {
	    return tryParseHeader();
	}
	
	while(!test('\n') && !src.isEof()) {
	    buf += src.getChar()
	    src.nextChar()
	}

	return Text(buf)
    }

    fun tryParseHeader(): Org {

	var level: Int = 1
	while(test('*')) level++

	return if(!test(' ')) {
	    var prefix: String = ""
	    for(i in 1..level) prefix += '*'
	    Text(prefix + parseLine())
	} else Section(parseLine().toString(), level)
    }
    
}
