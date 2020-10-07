/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package orgmode

import java.io.File
import orgmode.parser.*

fun main(args: Array<String>) {

    var org: Org

    if (args.size > 0) {
        org = RegexOrgParser(FileSource(args[0])).parse()
    } else {

        org = RegexOrgParser(
            StringSource(
                """
* DONE Test blocks [1/2] [44%]
CLOSED: [2020-02-02 Thue +1w]
:PROPERTIES:
:TAG: Test
:END:
#+BEGIN_SRC
test
code
#+END_SRC

1. [ ] Test
2. [X] List
3. Checkboxes
4. [ ]
"""
            )
        ).parse()
    }

    if(args.size == 0) {
        println(org.toString())
        println(org.toJson())
    }
    File("README.html").writeText(org.toHtml())
    File("README.md").writeText(org.toMarkdown())
}
