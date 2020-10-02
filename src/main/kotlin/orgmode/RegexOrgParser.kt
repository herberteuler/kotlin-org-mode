

package orgmode

class RegexOrgParser(src: Source) : AbstractParser<Org>(src) {

    val linkRegex:       Regex = """(.*)(\[\[([^\]]+)\](\[(.*)\])?\])(.*(\n)?)""".toRegex()
    val emphasisRegex:   Regex = """(.*)(^|\s)(\*([^ ].+[^ ]|[^ ])\*)(\s|$)(.*(\n)?)""".toRegex()
    val strikeoutRegex:  Regex = """(.*)(^|\s)(\+([^ ].+[^ ]|[^ ])\+)(\s|$)(.*(\n)?)""".toRegex()
    val underlineRegex:  Regex = """(.*)(^|\s)(\_([^ ].+[^ ]|[^ ])\_)(\s|$)(.*(\n)?)""".toRegex()
    val codeRegex:       Regex = """(.*)(^|\s)(\=([^ ].+[^ ]|[^ ])\=)(\s|$)(.*(\n)?)""".toRegex()
    val italicRegex:     Regex = """(.*)(^|\s)(\/([^ ].+[^ ]|[^ ])\/)(\s|$)(.*(\n)?)""".toRegex()
    val textRegex:       Regex = """(.*)(\n)?""".toRegex()
    val sectionRegex:    Regex = """^(\*+) (.+(\n)?)""".toRegex()
    val listRegex:       Regex = """^(\s*)(\+|-|[0-9]+[\.\)]) (.+(\n)?)""".toRegex()
    val blockBeginRegex: Regex = """^(\s*)#\+BEGIN_(SRC)(.*(\n)?)?""".toRegex(RegexOption.IGNORE_CASE)
    val blockEndRegex:   Regex = """^(\s*)#\+END_(SRC)(\n|$)""".toRegex(RegexOption.IGNORE_CASE)
    val planningRegex:   Regex = """^(DEADLINE|SCHEDULED|CLOSED): (.+)(\n)?""".toRegex()

    var buffer: String? = null

    fun getIndent(line: String): Int {
        var i: Int = 0

        while(i < line.length && line[i].isWhitespace()) {
            i++
        }
        return i
    }

    override fun parse(): Org {

        var doc: Document = Document()

        parseSection(doc)

        return doc
    }

    fun parseSection(section: Section): Section? {
        var paragraph: Paragraph = Paragraph()
        var skip: Boolean = false
        var line: Org? = null
        var indent: Int? = null
        var rawLine: String = ""

        rawLine = getLine()
        var planning = planningRegex.matchEntire(rawLine)
        if(planning != null) {
            section.plan(Planning(when(planning.groups[1]!!.value) {
                             "DEADLINE"  -> PLANNING_TYPE.DEADLINE
                             "SCHEDULED" -> PLANNING_TYPE.SCHEDULED
                             "CLOSED"    -> PLANNING_TYPE.CLOSED
                             else -> throw ParserException("Unknown planning type")
            }, planning.groups[2]!!.value))
        } else {
            skip = true
            indent = getIndent(rawLine)
            line = parseLine(rawLine)
        }

        while (skip || !src.isEof()) {
            if (!skip) {
                rawLine = getLine()
                indent = getIndent(rawLine)
                line = parseLine(rawLine)
            }
            skip = false

            indent ?: throw ParserException("Wrong skip")
            line ?: throw ParserException("Wrong skip")

            if (line is MarkupText) {
                if (!line.isEmpty()) {
                    paragraph.add(line)
                } else if (!paragraph.isEmpty()) {
                    section.add(paragraph)
                    paragraph = Paragraph()
                }
            } else if (line is Section) {
                if (!paragraph.isEmpty()) section.add(paragraph)
                if (line.level <= section.level) {
                    return line
                } else {
                    var nextSection: Section? = line
                    while (nextSection != null && nextSection.level > section.level) {
                        var tempSection: Section? = parseSection(nextSection)
                        section.add(nextSection)
                        nextSection = tempSection
                    }
                    return nextSection
                }
            } else if (line is ListEntry) {
                if (!paragraph.isEmpty()) section.add(paragraph)
                paragraph = Paragraph()
                var list = OrgList()
                val (newLine, newIndent) = parseList(list, line, indent)
                section.add(list)
                if (newLine == null) {
                    continue
                }
                line = newLine
                indent = newIndent
                skip = true
            } else if(line is Block) {
                if (!paragraph.isEmpty()) section.add(paragraph)
                paragraph = Paragraph()
                parseBlock(line)
                section.add(line)
            }
        }
        if (!paragraph.isEmpty()) {
            section.add(paragraph)
        }
        return null
    }

    fun parseBlock(block: Block) {
        while(true) {
            var codeLine = getLine()
            if(blockEndRegex.matches(codeLine)) {
                return
            }
            block.add(codeLine)
            if(src.isEof()) throw ParserException("Code block without end")
        }
    }

    fun parseList(list: OrgList, firstEntry: ListEntry, curIndent: Int): Pair<Org?, Int> {

        var paragraph: Paragraph = Paragraph()
        var entry = firstEntry
        var skip: Boolean = false
        var line: Org? = null
        var indent: Int? = null
        var emptyLines: Int = 0

        while (!src.isEof()) {

            if (!skip) {
                indent = skipWhitespaces()
                line = parseLine(" ".repeat(indent) + getLine())
            }

            skip = false

            indent ?: throw ParserException("Wrong skip")
            line ?: throw ParserException("Wrong skip")

            if (line is Section) {
                if (!paragraph.isEmpty()) entry.add(paragraph)
                list.add(entry)
                return Pair(line, 0)
            } else if (line is ListEntry) {
                if (!paragraph.isEmpty()) entry.add(paragraph)
                paragraph = Paragraph()
                if (indent < curIndent) {
                    list.add(entry)
                    return Pair(line, indent)
                } else if (indent == curIndent) {
                    list.add(entry)
                    entry = line
                } else {
                    var newList = OrgList()
                    val (nextLine, newIndent) = parseList(newList, line, indent)
                    entry.add(newList)
                    if (nextLine == null) {
                        list.add(entry)
                        return Pair(null, 0)
                    }
                    line = nextLine
                    indent = newIndent
                    skip = true
                }
            } else if (line is MarkupText) {
                if (line.isEmpty()) {
                    if (!paragraph.isEmpty()) entry.add(paragraph)
                    paragraph = Paragraph()
                    emptyLines++
                    if (emptyLines >= 2) {
                        list.add(entry)
                        return Pair(null, 0)
                    }
                    continue
                }
                emptyLines = 0
                if (indent <= curIndent) {
                    if (!paragraph.isEmpty()) entry.add(paragraph)
                    list.add(entry)
                    return Pair(line, indent)
                } else {
                    paragraph.add(line)
                }
            } else if(line is Block) {
                if (!paragraph.isEmpty()) entry.add(paragraph)
                paragraph = Paragraph()
                parseBlock(line)
                entry.add(line)
            }
        }
        if (!paragraph.isEmpty()) entry.add(paragraph)
        list.add(entry)

        return Pair(null, 0)
    }

    fun parseLine(line: String): Org {
        var match: MatchResult? = sectionRegex.matchEntire(line)
        if (match != null) {
            return Section(MarkupText(parseMarkup(match.groups[2]!!.value)), match.groups[1]!!.value.length)
        }
        match = listRegex.matchEntire(line)
        if (match != null) {
            return ListEntry(MarkupText(parseMarkup(match.groups[3]?.value ?: "")), match.groups[2]!!.value, match.groups[1]?.value?.length ?: 0)
        }
        match = blockBeginRegex.matchEntire(line)
        if(match != null) {
            if(match.groups[2]!!.value == "SRC") {
                return CodeBlock();
            }
        }

        return MarkupText(parseMarkup(line))
    }

    fun parseNextMarkup(head: String, markup: MarkupText, rest: String): List<MarkupText> {
        var res: List<MarkupText> = listOf()

        if (head != "") {
            res += parseMarkup(head)
        }
        res += markup
        if (rest != "") {
            res += parseMarkup(rest)
        }
        return res
    }

    fun generalMarkup(ctor: (List<MarkupText>, MarkupText?) -> MarkupText): (MatchResult) -> List<MarkupText> {
        return {
            match ->
            var res: List<MarkupText> = listOf()
            if (match.groups[1] != null && match.groups[1]!!.value != "") {
                res += parseMarkup(match.groups[1]!!.value)
            }
            res += ctor(parseMarkup(match.groups[4]!!.value), null)
            if (match.groups[6] != null && match.groups[6]!!.value != "") {
                res += parseMarkup(match.groups[6]!!.value)
            }
            res
        }
    }

    val regexToMarkup: Map<Regex, (MatchResult) -> List<MarkupText>> = mapOf(
        linkRegex to {
            match ->
            var res: List<MarkupText> = listOf()
            if (match.groups[1]!!.value != "") {
                res += parseMarkup(match.groups[1]!!.value)
            }
            if (match.groups[5] != null) {
                res += Link(match.groups[3]!!.value, parseMarkup(match.groups[5]!!.value))
            } else {
                res += Link(match.groups[3]!!.value)
            }
            if (match.groups[6]!!.value != "") {
                res += parseMarkup(match.groups[6]!!.value)
            }
            res
        },
        italicRegex to generalMarkup(::Italic),
        emphasisRegex to generalMarkup(::Emphasis),
        strikeoutRegex to generalMarkup(::Strikeout),
        underlineRegex to generalMarkup(::Underline),
        codeRegex to {
            match ->
            var res: List<MarkupText> = listOf()
            if (match.groups[1] != null && match.groups[1]!!.value != "") {
                res += parseMarkup(match.groups[1]!!.value)
            }
            res += Code(match.groups[4]!!.value)
            if (match.groups[6] != null && match.groups[6]!!.value != "") {
                res += parseMarkup(match.groups[6]!!.value)
            }
            res
        },
        textRegex to {
            match ->
            var res: List<MarkupText> = listOf()
            if (match.groups[1]!!.value != "") {
                res += Text(match.groups[1]!!.value)
            }
            if (match.groups[2] != null) {
                res += LineBreak()
            }
            res
        }
    )

    fun getLine(): String {
        var res: StringBuilder = StringBuilder()

        while (!test('\n') && !src.isEof()) {
            if (test('\\')) {
                if (test('\\')) {
                    if (test('\n')) {
                        if (res.isEmpty()) break
                        res.append("\n")
                        break
                    }
                    res.append("\\")
                } else {
                    res.append(src.getChar())
                    src.nextChar()
                    continue
                }
            }
            res.append(src.getChar())
            src.nextChar()
        }

        return res.toString()
    }

    fun parseMarkup(s: String): List<MarkupText> {

        for ((regex, getMarkup) in regexToMarkup) {
            var match: MatchResult? = regex.matchEntire(s)
            if (match != null) {
                return getMarkup(match)
            }
        }

        throw ParserException("Not found any matched group")
    }
}
