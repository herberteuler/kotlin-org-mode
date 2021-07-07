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

    fun sectionTextParser(level: Int, state: STATE, priority: Priority?, prefix: String = ""): Parser<Section> {
         return CombParser {
             str { c -> c !in "\n:" } bind {
                 text ->
                     choice(
                         eofOrNl * {
                             Section(Text(prefix + text), level).also {
                                 it.state = state
                                 it.priority = priority
                             }
                         },
                         !(tagParser l ws l eofOrNl) bind {
                             tag ->
                                 if(tag != null) {
                                     just(Section(Text(prefix + text), level).also {
                                              it.state = state
                                              it.priority = priority
                                              it.tag = tag
                                     })
                                 } else {
                                     char(':') bind { _ -> sectionTextParser(level, state, priority, prefix + text + ":") }
                                 }
                         }
                     )
             }
         }
    }

    val headlineParser: Parser<Section> = CombParser {
        starsParser bind {
            level -> (ws r keywordParser) bind {
                state -> (ws r !priorityParser) bind {
                    priority -> sectionTextParser(level, state, priority)
                }
            }
        }
    }

    override fun parse(): Org {
        var doc: Document = Document()

        return headlineParser.parse(s).second
    }
}
