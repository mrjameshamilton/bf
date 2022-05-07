package eu.jameshamilton.bf

import eu.jameshamilton.bf.Target.*
import eu.jameshamilton.bf.backend.c.CCodeGenerator
import eu.jameshamilton.bf.backend.jvm.JvmCodeGenerator
import eu.jameshamilton.bf.backend.llvm.LlvmCodeGenerator
import eu.jameshamilton.bf.frontend.Parser
import eu.jameshamilton.bf.frontend.Scanner
import eu.jameshamilton.bf.optimize.Optimizer
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.File

enum class Target {
    JVM,
    C,
    LLVM
}

val parser = ArgParser("bf")
val script by parser.argument(ArgType.String, description = "brainf*ck script")
val output by parser.option(ArgType.String, shortName = "o", description = "output")
val target by parser.option(ArgType.Choice<Target>(), shortName = "t", description = "target").default(JVM)
val debug by parser.option(ArgType.Boolean, shortName = "d").default(false)

fun main(args: Array<String>) {
    parser.parse(args)

    try {
        val scanner = Scanner(File(script).readText())
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens)
        val program = parser.parse()
        val optimizedAst = Optimizer().optimize(program, passes = 1)
        val generator = when (target) {
            JVM -> JvmCodeGenerator(output == null)
            C -> CCodeGenerator()
            LLVM -> LlvmCodeGenerator()
        }
        generator.generate(optimizedAst, if (output != null) File(output) else null)
    } catch (e: Parser.ParseError) {
        println("Error encountered on line ${e.line}: ${e.message}")
    }
}
