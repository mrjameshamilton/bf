package eu.jameshamilton.bf

import eu.jameshamilton.bf.frontend.Node
import eu.jameshamilton.bf.frontend.Parser
import eu.jameshamilton.bf.frontend.Scanner
import eu.jameshamilton.bf.optimize.Optimizer
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlin.math.abs

class OptimizerTest : FreeSpec({

    "Zero-ing loops should be replaced by a zero instruction" {
        optimize(",[+]") shouldBe ",[Z]"
        optimize(",[-]") shouldBe ",[Z]"
    }

    "Consecutive zero commands should be merged" {
        optimize(",[+][-]") shouldBe ",[Z]"
        optimize(",[-][-]") shouldBe ",[Z]"
        optimize(",[+][+]") shouldBe ",[Z]"
        optimize("," + "[+][+]".repeat(10)) shouldBe ",[Z]"
    }

    "Non-consecutive zero commands should not be merged" {
        optimize(",[+]>[-]") shouldBe ",[Z]>{1}[Z]"
        optimize(",[-]>[-]") shouldBe ",[Z]>{1}[Z]"
        optimize(",[+]>[+]") shouldBe ",[Z]>{1}[Z]"
        optimize("," + "[+]>[+]".repeat(10)) shouldBe "," + "[Z]>{1}".repeat(10) + "[Z]"
    }

    "Consecutive moves should be merged" {
        optimize(">>>>") shouldBe ">{4}"
        optimize("<<<<") shouldBe "<{4}"
        optimize("<<.>>") shouldBe "<{2}.>{2}"
        optimize("<<>") shouldBe "<{1}"
    }

    "Consecutive moves resulting in net-zero movement should be eliminated" {
        optimize("<<>>") shouldBe ""
    }

    "Consecutive adds should be merged" {
        optimize("++++") shouldBe "+{4}"
        optimize("----") shouldBe "-{4}"
        optimize("--.++") shouldBe "-{2}.+{2}"
        optimize("--+") shouldBe "-{1}"
    }

    "Consecutive adds resulting in net-zero increase should be eliminated" {
        optimize("++--") shouldBe ""
    }

    "Empty loops should be removed" {
        optimize(".[]") shouldBe "."
    }

    "Empty loops inside other loops should be removed" {
        optimize(",[+[]]") shouldBe ",[+{1}]"
    }

    "Loops before memory changes should be removed" {
        optimize("[++++]+++") shouldBe "+{3}"
        optimize("[++++][----]+++") shouldBe "+{3}"
        optimize("[++++]-[----]+++") shouldBe "-{1}[-{4}]+{3}"
        optimize(".[++++]") shouldBe "."
        optimize(",[++++]") shouldBe ",[+{4}]"
        optimize("[+++]") shouldBe ""
    }

    "Non instructions should be removed" {
        optimize("hello[hello]") shouldBe ""
        optimize("hello[+++hello]") shouldBe ""
    }

    "Operations that cancel are reduced after multiple passes" {
        optimize("++++-->>+-<<<") shouldBe "+{2}<{1}"
    }

    "Zero-ing before memory updates are removed" {
        optimize("[+]") shouldBe ""
        optimize("...[+]") shouldBe "..."
    }
})

fun optimize(string: String): String =
    Optimizer().optimize(Parser(Scanner(string).scanTokens()).parse(), passes = 2).accept(DebugAstPrinter())

class DebugAstPrinter : Node.Visitor<String> {
    override fun visitMove(move: Node.Move): String = (if (move.amount < 0) "<" else ">") + "{${abs(move.amount)}}"

    override fun visitAdd(add: Node.Add): String = (if (add.amount < 0) "-" else "+") + "{${abs(add.amount)}}"

    override fun visitZero(zero: Node.Zero): String = "[Z]"

    override fun visitLoop(loop: Node.Loop): String = "[${loop.body.joinToString(separator = "") { it.accept(this) }}]"

    override fun visitPrint(print: Node.Print): String = "."

    override fun visitRead(read: Node.Read): String = (",")

    override fun visitProgram(program: Node.Program): String =
        program.body.joinToString(separator = "") { it.accept(this) }
}
