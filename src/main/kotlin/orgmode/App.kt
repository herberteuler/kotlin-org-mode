/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package orgmode

import java.io.File

var timer: Long = System.nanoTime()

fun tic() {
    timer = System.nanoTime()
}

fun toc(msg: String) {
    println("$msg done in ${(System.nanoTime() - timer)/1000000} ms")
}


fun main(args: Array<String>) {

    var org: Org

    tic()

    if(args.size > 0) {
	org = RegexOrgParser(FileSource(args[0])).parse()
    } else {

	org = RegexOrgParser(StringSource("""
* Unordered List
- elem 1

- elem 2

* Ordered List

1. elem 1
2. elem 2
""")).parse()
    }

    // println(org.toString())
    // println(org.toJson())

    toc("Regex Parser")
//     tic()

//     if(args.size > 0) {
// 	org = OrgParser(FileSource(args[0])).parse()
//     } else {

// 	org = OrgParser(StringSource("""
// * Unordered List
// - elem 1

// - elem 2

// * Ordered List

// 1. elem 1
// 2. elem 2
// """)).parse()
//     }

//     // println(org.toString())
//     // println(org.toJson())

//     toc("Fucking ugly parser")
    File("/tmp/kt.html").writeText(org.toHtml())

}
