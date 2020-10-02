package orgmode

enum class STATE {
    TODO {
        override fun getColor(): String = "orange"
    },
    FIXME {
        override fun getColor(): String = "red"
    },
    DONE {
        override fun getColor(): String = "green"
    },
    NONE {
        override fun getColor(): String = throw OrgException("Trying get color of NONE state")
    };
    fun toHtml(): String {
        return "<span style=\"color:${getColor()}\">${toString()}</span>"
    }

    abstract fun getColor(): String

}

enum class PLANNING_TYPE {
    DEADLINE, SCHEDULED, CLOSED
}

class Planning(var type: PLANNING_TYPE, var timestamp: String) {
    override fun equals(other: Any?): Boolean {
        if(other !is Planning) return false
        return type == other.type && timestamp == other.timestamp
    }
}

open class Section(text: MarkupText, level: Int, entities: List<Org> = emptyList(), var state: STATE = STATE.NONE) : Org(entities) {

    var level: Int = level
    var text: MarkupText = text
    var planning: Planning? = null

    fun plan(planning: Planning) {
        this.planning = planning
    }

    override fun toString(): String {
        var prefix: String = "\n"

        for (i in 1..level) {
            prefix += '*'
        }

        return "$prefix ${text.toString()}\n${super.toString()}"
    }

    override fun toJson(): String {
        var elements: String = ""

        for (i in entities.indices) {
            if (i != 0) elements += ", "
            elements += entities[i].toJson()
        }

        return "{ \"type\": \"section\", \"header\": ${text.toJson()}, \"level\": $level, \"elements\": [$elements] }"
    }

    override fun toHtml(): String {
        var innerHtml: String = super.toHtml()
        return "<h$level>${if(state != STATE.NONE) state.toHtml() + " " else ""}${text.toHtml()}</h$level>$innerHtml"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Section) return false
        if((planning == null) != (other.planning == null)) return false
        if(planning != null && !planning!!.equals(other.planning)) return false
        return state == other.state && other.text == text && other.level == level && super.equals(other)
    }
}

class Document(entities: List<Org> = emptyList()) : Section(Text(""), 0, entities) {

    override fun toString(): String {
        return entities.fold("") { acc, e -> acc + e.toString() }
    }

    override fun toJson(): String {
        var elements: String = ""

        for (i in entities.indices) {
            if (i != 0) elements += ", "
            elements += entities[i].toJson()
        }

        return "{ \"type\": \"document\", \"elements\": [$elements] }"
    }

    override fun toHtml(): String {
        var innerHtml: String = entities.fold("") { acc, e -> acc + e.toHtml() }
        return "<html><head></head><body>$innerHtml</body></html>"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Document) return false
        return super.equals(other)
    }
}
