package orgmode.parser.combinator

import kotlin.reflect.KProperty

class ParserError(public val exp: String, public val was: String, public var rest: String) : Exception("Expected: ${exp}, was: ${was}. The rest: \"${rest}\"") { }

interface Parser<T> {
    fun parse(s: String): Pair<String, T>
}

open class LeftParser<T, E>(val left: Parser<T>, val right: Parser<E>) : Parser<T> {
    override  fun parse(s: String): Pair<String, T> {
        var (rest, f) = left.parse(s)
        try {
            var (rest, _) = right.parse(rest)
            return Pair(rest, f)
        } catch(e: ParserError) {
            throw ParserError(e.exp, e.was, s)
        }

    }
}

class NoneParser<T>(val res: T) : Parser<T> {
    override fun parse(s: String): Pair<String, T> {
        return Pair(s, res)
    }
}


class MaybeParser<T>(val p: Parser<T>): Parser<T?> {
    override fun parse(s: String): Pair<String, T?> {
        try {
            return p.parse(s);
        } catch(e: ParserError) {
            return Pair(s, null)
        }
    }
}

open class RightParser<T, E>(val left: Parser<T>, val right: Parser<E>) : Parser<E> {
    override fun parse(s: String): Pair<String, E> {
        var (rest, _) = left.parse(s)
        try {
            var (rest, f) = right.parse(rest)
            return Pair(rest, f)
        } catch(e: ParserError) {
            throw ParserError(e.exp, e.was, s)
        }

    }
}

open class MapParser<T, E>(val p: Parser<T>, val f: (T) -> E) : Parser<E> {
    override fun parse(s: String): Pair<String, E> {
        val (rest, r) = p.parse(s)
        return Pair(rest, f(r))
    }
}

open class AnyParser : Parser<Char> {
    override fun parse(s: String): Pair<String, Char> {
        if (s.length == 0) throw ParserError("any character", "end of input", s)
        val c: Char = s.get(0)
        return Pair(s.substring(1), c)
    }
}

open class EofParser : Parser<Unit> {
    override fun parse(s: String): Pair<String, Unit> {
        if (s == "") return Pair("", Unit)
        else throw ParserError("end of input", "not empty", s);
    }
}

open class ChoiceParser<T>(val parsers: List<Parser<T>>) : Parser<T> {
    override fun parse(s: String): Pair<String, T> {
        // FIXME It's very slow, I think
        var exps: MutableList<String> = mutableListOf();
        var wass: MutableList<String> = mutableListOf();

        for(p: Parser<T> in parsers) {
            try {
                return p.parse(s)
            } catch(e: ParserError) {
                exps.add(e.exp)
                wass.add(e.was)
            }
        }
        throw ParserError(exps.joinToString(), wass.joinToString(), s)
    }
}

open class SatisfyParser(val descr: String, val pred: (Char) -> Boolean) : Parser<Char> {
    override fun parse(s: String): Pair<String, Char> {
        val (rest, c) = AnyParser().parse(s)
        if (pred(c)) {
            return Pair(rest, c)
        }
        throw ParserError(descr, "'${c}'", s)
    }
}

open class ManyParser<T>(val p: Parser<T>) : Parser<List<T>> {
    override fun parse(s: String): Pair<String, List<T>> {
        var res: MutableList<T> = mutableListOf()
        var s = s
        try {
            while(true) {
                val (rest, r) = p.parse(s)
                s = rest
                res.add(r)
            }
        } catch(e: ParserError) {
            return Pair(e.rest, res)
        }
    }
}

open class Many1Parser<T>(val p: Parser<T>) : Parser<List<T>> {
    override fun parse(s: String): Pair<String, List<T>> {
        val res = ManyParser<T>(p).parse(s)
        if(res.second.size == 0) {
            throw ParserError("at least one", "none", s)
        }
        return res
    }
}

open class SeparatedByParser<T, E>(val p: Parser<T>, val sep: Parser<E>) : Parser<List<T>> {
    override fun parse(s: String): Pair<String, List<T>> {
        try {
            val (rest1, first) = p.parse(s)
            val (rest, lst) = ManyParser(RightParser(sep, p)).parse(rest1)
            val res = mutableListOf(first)
            res.addAll(lst)
            return Pair(rest, res)
        } catch(e: ParserError) {
            return Pair(s, listOf())
        }
    }
}

class SeparatedBy1Parser<T, E>(val p: Parser<T>, val sep: Parser<E>) : Parser<List<T>> {
    override fun parse(s: String): Pair<String, List<T>> {
        val res = SeparatedByParser(p, sep).parse(s)
        if(res.second.size == 0) throw ParserError("at least one", "none", s)
        return res
    }
}

class WhitespaceParser : Parser<Char> {
    override fun parse(s: String): Pair<String, Char> {
        return SatisfyParser("whitespace", {c -> c in " \t\r"}).parse(s)
    }
}

class BindParser<T, E>(val p: Parser<T>, val f: (T) -> Parser<E>): Parser<E> {
    override fun parse(s: String): Pair<String, E> {
        val (rest, r) = p.parse(s)
        return f(r).parse(rest)
    }
}

class MatchParser<T>(val p: Parser<T>): Parser<Boolean> {
    override fun parse(s: String): Pair<String, Boolean> {
        try {
            p.parse(s)
            return Pair(s, true)
        } catch(e: ParserError) {
            return Pair(s, false)
        }
    }
}


class CombParser<T>(val builder: CombParser<T>.() -> Parser<T>) : Parser<T> {

    val singleWs: Parser<Char> = WhitespaceParser()
    val ws: Parser<String> = MapParser(ManyParser(singleWs), { l -> l.joinToString("") })
    val alphaNum: Parser<Char> = SatisfyParser("alpha numeric") { c -> c.isLetterOrDigit() }

    override fun parse(s: String): Pair<String, T> {
        return builder(this).parse(s)
    }
    fun str(pred: (Char) -> Boolean): Parser<String> {
        return MapParser(Many1Parser(SatisfyParser("chars", pred)), { l -> l.joinToString("") })
    }
    fun str(p: Parser<Char>): Parser<String> {
        return MapParser(Many1Parser(p), { l -> l.joinToString("") })
    }
    fun char(c: Char): Parser<Char> {
        return SatisfyParser("'$c'") { cs -> cs == c }
    }

    fun exact(s: String): Parser<String> {
        var p: Parser<String>? = null
        return CombParser<String> {
            for (c: Char in s) {
                if (p == null) {
                    p = char(c) * { c -> "" + c }
                } else {
                    p = p!! { sn -> char(c) * { c -> sn + c } }
                }
            }
            p!!
        }
    }

    fun <E> just(res: E): Parser<E> {
        return NoneParser(res)
    }
    fun <E> ret(res: E): Parser<E> {
        return just(res)
    }

    fun <E> choice(vararg ps: Parser<E>): Parser<E> {
        return ChoiceParser(ps.asList())
    }

    operator fun <E> Parser<E>.unaryPlus(): Parser<List<E>> {
        return Many1Parser(this)
    }
    operator fun <E> Parser<E>.unaryMinus(): Parser<List<E>> {
        return ManyParser(this)
    }
    operator fun <L, R> Parser<L>.plus(p: Parser<R>): Parser<L> {
        return LeftParser(this, p)
    }
    operator fun <L, R> Parser<L>.minus(p: Parser<R>): Parser<R> {
        return RightParser(this, p)
    }
    operator fun <L, R> Parser<L>.div(p: Parser<R>): Parser<List<L>> {
        return SeparatedBy1Parser(this, p)
    }
    operator fun <L, R> Parser<L>.rem(p: Parser<R>): Parser<List<L>> {
        return SeparatedByParser(this, p)
    }
    operator fun <E, R> Parser<E>.times(f: (E) -> R): Parser<R> {
        return MapParser(this, f)
    }
    operator fun <E, R> Parser<E>.invoke(f: (E) -> Parser<R>): Parser<R> {
        return BindParser(this, f)
    }
    infix fun <E, R> Parser<E>.bind(f: (E) -> Parser<R>): Parser<R> {
        return this(f)
    }
    operator fun <E> Parser<E>.not(): Parser<E?> {
        return MaybeParser(this)
    }

    infix fun <L, R> Parser<L>.l(p: Parser<R>): Parser<L> {
        return LeftParser(this, p)
    }
    infix fun <L, R> Parser<L>.r(p: Parser<R>): Parser<R> {
        return RightParser(this, p)
    }
}
