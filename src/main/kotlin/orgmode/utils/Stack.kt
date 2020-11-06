package orgmode.utils

data class StackElement<T>(val prev: StackElement<T>?, var value: T)

open class Stack<T>() {
    var root: StackElement<T>? = null

    fun push(value: T) {
        if(root == null) {
            root = StackElement(null, value)
        } else {
            root = StackElement(root, value)
        }
    }

    open fun pop(): T {
        root ?: throw Exception("Stack is empty")
        val res: T = root!!.value
        root = root!!.prev
        return res
    }

    open fun top(): T {
        root ?: throw Exception("Stack is empty")
        return root!!.value
    }

    fun isEmpty(): Boolean {
        return root == null
    }

    fun clear() {
        root = null
    }
}


class MarkupStack(markup_symbols: List<Char>) : Stack<Pair<Char, Int>>() {
    var cnt: Int = 0
    val count_of: Map<Char, Int> = markup_symbols.map { s -> s to 0 }.toMap()

    override fun pop(): Pair<Char, Int> {
        if(isEmpty()) return Pair('\u0000', -1)
        return super.pop()
    }
    override fun top(): Pair<Char, Int> {
        if(isEmpty()) return Pair('\u0000', -1)
        return super.top()
    }

    fun push(c: Char): Int {
        cnt++
        push(Pair(c, cnt - 1))
        return cnt - 1
    }
    fun has(c: Char): Boolean {
        var i: StackElement<Pair<Char, Int>>? = root
        while(i != null) {
            if(i.value.first == c) return true
            i = i.prev
        }
        return false
    }
    fun has(id: Int): Boolean {
        var i: StackElement<Pair<Char, Int>>? = root
        while(i != null) {
            if(i.value.second == id) return true
            i = i.prev
        }
        return false
    }

    fun popUntil(c: Char): Boolean {
        if(!has(c)) return false;
        while(root!!.value.first != c) {
            pop()
        }
        return true
    }

    fun popUntil(id: Int): Boolean {
        if(!has(id)) return false;
        while(root!!.value.second != id) {
            pop()
        }
        return true
    }
}
