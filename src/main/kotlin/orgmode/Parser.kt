
package orgmode

interface Parser<T> {
    fun parse(): T
}

abstract class AbstractParser<T>(src: Source) : Parser<T> {

    val src: Source = src

    public fun test(c: Char): Boolean {
	if(c == src.getChar()) {
	    src.nextChar()
	    return true
	} else {
	    return false
	}
    }

    public fun expect(c: Char): Unit {
	if(!test(c)) throw ParserException("Expected " + c + ", but " + src.getChar() + " met")
    }

    public fun expect(s: String): Unit {
	for(c in s) expect(c)
    }
}
