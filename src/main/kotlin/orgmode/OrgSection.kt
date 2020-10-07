
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
        return """<code style="color:${getColor()}">${toString()}</code>"""
    }

    abstract fun getColor(): String

}

enum class PLANNING_TYPE {
    DEADLINE {
        override fun getHtmlColor(): String = "red"
    },
    SCHEDULED{
        override fun getHtmlColor(): String = "orange"
    },
    CLOSED{
        override fun getHtmlColor(): String = "gray"
    };

    abstract fun getHtmlColor(): String
}

class Planning(var type: PLANNING_TYPE, var timestamp: Timestamp) {

    fun toJson(): String = """{ "type": "$type", "timestamp": ${timestamp.toJson()}}"""
    fun toHtml(): String = """<code style="background:${type.getHtmlColor()}">$timestamp</code>"""

    override fun equals(other: Any?): Boolean {
        if(other !is Planning) return false
        return type == other.type && timestamp == other.timestamp
    }
}

class Property(var name: String, var plus: Boolean, var value: String?)

class Timestamp(var active: Boolean,
                var year: Int,
                var month: Int,
                var day: Int,
                var dayname: String,
                var hour: Int? = null,
                var minutes: Int? = null,
                var endHour: Int? = null,
                var endMinutes: Int? = null,
                var repeatOrDelayMark: String? = null,
                var repeatOrDelayValue: Int? = null,
                var repeatOrDelayUnit: Char? = null) {

    fun toJson(): String {
        var res: String = """ "active": $active, "year": $year, "month": $month, "day": $day, "dayname": "$dayname" """
        if(hour != null && minutes != null) {
            res += """, "time": "$hour:$minutes" """
        }
        if(endHour != null && endMinutes != null) {
            res += """, "end_time": "$endHour:$endMinutes" """
        }
        if(repeatOrDelayMark != null &&
           repeatOrDelayValue != null &&
           repeatOrDelayUnit != null) {
            res += """, "repeat_or_delay": "$repeatOrDelayMark$repeatOrDelayValue$repeatOrDelayUnit" """
        }
        return "{$res}"
    }

    override fun toString(): String {
        var res: String = "$year-$month-$day $dayname"
        if(hour != null && minutes != null) {
            res += " $hour:$minutes"
        }
        if(endHour != null && endMinutes != null) {
            res += "-$endHour:$endMinutes"
        }
        if(repeatOrDelayMark != null &&
           repeatOrDelayValue != null &&
           repeatOrDelayUnit != null) {
            res += " $repeatOrDelayMark$repeatOrDelayValue$repeatOrDelayUnit"
        }
        if(active) {
            return "<$res>"
        } else {
            return "[$res]"
        }
    }

    override fun equals(other: Any?): Boolean {
        if(other !is Timestamp) return false
        if(other.active != active) return false
        if(other.year != year) return false
        if(other.month != month) return false
        if(other.day != day) return false
        if(other.dayname != dayname) return false
        if(other.hour != hour) return false
        if(other.minutes != minutes) return false
        if(other.endHour != endHour) return false
        if(other.endMinutes != endMinutes) return false
        if(other.repeatOrDelayMark != repeatOrDelayMark) return false
        if(other.repeatOrDelayValue != repeatOrDelayValue) return false
        if(other.repeatOrDelayUnit != repeatOrDelayUnit) return false
        return true
    }

}

open class Section(text: MarkupText, level: Int, entities: List<Org> = emptyList(), var state: STATE = STATE.NONE) : Org(entities) {

    var level: Int = level
    var text: MarkupText = text
    var planning: List<Planning> = listOf()
    var properties: List<Property> = listOf()

    fun plan(planning: Planning) {
        this.planning += planning
    }

    fun addProperty(property: Property) {
        this.properties += property
    }

    override fun toString(): String = "\n${"*".repeat(level)} ${text.toString()}\n${super.toString()}"
    override fun toMarkdown(): String = "\n${"#".repeat(level)} ${text.toMarkdown()}\n${super.toMarkdown()}"

    override fun toJson(): String {
        var elements: String = ""

        for (i in entities.indices) {
            if (i != 0) elements += ", "
            elements += entities[i].toJson()
        }

        var planningJson = ""
        if(!planning.isEmpty()) {
            planningJson = "[ ${planning.foldIndexed("") {i, acc, e -> if(i != 0) acc + ", " + e.toJson() else e.toJson()}} ]"
        }

        return """{ "type": "section", "header": ${text.toJson()}, "level": $level, "state": "$state", "planning": $planningJson, "elements": [$elements] }"""
    }

    override fun toHtml(): String {
        var innerHtml: String = super.toHtml()

        var planningHtml = ""
        if(!planning.isEmpty()) {
            planningHtml = "${planning.foldIndexed("") {i, acc, e -> if(i != 0) acc + "</br>" + e.toHtml() else e.toHtml()}}"
        }
        return "<h$level>${if(state != STATE.NONE) state.toHtml() + " " else ""}${text.toHtml()}</h$level><hr>$planningHtml$innerHtml"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Section) return false
        if(planning.size != planning.size) return false
        for(i in planning.indices) {
            if(!planning[i].equals(other.planning[i])) return false
        }
        return state == other.state && other.text == text && other.level == level && super.equals(other)
    }
}

class Document(entities: List<Org> = emptyList()) : Section(Text(""), 0, entities) {

    override fun toString(): String {
        return entities.fold("") { acc, e -> acc + e.toString() }
    }
    override fun toMarkdown(): String {
        return entities.fold("") { acc, e -> acc + e.toMarkdown() }
    }

    override fun toJson(): String {
        var elements: String = ""

        for (i in entities.indices) {
            if (i != 0) elements += ", "
            elements += entities[i].toJson()
        }

        return """{ "type": "document", "elements": [$elements] }"""
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
