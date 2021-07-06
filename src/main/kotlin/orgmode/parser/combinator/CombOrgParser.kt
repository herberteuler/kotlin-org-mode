package orgmode.parser.combinator

import orgmode.*

fun readAll(src: Source): String {

    var sb: StringBuilder = StringBuilder()

    while (!src.isEof()) {
        sb.append(src.getChar())
        src.nextChar()
    }

    return sb.toString()
}

class CombOrgParser(src: Source) : orgmode.parser.Parser<Org> {
    val s: String = readAll(src)

    val starsParser: Parser<Int> = CombParser {
        str { c -> c == '*' } * { s -> s.length }
    }

    val keywordParser: Parser<STATE> = CombParser {
        choice(
            exact("TODO") * { _ -> STATE.TODO },
            exact("FIXME") * { _ -> STATE.FIXME },
            exact("DONE") * { _ -> STATE.DONE },
            just(STATE.NONE)
        )
    }

    val priorityParser: Parser<Priority> = CombParser {
        (char('[') r char('#') r alphaNum l char(']')) * { c -> Priority(c) }
    }

    val tagParser: Parser<Tag> = CombParser {
        (char(':') r (str(choice(alphaNum, char('_'), char('@'), char('#'), char('%'))) % char(':')) l char(':')) * { l -> Tag(l) }
    }

    val headlineParser: Parser<Section> = CombParser {
        starsParser bind {
            level -> (ws r keywordParser) bind {
                state -> (ws r !priorityParser) bind {
                    priority -> (str(choice(alphaNum, singleWs))) bind {
                        text -> (!char('\n')) {
                            newLine ->
                                if (newLine == null) {
                                    (!tagParser) {
                                        tag ->
                                            just(Section(Text(text), level).also {
                                                     it.state = state
                                                     it.priority = priority
                                                     it.tag = tag
                                            }) l (ws r !char('\n'))
                                    }
                                } else {
                                    just(Section(Text(text), level).also {
                                             it.state = state
                                             it.priority = priority
                                    })
                                }
                        }
                    }
                }
            }
        }
    }

    override fun parse(): Org {
        var doc: Document = Document()

        return headlineParser.parse(s).second
    }
}
