package eu.jameshamilton.bf.backend.smali

import eu.jameshamilton.bf.backend.CodeGenerator
import eu.jameshamilton.bf.debug
import eu.jameshamilton.bf.frontend.Node
import java.io.File
import java.lang.Integer.toHexString

class SmaliCodeGenerator : CodeGenerator {

    override fun generate(program: Node.Program, output: File?) {
        val smali = program.accept(SmaliComposer())
        if (debug) println(smali)
        output?.writeText(smali)
    }

    class SmaliComposer : Node.Visitor<String> {

        override fun visitProgram(program: Node.Program): String = """
        |.class public LMain;
        |.super Ljava/lang/Object;
        |.method public static main([Ljava/lang/String;)V
        |    .registers 3
        |    const/16 $MEMORY, 0x7530
        |    new-array $MEMORY, $MEMORY, [B
        |    const/4 $DP, 0x0
        |${program.bodyAccept(this, ::concat)}
        |    return-void
        |.end method
        | 
        |.method public static print(B)V
        |    .registers 3
        |    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
        |    int-to-char v1, p0
        |    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->print(C)V
        |    return-void
        |.end method

        |.method public static read()B
        |    .registers 1
        |    sget-object v0, Ljava/lang/System;->in:Ljava/io/InputStream;
        |    invoke-virtual {v0}, Ljava/io/InputStream;->read()I
        |    move-result v0
        |    int-to-byte v0, v0
        |    return v0
        |.end method
        |""".trimMargin()

        override fun visitMove(move: Node.Move): String = """
        |    add-int/lit8 $DP, $DP, 0x${toHexString(move.amount)}
        """.trimMargin()

        override fun visitAdd(add: Node.Add): String = """
        |    aget-byte $TEMP, $MEMORY, $DP
        |    add-int/lit8 $TEMP, $TEMP, 0x${toHexString(add.amount)}
        |    int-to-byte $TEMP, $TEMP
        |    aput-byte $TEMP, $MEMORY, $DP
        """.trimMargin()

        override fun visitZero(zero: Node.Zero): String = """
        |    const/4 $TEMP, 0
        |    aput-byte $TEMP, $MEMORY, $DP
        """.trimMargin()

        override fun visitLoop(loop: Node.Loop): String = """
        |    :start_${loop.hashCode()}
        |    aget-byte $TEMP, $MEMORY, $DP
        |    if-eqz $TEMP, :end_${loop.hashCode()}
        |${loop.bodyAccept(this, ::concat)}    
        |    goto :start_${loop.hashCode()}
        |    :end_${loop.hashCode()}
        """.trimMargin()

        override fun visitPrint(print: Node.Print): String = """
        |    aget-byte $TEMP, $MEMORY, $DP
        |    invoke-static {$TEMP}, LMain;->print(B)V
        """.trimMargin()

        override fun visitRead(read: Node.Read): String = """
        |    invoke-static {}, LMain;->read()B
        |    move-result $TEMP
        |    aput-byte $TEMP, $MEMORY, $DP
        """.trimMargin()

        private fun concat(a: String, b: String) = "$a\n$b"

        companion object {
            private const val MEMORY = "v0"
            private const val DP = "v1"
            private const val TEMP = "v2"
        }
    }
}
