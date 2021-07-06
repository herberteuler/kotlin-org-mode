package orgmode

import java.io.File
import orgmode.parser.*
import orgmode.parser.combinator.CombOrgParser

var timer: Long = System.nanoTime()

fun tic() {
    timer = System.nanoTime()
}

fun toc(msg: String) {
    println("$msg in ${(System.nanoTime() - timer) / 1000000} ms")
}

fun main(args: Array<String>) {

    var org: Org

    if (args.size > 0) {
        tic()
        org = RegexOrgParser(FileSource(args[0])).parse()
        toc("File parsed")
    } else {

        org = CombOrgParser(
            StringSource("*** TODO Test dasd ads :ta:tu:te ")
        ).parse()
//         org = RegexOrgParser(
//             StringSource(
//                 """
// | test | test |
// |1 | 2|
// |-+-|
// |ggag|haha|

// 1. Test
// 2. Table
//    | +test+ | _Test_|
//    | 123 | =353= |
// """
//             )
//         ).parse()




    }

    if(args.size == 0) {
        println(org.toString())
        println(org.toJson())
        File("/tmp/kt.html").writeText(org.toHtml())
    }
    if(args.size == 2) {
        tic()
        File(args[1]).writeText(org.toHtml())
        toc("Html generated")
    }
    if(args.size == 1) {
        File("README.html").writeText(org.toHtml())
        File("README.md").writeText(org.toMarkdown())
    }
    // println(org.toString())
    // println(org.toJson())
}
