package eu.jameshamilton.bf.backend.js

import eu.jameshamilton.bf.backend.CodeGenerator
import eu.jameshamilton.bf.debug
import eu.jameshamilton.bf.frontend.Node
import java.io.File

class JsCodeGenerator : CodeGenerator {
    override fun generate(program: Node.Program, output: File?) {
        val js = program.accept(JsComposer())
        if (debug) println(js)
        output?.writeText(js)
    }

    class JsComposer : Node.Visitor<String> {
        private var indent = 0

        override fun visitProgram(program: Node.Program): String = buildString {
            append(
                """
            |#!/usr/bin/env node
            |'use strict'
            |const memory = new Array(30000).fill(0)
            |let input = process.argv.slice(2).join('').split('')
            |let ptr = 0
            |""".trimMargin()
            )
            program.bodyAccept(this@JsComposer) { a, b -> "$a\n$b" }?.let { append(it) }
            appendLine()
        }

        override fun visitMove(move: Node.Move): String = buildString {
            appendIndent("ptr += ${move.amount}")
        }

        override fun visitAdd(add: Node.Add): String = buildString {
            appendIndent("memory[ptr] += ${add.amount}")
        }

        override fun visitZero(zero: Node.Zero): String = buildString {
            appendIndent("memory[ptr] = 0")
        }

        override fun visitLoop(loop: Node.Loop): String = buildString {
            appendLineIndent("while (memory[ptr]) {")
            indent()
            loop.bodyAccept(this@JsComposer) { a, b -> "$a\n$b" }?.let { appendLine(it) }
            outdent()
            appendIndent("}")
        }

        override fun visitPrint(print: Node.Print): String = buildString {
            appendIndent("process.stdout.write(String.fromCharCode(memory[ptr]))")
        }

        override fun visitRead(read: Node.Read): String = buildString {
            appendLineIndent("if (input.length > 0) memory[ptr] = input.shift().charCodeAt(0)")
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
