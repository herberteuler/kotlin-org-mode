

package orgmode

class RegexOrgParser(src: Source) : AbstractParser<Org>(src) {

    val markupRegex: Regex = """((.*)((^|\s)((\*([^ ].*[^ ]|[^ ])\*)|(\+([^ ].*[^ ]|[^ ])\+)|(\_([^ ].*[^ ]|[^ ])\_)|(\=([^ ].*[^ ]|[^ ])\=)|(\/([^ ].*[^ ]|[^ ])\/)|(\[\[([^\]]*)\](\[(.*)\])?\]))(\s|$))(.*)|(.*))(\n)?""".toRegex()
    //                          (1(2)(3(4  )(5(6(7 empahsis     )  ) (8 (9 strikeout    )  ) (10(11 underline   )  ) (12(13 code        )  ) (14(15 italic      )  ) (16  (17   )   (18(19)  )   ))(20  ))(21) (22))(23)

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

    val meaningfulGroups: List<Int> = listOf(7, 9, 11, 13, 15, 17, 22)

    fun getMatchedMarkup(markup: MarkupText, match: MatchResult): List<MarkupText> {
        var res: List<MarkupText> = listOf()
        with(match) {
            if(groups[2]!!.value != "") {
                res += Text(groups[2]!!.value)
            }
            res += markup
            if(groups[21]!!.value != "") {
                res += parseMarkup(groups[21]!!.value)
            }
            if(groups[23] != null) {
                res += LineBreak()
            }
        }
        return res
    }

    fun groupToMarkup(groupId: Int, match: MatchResult): List<MarkupText> {
        with(match) {
            return when(groupId) {
                7 -> getMatchedMarkup(Emphasis(parseMarkup(groups[groupId]!!.value)), match)
                9 -> getMatchedMarkup(Strikeout(parseMarkup(groups[groupId]!!.value)), match)
                11 -> getMatchedMarkup(Underline(parseMarkup(groups[groupId]!!.value)), match)
                13 -> getMatchedMarkup(Code(groups[groupId]!!.value), match)
                15 -> getMatchedMarkup(Italic(parseMarkup(groups[groupId]!!.value)), match)
                17 -> getMatchedMarkup(if(groups[18] != null) Link(groups[groupId]!!.value, parseMarkup(groups[19]!!.value)) else Link(groups[groupId]!!.value), match)
                22 -> if(groups[groupId]!!.value != "") listOf(Text(groups[groupId]!!.value)) + (if(groups[23] != null) listOf(LineBreak()) else listOf()) else listOf()
                else -> throw ParserException("Unknown group id passed to groupToMarkup")
            }
        }
    }

    fun parseMarkup(s: String): List<MarkupText> {

        val match: MatchResult? = markupRegex.matchEntire(s)

        if(match != null) {
            for(groupId in meaningfulGroups) {
                if(match.groups[groupId] != null) {
                    return groupToMarkup(groupId, match)
                }
            }
        } else {
            throw ParserException("Cant match markup string")
        }

        throw ParserException("Not found any matched group")

    }

}
