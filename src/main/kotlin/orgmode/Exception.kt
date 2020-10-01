
package orgmode

class ParserException(msg: String) : Exception(msg) {

    val msg: String = msg
}

class OrgException(msg: String) : Exception(msg) {

    val msg: String = msg
}
