
package orgmode

import java.io.File

interface Source {
    fun isEof(): Boolean
    fun getChar(): Char
    fun nextChar(): Unit
}

open class StringSource : Source {

    private var src: String
    private var index: Int
    
    constructor(src: String) {
	this.src = src
	this.index = 0
    }

    override fun isEof(): Boolean {
	return index >= src.length
    }
    
    override fun getChar(): Char {
	return if (!isEof()) src[index] else '\u0000'
    }

    override fun nextChar(): Unit {
	index++
    }

}

class FileSource(file: String) : StringSource(File(file).readText())
