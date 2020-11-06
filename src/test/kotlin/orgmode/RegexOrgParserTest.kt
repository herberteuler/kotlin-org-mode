
package orgmode

import kotlin.test.Test
import kotlin.test.assertEquals
import orgmode.parser.*

class RegexOrgParserTest {

    val DEBUG: Boolean = false

    @Suppress("UNCHECKED_CAST")
    fun parseMarkup(s: String): MarkupText {
        return MarkupText(RegexOrgParser(StringSource(s)).parse().entities[0].entities as List<MarkupText>)
    }

    @Test fun testParseText() {

        // val org: Org = RegexOrgParser(StringSource("Test")).parse()
        val org: Org = parseMarkup("Test")

        val res: Org = MarkupText(listOf(Text("Test")))

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }

    @Test fun testParseTextWords() {

        val org: Org = parseMarkup("Test Text")

        val res: Org = MarkupText(listOf(Text("Test Text")))

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }

    @Test fun testParseTextLines() {

        val org: Org = RegexOrgParser(
            StringSource(
                """Test Text
Second Line
"""
            )
        ).parse()

        val res: Org = Document(listOf(Paragraph(listOf(MarkupText(listOf(Text("Test Text"))), MarkupText(listOf(Text("Second Line")))))))

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }

    @Test fun testParseMarkupEmphasis() {

        val org: Org = RegexOrgParser(
            StringSource(
                """
*test* \\
***not header
****emphasis*
"""
            )
        ).parse()

        val res: Org = Document(
            listOf(
                Paragraph(
                    listOf(
                        MarkupText(
                            listOf(
                                Emphasis(listOf(Text("test"))),
                                LineBreak()
                            )
                        ),
                        MarkupText(
                            listOf(
                                Text("***not header"),
                                Emphasis(listOf(Text("***emphasis")))
                            )
                        )
                    )
                )
            )
        )

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }

    @Test fun testParseMarkupStrikeout() {

        val org: Org = RegexOrgParser(
            StringSource(
                """
+test+ \\
+not list
+++not list+
"""
            )
        ).parse()

        val res: Org = Document(
            listOf(
                Paragraph(
                    listOf(
                        MarkupText(
                            listOf(
                                Strikeout(listOf(Text("test"))),
                                LineBreak()
                            )
                        ),
                        MarkupText(
                            listOf(
                                Text("+not list"),
                                Strikeout(listOf(Text("++not list")))
                            )
                        )
                    )
                )
            )
        )

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }

    @Test fun testParseMarkup() {

        for ((c, e) in mapOf('_' to ::Underline, '/' to ::Italic)) {
            val org: Org = RegexOrgParser(
                StringSource(
                    """
${c}test$c \\
${c}test
"""
                )
            ).parse()

            val res: Org = Document(
                listOf(
                    Paragraph(
                        listOf(
                            MarkupText(
                                listOf(
                                    e(listOf(Text("test")), null),
                                    LineBreak()
                                )
                            ),
                            MarkupText(
                                listOf(
                                    Text("${c}test")
                                )
                            )
                        )
                    )
                )
            )

            if(DEBUG) println(org.toJson())
            if(DEBUG) println(res.toJson())

            assertEquals(org, res)
        }
    }
    @Test fun testParseMarkupCode() {

        val org: Org = RegexOrgParser(
            StringSource(
                """
=*test*= \\
=test \\
test =code=
"""
            )
        ).parse()

        val res: Org = Document(
            listOf(
                Paragraph(
                    listOf(
                        MarkupText(
                            listOf(
                                Code("*test*"),
                                LineBreak()
                            )
                        ),
                        MarkupText(
                            listOf(
                                Text("=test "),
                                LineBreak()
                            )
                        ),
                        MarkupText(
                            listOf(
                                Text("test"),
                                Code("code")
                            )
                        )
                    )
                )
            )
        )

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }
    @Test fun testParseMarkupLink() {

        val org: Org = RegexOrgParser(
            StringSource(
                """
[[https://iliayar.ru]]
[[http://iliayar.ru][*Home* /page/]]
"""
            )
        ).parse()

        val res: Org = Document(
            listOf(
                Paragraph(
                    listOf(
                        Link("https://iliayar.ru"),
                        Link(
                            "http://iliayar.ru",
                            listOf(
                                Emphasis(listOf(Text("Home"))),
                                Italic(listOf(Text("page")))
                            )
                        )
                    )
                )
            )
        )

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }
    @Test fun testParseSections() {

        val org: Org = RegexOrgParser(
            StringSource(
                """* Test1
** Test 2
* Test 3
"""
            )
        ).parse()

        val res: Org = Document(
            listOf(
                Section(
                    parseMarkup("Test1"), 1,
                    listOf(
                        Section(parseMarkup("Test 2"), 2, emptyList())
                    )
                ),
                Section(parseMarkup("Test 3"), 1, emptyList())
            )
        )

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }

    @Test fun testParseSectionsWithText() {

        val org: Org = RegexOrgParser(
            StringSource(
                """* Test1
Text 1
** Test 2
Text 2
Text 3
* Test 3
"""
            )
        ).parse()

        val res: Org = Document(
            listOf(
                Section(
                    parseMarkup("Test1"), 1,
                    listOf(
                        Paragraph(
                            listOf(
                                parseMarkup("Text 1")
                            )
                        ),
                        Section(
                            parseMarkup("Test 2"), 2,
                            listOf(
                                Paragraph(
                                    listOf(
                                        parseMarkup("Text 2"),
                                        parseMarkup("Text 3")
                                    )
                                )
                            )
                        )
                    )
                ),
                Section(parseMarkup("Test 3"), 1, emptyList())
            )
        )

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }

    @Test fun testMulitline() {

        val org: Org = RegexOrgParser(
            StringSource(
                """
* Doc
Text 1
Same line\\
Another line
"""
            )
        ).parse()

        val res: Org = Document(
            listOf(
                Section(
                    parseMarkup("Doc"), 1,
                    listOf(
                        Paragraph(
                            listOf(
                                parseMarkup("Text 1"),
                                parseMarkup("Same line\\\\\n"),
                                parseMarkup("Another line")
                            )
                        )
                    )
                )
            )
        )

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }
    @Test fun testList() {

        val org: Org = RegexOrgParser(
            StringSource(
                """
* Unordered List
- elem 1

- elem 2

* Ordered List

1. elem 1

2. elem 2
"""
            )
        ).parse()

        val res: Org = Document(
            listOf(
                Section(
                    parseMarkup("Unordered List"), 1,
                    listOf(
                        OrgList(
                            listOf(
                                ListEntry(parseMarkup("elem 1")),
                                ListEntry(parseMarkup("elem 2"))
                            )
                        )
                    )
                ),
                Section(
                    parseMarkup("Ordered List"), 1,
                    listOf(
                        OrgList(
                            listOf(
                                ListEntry(parseMarkup("elem 1"), "1."),
                                ListEntry(parseMarkup("elem 2"), "2.")
                            )
                        )
                    )
                )

            )
        )

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }

    @Test fun testListWithContent() {

        val org: Org = RegexOrgParser(
            StringSource(
                """
* List
- elem 1
  Text
Not in list
- elem 2

  Another Text

  Still in list


         Not in list too

"""
            )
        ).parse()

        val res: Org = Document(
            listOf(
                Section(
                    parseMarkup("List"), 1,
                    listOf(
                        OrgList(
                            listOf(
                                ListEntry(
                                    parseMarkup("elem 1"),
                                    entities =
                                        listOf(
                                            Paragraph(
                                                listOf(
                                                    parseMarkup("Text")
                                                )
                                            )
                                        )
                                )
                            )
                        ),
                        Paragraph(
                            listOf(
                                parseMarkup("Not in list")
                            )
                        ),
                        OrgList(
                            listOf(
                                ListEntry(
                                    parseMarkup("elem 2"),
                                    entities =
                                        listOf(
                                            Paragraph(
                                                listOf(
                                                    parseMarkup("Another Text")
                                                )
                                            ),
                                            Paragraph(
                                                listOf(
                                                    parseMarkup("Still in list")
                                                )
                                            )
                                        )
                                )
                            )
                        ),
                        Paragraph(
                            listOf(
                                parseMarkup("Not in list too")
                            )
                        )
                    )
                )
            )
        )

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }

    @Test fun testTwoEmptyLinesBreaksAllLists() {

        val org: Org = RegexOrgParser(
            StringSource(
                """
* List
- elem 1
  Text
  - inner list
    Inner list Text


  Text

"""
            )
        ).parse()

        val res: Org = Document(
            listOf(
                Section(
                    parseMarkup("List"), 1,
                    listOf(
                        OrgList(
                            listOf(
                                ListEntry(
                                    parseMarkup("elem 1"),
                                    entities =
                                        listOf(
                                            Paragraph(
                                                listOf(
                                                    parseMarkup("Text")
                                                )
                                            ),
                                            OrgList(
                                                listOf(
                                                    ListEntry(
                                                        parseMarkup("inner list"),
                                                        entities =
                                                            listOf(
                                                                Paragraph(
                                                                    listOf(
                                                                        parseMarkup("Inner list Text")
                                                                    )
                                                                )
                                                            )
                                                    )
                                                )
                                            )
                                        )
                                )
                            )
                        ),
                        Paragraph(
                            listOf(
                                parseMarkup("Text")
                            )
                        )

                    )
                )
            )
        )

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }

    @Test fun testIndentBreaksAllLists() {

        val org: Org = RegexOrgParser(
            StringSource(
                """
* List
- elem 1
  Text
  - inner list
    Inner list Text
Text

"""
            )
        ).parse()

        val res: Org = Document(
            listOf(
                Section(
                    parseMarkup("List"), 1,
                    listOf(
                        OrgList(
                            listOf(
                                ListEntry(
                                    parseMarkup("elem 1"),
                                    entities =
                                        listOf(
                                            Paragraph(
                                                listOf(
                                                    parseMarkup("Text")
                                                )
                                            ),
                                            OrgList(
                                                listOf(
                                                    ListEntry(
                                                        parseMarkup("inner list"),
                                                        entities =
                                                            listOf(
                                                                Paragraph(
                                                                    listOf(
                                                                        parseMarkup("Inner list Text")
                                                                    )
                                                                )
                                                            )
                                                    )
                                                )
                                            )
                                        )
                                )
                            )
                        ),
                        Paragraph(
                            listOf(
                                parseMarkup("Text")
                            )
                        )

                    )
                )
            )
        )

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }
    @Test fun testStatisticAndCheckboxesList() {

        val org: Org = RegexOrgParser(
            StringSource(
                """
* Unordered List [1/2][50%]
- [ ] elem 1

- [X] elem 2
"""
            )
        ).parse()

        val res: Org = Document(
            listOf(
                Section(
                    MarkupText(listOf(Text("Unordered List "), StatisticCookie("[1/2]"), StatisticCookie("[50%]"))), 1,
                    listOf(
                        OrgList(
                            listOf(
                                ListEntry(parseMarkup("elem 1"), checkbox = LIST_CHECKBOX.UNCHECKED),
                                ListEntry(parseMarkup("elem 2"), checkbox = LIST_CHECKBOX.CHECKED)
                            )
                        )
                    )
                )
            )
        )

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }
    @Test fun testStatusAndScheduling() {

        val org: Org = RegexOrgParser(
            StringSource(
                """
* TODO Unordered List
DEADLINE: <2020-01-16 Thue 22:00-23:30 +1d>
- elem 1
- elem 2
"""
            )
        ).parse()

        val res: Org = Document(
            listOf(
                Section(
                    MarkupText(listOf(Text("Unordered List "))), 1,
                    listOf(
                        OrgList(
                            listOf(
                                ListEntry(parseMarkup("elem 1")),
                                ListEntry(parseMarkup("elem 2"))
                            )
                        )
                    ), STATE.TODO
                ).apply { plan(Planning(PLANNING_TYPE.DEADLINE, Timestamp(true, 2020, 1, 16, "Thue", 22, 0, 23, 30, "+", 1, 'd'))) }
            )
        )

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }

    @Test fun testTables() {

        val org: Org = RegexOrgParser(
            StringSource(
                """
* Table
|  test |   *bold* |
|-------+----------|
| value | /italic/ |


| not | table
"""
            )
        ).parse()

        val res: Org = Document(
            listOf(
                Section(
                    MarkupText(listOf(Text("Table"))), 1,
                    listOf(
                        OrgTable(listOf(
                                     OrgTableLine(listOf(
                                                      parseMarkup("test"),
                                                      parseMarkup("*bold*")
                                     )),
                                     OrgTableSplit(2),
                                     OrgTableLine(listOf(
                                                      parseMarkup("value"),
                                                      parseMarkup("/italic/")
                                     ))
                        )),
                        Paragraph(listOf(
                                      parseMarkup("| not | table")
                        ))
                    )
                )
            )
        )

        if(DEBUG) println(org.toJson())
        if(DEBUG) println(res.toJson())

        assertEquals(org, res)
    }
}
