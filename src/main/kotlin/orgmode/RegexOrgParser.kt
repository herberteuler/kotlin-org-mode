

package orgmode

class RegexOrgParser(src: Source) : AbstractParser<Org>(src) {

    val markupRegex: Regex = """((.*)((^|\s)((\*([^ ].*[^ ]|[^ ])\*)|(\+([^ ].*[^ ]|[^ ])\+)|(\_([^ ].*[^ ]|[^ ])\_)|(\=([^ ].*[^ ]|[^ ])\=)|(\/([^ ].*[^ ]|[^ ])\/))(\s|$))(.*)|(.*))(\n)?""".toRegex()
    //                          (1(2)(3(4  )(5(6(7 empahsis     )  ) (8 (9 strikeout    )  ) (10(11 underline   )  ) (12(13 code        )  ) (14(15 italic      )  ))(16  ))(17) (18))(19)

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

    val meaningfulGroups: List<Int> = listOf(7, 9, 11, 13, 15, 18)

    fun getMatchedMarkup(markup: MarkupText, match: MatchResult): List<MarkupText> {
        var res: List<MarkupText> = listOf()
        with(match) {
            if(groups[2]!!.value != "") {
                res += Text(groups[2]!!.value)
            }
            res += markup
            if(groups[17]!!.value != "") {
                res += parseMarkup(groups[17]!!.value)
            }
            if(groups[19] != null) {
                res += LineBreak()
            }
        }
        return res
    }

    fun groupToMarkup(groupId: Int, match: MatchResult): List<MarkupText> {
        with(match) {
            return when(groupId) {
                7 -> getMatchedMarkup(Emphasis(parseMarkup(groups[7]!!.value)), match)
                9 -> getMatchedMarkup(Strikeout(parseMarkup(groups[9]!!.value)), match)
                11 -> getMatchedMarkup(Underline(parseMarkup(groups[11]!!.value)), match)
                13 -> getMatchedMarkup(Code(groups[13]!!.value), match)
                15 -> getMatchedMarkup(Italic(parseMarkup(groups[15]!!.value)), match)
                18 -> if(groups[18]!!.value != "") listOf(Text(groups[groupId]!!.value)) + (if(groups[19] != null) listOf(LineBreak()) else listOf()) else listOf()
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
