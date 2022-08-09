package eu.jameshamilton.bf.lox

import eu.jameshamilton.bf.backend.CodeGenerator
import eu.jameshamilton.bf.frontend.Node
import java.io.File

class LoxCodeGenerator(input: String? = null) : CodeGenerator {
    val input = input?.toCharArray()?.toList() ?: emptyList()

    override fun generate(program: Node.Program, output: File?) {
        output?.writeText(program.accept(LoxComposer()))
    }

    inner class LoxComposer : Node.Visitor<String> {
        private var indent = 0

        override fun visitProgram(program: Node.Program): String = buildString {
            append(
                """
            |// Lox doesn't have built-in arrays, so use a doubly-linked list for the memory.
            |class Memory {
            |    init(size) {
            |        this.current = nil;
            |        var i = 0;
            |        while (i < size) {
            |            this.insert(0);
            |            i = i + 1;
            |        }
            |    }
            |    
            |    // Inserts a new cell at the beginning of the list.
            |    insert(value) {
            |        class Cell { }
            |        var newCell = Cell();
            |        newCell.value = value;
            |        newCell.next = this.current;
            |        newCell.prev = nil;
            |        if (this.current != nil) this.current.prev = newCell;
            |        this.current = newCell;    
            |    }
            |    
            |    // Move the current memory-cell left (-amount) or right (+amount).
            |    move(amount) {
            |        if (amount == 0) return;
            |        else if (amount > 0) {
            |            var i = 0;
            |            while (i < amount) {
            |                this.current = this.current.next;
            |                i = i + 1;
            |            }
            |        } else {
            |            var i = amount;
            |            while (i < 0) {
            |                this.current = this.current.prev;
            |                i = i + 1;
            |            }
            |        }
            |    }
            | 
            |    get() { return this.current.value; }
            |    set(value) { this.current.value = value; }
            |    add(value) { this.current.value = this.current.value + value; }
            |}
            |
            |var input = Memory(${input.size});
            |${input.reversed().joinToString("\n") { "input.insert(${it.code});" }}
            |var memory = Memory(30000);
            |
            """.trimMargin()
            )
            program.bodyAccept(this@LoxComposer) { a, b -> "$a\n$b" }?.let { append(it) }
            appendLine()
        }

        override fun visitMove(move: Node.Move): String = buildString {
            appendIndent("memory.move(${move.amount});")
        }

        override fun visitAdd(add: Node.Add): String = buildString {
            appendIndent("memory.add(${add.amount});")
        }

        override fun visitZero(zero: Node.Zero): String = buildString {
            appendIndent("memory.set(0);")
        }

        override fun visitLoop(loop: Node.Loop): String = buildString {
            appendLineIndent("while (memory.get() != 0) {")
            indent()
            loop.bodyAccept(this@LoxComposer) { a, b -> "$a\n$b" }?.let { appendLine(it) }
            outdent()
            appendIndent("}")
        }

        override fun visitPrint(print: Node.Print): String = buildString {
            appendIndent("print(memory.get());")
        }

        override fun visitRead(read: Node.Read): String = buildString {
            appendIndent("memory.set(input.get()); input.move(1);")
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
