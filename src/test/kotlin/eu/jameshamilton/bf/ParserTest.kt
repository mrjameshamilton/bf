package eu.jameshamilton.bf

import eu.jameshamilton.bf.frontend.AstPrinter
import eu.jameshamilton.bf.frontend.Parser
import eu.jameshamilton.bf.frontend.Scanner
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ParserTest : FreeSpec({
    "Hello World should be parsed  correctly" {
        val helloWorld =
            """++++++++[>++++[>++>+++>+++>+<<<<-]
              |>+>+>->>+[<]<-]>>.>---.+++++++..++
              |+.>>.<-.<.+++.------.--------.>>+.
              |>++.""".trimMargin()

        parseAndPrint(helloWorld) shouldBe helloWorld.replace("\n", "")
    }

    "All commands should be parsed correctly" {
        parseAndPrint("[+-]<>,.") shouldBe "[+-]<>,."
    }

    "Non-commands should be ignored" {
        parseAndPrint("Hello [+-]<>,. World!") shouldBe "[+-]<>,."
    }
})

fun parseAndPrint(string: String): String =
    Parser(Scanner(string).scanTokens()).parse().accept(AstPrinter())
