/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package orgmode

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

class AppTest {
    // @Test fun testAppHasAGreeting() {
    //     val classUnderTest = App()
    //     assertNotNull(classUnderTest.greeting, "app should have a greeting")
    // }
    @Test fun testParseText() {
	
	val org: Org = OrgParser(StringSource("""Test
""")).parse()

	val res: Org = Document(arrayOf(Paragraph(0, arrayOf(Text("Test")))))
	
	println(org.toJson())
	println(res.toJson())
	
	assertEquals(org, res)
    }

    @Test fun testParseSections() {
	
	val org: Org = OrgParser(StringSource("""* Test1
** Test 2
* Test 3
""")).parse()

	val res: Org = Document(
	    arrayOf(
		Section("Test1", 1, arrayOf(
			    Section("Test 2", 2, emptyArray())
		)),
		Section("Test 3", 1, emptyArray())
	    )
	)
	
	println(org.toJson())
	println(res.toJson())
	
	assertEquals(org, res)
    }

    @Test fun testParseSectionsWithText() {
	
	val org: Org = OrgParser(StringSource("""* Test1
Text 1
** Test 2
Text 2
Text 3
* Test 3
""")).parse()

	val res: Org = Document(
	    arrayOf(
		Section("Test1", 1, arrayOf(
			    Paragraph(1, arrayOf(
					  Text("Text 1")
			    )),
			    Section("Test 2", 2, arrayOf(
					Paragraph(2, arrayOf(
						      Text("Text 2"),
						      Text("Text 3")
					))
			    ))
		)),
		Section("Test 3", 1, emptyArray())
	    )
	)
	
	println(org.toJson())
	println(res.toJson())
	
	assertEquals(org, res)
    }
}
