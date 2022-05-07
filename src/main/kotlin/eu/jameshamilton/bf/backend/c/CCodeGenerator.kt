package eu.jameshamilton.bf.backend.c

import eu.jameshamilton.bf.backend.CodeGenerator
import eu.jameshamilton.bf.frontend.Node
import java.io.File

class CCodeGenerator : CodeGenerator {
    override fun generate(program: Node.Program, output: File?) {
        output?.writeText(program.accept(CComposer()))
    }

    class CComposer : Node.Visitor<String> {
        private var indent = 1

        override fun visitProgram(program: Node.Program): String = buildString {
            append(
                """
            |#include <stdio.h>
            |int main() {
            |   char memory[30000] = {0}; 
            |   char *ptr = memory;""".trimMargin()
            )
            program.bodyAccept(this@CComposer) { a, b -> "$a\n$b" }?.let { append(it) }
            appendLine()
            appendLine("}")
        }

        override fun visitMove(move: Node.Move): String = buildString {
            appendIndent("ptr += ${move.amount};")
        }

        override fun visitAdd(add: Node.Add): String = buildString {
            appendIndent("*ptr += ${add.amount};")
        }

        override fun visitZero(zero: Node.Zero): String = buildString {
            appendIndent("*ptr = 0;")
        }

        override fun visitLoop(loop: Node.Loop): String = buildString {
            appendLineIndent("while (*ptr != 0) {")
            indent()
            loop.bodyAccept(this@CComposer) { a, b -> "$a\n$b" }?.let { appendLine(it) }
            outdent()
            appendIndent("}")
        }

        override fun visitPrint(print: Node.Print): String = buildString {
            appendIndent("putchar(*ptr);")
        }

        override fun visitRead(read: Node.Read): String = buildString {
            appendIndent("*ptr = getchar();")
        }

        private fun StringBuilder.appendLineIndent(string: String): StringBuilder =
            appendLine("    ".repeat(indent) + string)

        private fun StringBuilder.appendIndent(string: String): StringBuilder =
            append("    ".repeat(indent) + string)

        private fun indent() {
            indent++
        }

        private fun outdent() {
            indent--
        }
    }
}
