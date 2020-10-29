package orgmode


class OrgTable(var lines: List<OrgTableLine>) : Paragraph(emptyList()) {

    override fun toString(): String = "NOT IMPLEMENTED"
    override fun toHtml(): String = """<table>${lines.fold("") {acc, e -> acc + e.toHtml()}}</table>"""
    override fun toJson(): String = "NOT IMPLEMENTED"

    override fun isEmpty(): Boolean = lines.all { it.isEmpty() }

    override fun add(other: MarkupText): MarkupText = throw OrgException("Cannot add Markup element to Table")
    override fun add(element: Org): MarkupText = throw OrgException("Cannot add Markup element to Table")

    fun add(line: OrgTableLine) {
        lines += line
    }
}

open class OrgTableLine(var cols: List<MarkupText>) : MarkupText(emptyList()) {

    override fun toString(): String = "NOT IMPLEMENTED"
    override fun toHtml(): String = """<tr>${cols.fold("") {acc, e -> acc + "<td>" + e.toHtml() + "</td>"}}</tr>"""
    override fun toJson(): String = "NOT IMPLEMENTED"

    override fun add(other: MarkupText): MarkupText {
        cols += other
        return this
    }

    override fun add(element: Org): MarkupText = throw OrgException("Cannot add Org element to Table line")
    override fun isEmpty(): Boolean = false
}

class OrgTableSplit(): OrgTableLine(emptyList()) {

    override fun toHtml(): String = ""

    override fun isEmpty(): Boolean = true
}
