
/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package orgmode

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

class RegexOrgParserTest {
    // @Test fun testAppHasAGreeting() {
    //     val classUnderTest = App()
    //     assertNotNull(classUnderTest.greeting, "app should have a greeting")
    // }

    fun parseMarkup(s: String): MarkupText {
	return MarkupText(RegexOrgParser(StringSource(s)).parse().entities[0].entities as List<MarkupText>)
    }

    @Test fun testParseText() {

	// val org: Org = RegexOrgParser(StringSource("Test")).parse()
	val org: Org = parseMarkup("Test")

	val res: Org = MarkupText(listOf(Text("Test")))

	println(org.toJson())
	println(res.toJson())

	assertEquals(org, res)
    }

    @Test fun testParseTextWords() {

	val org: Org = parseMarkup("Test Text")

	val res: Org = MarkupText(listOf(Text("Test Text")))

	println(org.toJson())
	println(res.toJson())

	assertEquals(org, res)
    }

    @Test fun testParseTextLines() {

	val org: Org = RegexOrgParser(StringSource("""Test Text
Second Line
""")).parse()

	val res: Org = Document(listOf(Paragraph(listOf(MarkupText(listOf(Text("Test Text"))), MarkupText(listOf(Text("Second Line")))))))

	println(org.toJson())
	println(res.toJson())

	assertEquals(org, res)
    }

    @Test fun testParseMarkupEmphasis() {

	val org: Org = RegexOrgParser(StringSource("""
*test* \\
***not header
****emphasis*
""")).parse()

	val res: Org = Document(
	    listOf(
		Paragraph(
		    listOf(
			MarkupText(
			    listOf(
				Emphasis(listOf(Text("test"))),
				LineBreak())
			),
			MarkupText(
			    listOf(
				Text("***not header"),
				Emphasis(listOf(Text("***emphasis")))
			))
		))
	))

	println(org.toJson())
	println(res.toJson())

	assertEquals(org, res)
    }

    @Test fun testParseMarkupStrikeout() {

	val org: Org = RegexOrgParser(StringSource("""
+test+ \\
+not list
+++not list+
""")).parse()

	val res: Org = Document(
	    listOf(
		Paragraph(
		    listOf(
			MarkupText(
			    listOf(
				Strikeout(listOf(Text("test"))),
				LineBreak())
			),
			MarkupText(
			    listOf(
				Text("+not list"),
				Strikeout(listOf(Text("++not list")))
			))
		))
	))

	println(org.toJson())
	println(res.toJson())

	assertEquals(org, res)
    }

    @Test fun testParseMarkup() {

	for((c, e) in mapOf('_' to ::Underline, '/' to ::Italic)) {
	    val org: Org = RegexOrgParser(StringSource("""
${c}test${c} \\
${c}test
""")).parse()

	    val res: Org = Document(
		listOf(
		    Paragraph(
			listOf(
			    MarkupText(
				listOf(
				    e(listOf(Text("test")), null),
				    LineBreak())
			    ),
			    MarkupText(
				listOf(
				    Text("${c}test")
			    ))
		    ))
	    ))

	    println(org.toJson())
	    println(res.toJson())

	    assertEquals(org, res)
	}
    }
    @Test fun testParseMarkupCode() {

	val org: Org = RegexOrgParser(StringSource("""
=*test*= \\
=test \\
test =code=
""")).parse()

	val res: Org = Document(
	    listOf(
		Paragraph(
		    listOf(
			MarkupText(
			    listOf(
				Code("*test*"),
				LineBreak()
			)),
			MarkupText(
			    listOf(
				Text("=test "),
				LineBreak()
			)),
			MarkupText(
			    listOf(
				Text("test"),
				Code("code")
			    ))
		))
	))

	println(org.toJson())
	println(res.toJson())

	assertEquals(org, res)
    }
}
