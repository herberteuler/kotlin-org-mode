
package orgmode

class OrgParser(src: Source) : AbstractParser<Org>(src) {


    override fun parse(): Org {
	var root: Section = Document()
	parseSection(root)
	return root
    }
    
    fun parseSection(root: Section): Section? {

	var lines: Array<Org> = emptyArray()
	
	while(!src.isEof()) {

	    var element = parseLine()

	    if(element is Section) {
		if (!lines.isEmpty()) root.add(Paragraph(root.level, lines))

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
	    } else {
		lines += element
	    }
	    
	}
	if(!lines.isEmpty()) root.add(Paragraph(root.level, lines))
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
