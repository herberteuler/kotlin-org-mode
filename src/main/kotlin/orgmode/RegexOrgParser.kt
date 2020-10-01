

package orgmode

class RegexOrgParser(src: Source) : AbstractParser<Org>(src) {

    val linkRegex: Regex = """(.*)(\[\[([^\]]+)\](\[(.*)\])?\])(.*(\n)?)""".toRegex()
    val emphasisRegex: Regex = """(.*)(^|\s)(\*([^ ].+[^ ]|[^ ])\*)(\s|$)(.*(\n)?)""".toRegex()
    val strikeoutRegex: Regex = """(.*)(^|\s)(\+([^ ].+[^ ]|[^ ])\+)(\s|$)(.*(\n)?)""".toRegex()
    val underlineRegex: Regex = """(.*)(^|\s)(\_([^ ].+[^ ]|[^ ])\_)(\s|$)(.*(\n)?)""".toRegex()
    val codeRegex: Regex = """(.*)(^|\s)(\=([^ ].+[^ ]|[^ ])\=)(\s|$)(.*(\n)?)""".toRegex()
    val italicRegex: Regex = """(.*)(^|\s)(\/([^ ].+[^ ]|[^ ])\/)(\s|$)(.*(\n)?)""".toRegex()
    val textRegex: Regex = """(.*)(\n)?""".toRegex()

    fun parseNextMarkup(head: String, markup: MarkupText, rest: String): List<MarkupText> {
        var res: List<MarkupText> = listOf()

        if(head != "") {
            res += parseMarkup(head)
        }
        res += markup
        if(rest != "") {
            res += parseMarkup(rest)
        }
        return res
    }

    fun generalMarkup(ctor: (List<MarkupText>, MarkupText?) -> MarkupText): (MatchResult) -> List<MarkupText> {
        return {
            match ->
                var res: List<MarkupText> = listOf()
                if(match.groups[1] != null && match.groups[1]!!.value != "") {
                    res += parseMarkup(match.groups[1]!!.value)
                }
                res += ctor(parseMarkup(match.groups[4]!!.value), null)
                if(match.groups[6] != null && match.groups[6]!!.value != "") {
                    res += parseMarkup(match.groups[6]!!.value)
                }
                res
        }
    }

    val regexToMarkup: Map<Regex, (MatchResult) -> List<MarkupText>> = mapOf(
        linkRegex to {
            match ->
                var res: List<MarkupText> = listOf()
            if(match.groups[1]!!.value != "") {
                res += parseMarkup(match.groups[1]!!.value)
            }
            if(match.groups[5] != null) {
                res += Link(match.groups[3]!!.value, parseMarkup(match.groups[5]!!.value))
            } else {
                res += Link(match.groups[3]!!.value)
            }
            if(match.groups[6]!!.value != "") {
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
                if(match.groups[1] != null && match.groups[1]!!.value != "") {
                    res += parseMarkup(match.groups[1]!!.value)
                }
                res += Code(match.groups[4]!!.value)
                if(match.groups[6] != null && match.groups[6]!!.value != "") {
                    res += parseMarkup(match.groups[6]!!.value)
                }
                res
        },
        textRegex to {
            match ->
                var res: List<MarkupText> = listOf()
                if(match.groups[1]!!.value != "") {
                    res += Text(match.groups[1]!!.value)
                }
                if(match.groups[2] != null) {
                    res += LineBreak()
                }
                res
        }
    )


    override fun parse(): Org {

        var temp: MarkupText = Paragraph()

        while(!src.isEof()) {
            val parsedMarkup: MarkupText = MarkupText(parseMarkup(getLine()))
            if(!parsedMarkup.isEmpty()) {
                temp.add(parsedMarkup)
            }
        }

        return Document(listOf(temp))
    }


    fun getLine(): String {
        var res: String = ""

        while(!test('\n') && !src.isEof()) {
            if(test('\\')) {
                if(test('\\')) {
                    if(test('\n')) {
                        if(res == "") break
                        res += "\n"
                        break
                    }
                    res += '\\'
                } else {
                    res += src.getChar()
                    src.nextChar()
                    continue
                }
            }
            res += src.getChar()
            src.nextChar()
        }

        return res
    }

    fun parseMarkup(s: String): List<MarkupText> {

        println("Parsing string ${s}")

        for((regex, getMarkup) in regexToMarkup) {
            var match: MatchResult? = regex.matchEntire(s)
            if(match != null) {
                println("Found match ${regex.toString()}")
                return getMarkup(match)
            }
                println("Trying regex ${regex.toString()}")
        }

        throw ParserException("Not found any matched group")

    }

}
