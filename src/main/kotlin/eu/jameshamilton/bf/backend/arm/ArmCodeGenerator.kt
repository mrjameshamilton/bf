package eu.jameshamilton.bf.backend.arm

import eu.jameshamilton.bf.backend.CodeGenerator
import eu.jameshamilton.bf.debug
import eu.jameshamilton.bf.frontend.Node
import java.io.File

class ArmCodeGenerator : CodeGenerator {

    override fun generate(program: Node.Program, output: File?) {
        val arm = program.accept(ArmComposer())
        if (debug) println(arm)
        output?.writeText(arm)
    }

    class ArmComposer : Node.Visitor<String> {

        override fun visitProgram(program: Node.Program): String = """
        |.bss
        |.lcomm MEMORY, 30000
        |.text
        |.global _start
        |_start:
        |    ldr $DP, =MEMORY
        |    
        |${program.bodyAccept(this, ::concat) ?: ""}
        |
        |    // exit(0)
        |    mov     %r0, #0
        |    mov     %r7, $SYS_EXIT
        |    swi     #0
        | 
        """.trimMargin()

        override fun visitMove(move: Node.Move): String = """
        |    add $DP, $DP, #${move.amount}
        """.trimMargin()

        override fun visitAdd(add: Node.Add): String = """
        |    ldrb $TEMP, [$DP]
        |    add $TEMP, $TEMP, #${add.amount}
        |    strb $TEMP, [$DP]
        """.trimMargin()

        override fun visitZero(zero: Node.Zero): String = """
        |    mov $TEMP, #0
        |    strb $TEMP, [$DP]
        """.trimMargin()

        override fun visitLoop(loop: Node.Loop): String = """
        |    .start_${loop.hashCode()}:
        |         ldrb $TEMP, [$DP]
        |         cmp $TEMP, #0
        |         beq .end_${loop.hashCode()}
        |     ${loop.bodyAccept(this, ::concat)}    
        |         b .start_${loop.hashCode()}
        |    .end_${loop.hashCode()}:
        """.trimMargin()

        override fun visitPrint(print: Node.Print): String = """
        |    mov %r0, $STDOUT
        |    mov %r1, $DP
        |    mov %r2, #1
        |    mov %r7, $SYS_WRITE
        |    swi #0
        """.trimMargin()

        override fun visitRead(read: Node.Read): String = """
        |    mov %r0, $STDIN
        |    mov %r1, $DP
        |    mov %r2, #1
        |    mov %r7, $SYS_READ
        |    swi #0
        """.trimMargin()

        private fun concat(a: String, b: String) = "$a\n$b"

        companion object {
            private const val DP = "%r1"
            private const val TEMP = "%r2"

            private const val STDIN = "#0"
            private const val STDOUT = "#1"

            private const val SYS_EXIT = "#1"
            private const val SYS_READ = "#3"
            private const val SYS_WRITE = "#4"
        }
    }
}
