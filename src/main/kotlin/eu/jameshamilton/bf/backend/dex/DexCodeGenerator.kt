package eu.jameshamilton.bf.backend.dex

import com.github.netomi.bat.dexdump.DexDumpPrinter
import com.github.netomi.bat.dexfile.DexFile
import com.github.netomi.bat.dexfile.DexFormat
import com.github.netomi.bat.dexfile.MethodModifier
import com.github.netomi.bat.dexfile.MethodModifier.STATIC
import com.github.netomi.bat.dexfile.Visibility
import com.github.netomi.bat.dexfile.Visibility.PUBLIC
import com.github.netomi.bat.dexfile.editor.ClassDefEditor
import com.github.netomi.bat.dexfile.editor.DexEditor
import com.github.netomi.bat.dexfile.instruction.editor.InstructionBuilder
import com.github.netomi.bat.dexfile.io.DexFileWriter
import eu.jameshamilton.bf.backend.CodeGenerator
import eu.jameshamilton.bf.debug
import eu.jameshamilton.bf.frontend.Node
import java.io.File
import java.io.FileOutputStream
import java.util.EnumSet

class DexCodeGenerator : CodeGenerator {

    override fun generate(program: Node.Program, output: File?) {
        val dexFile = DexFile.of(DexFormat.FORMAT_035)

        DexEditor.of(dexFile).addClassDef("LMain;", PUBLIC)
            .addMethod("main", PUBLIC, EnumSet.of(STATIC), listOf("[Ljava/lang/String;"), "V", registers = 3) {
                program.accept(DexComposer(this))
            }
            .addMethod("print", PUBLIC, EnumSet.of(STATIC), listOf("B"), "V", registers = 3) {
                staticGetObject("Ljava/lang/System;", "out", "Ljava/io/PrintStream;", 0)
                intToChar(1, 2)
                invokeVirtual("Ljava/io/PrintStream;", "print", listOf("C"), "V", 0, 1)
                returnVoid()
            }
            .addMethod("read", PUBLIC, EnumSet.of(STATIC), emptyList(), "B", registers = 1) {
                staticGetObject("Ljava/lang/System;", "in", "Ljava/io/InputStream;", 0)
                invokeVirtual("Ljava/io/InputStream;", "read", emptyList(), "I", 0)
                moveResult(0)
                intToByte(0, 0)
                `return`(0)
            }

        if (debug) dexFile.accept(DexDumpPrinter())

        output?.let { dexFile.accept(DexFileWriter(FileOutputStream(it))) }
    }

    private class DexComposer(private val builder: InstructionBuilder) : Node.Visitor<InstructionBuilder> {

        override fun visitProgram(program: Node.Program): InstructionBuilder = builder.apply {
            const16(30000, MEMORY)
            newArray("[B", MEMORY, MEMORY)
            const4(0, DP)
            program.bodyAccept(this@DexComposer)
            returnVoid()
        }

        override fun visitMove(move: Node.Move): InstructionBuilder = builder.apply {
            addIntLit8(move.amount, DP, DP)
        }

        override fun visitAdd(add: Node.Add) = builder.apply {
            arrayGetByte(TEMP, MEMORY, DP)
            addIntLit8(add.amount, TEMP, TEMP)
            intToByte(TEMP, TEMP)
            arrayPutByte(TEMP, MEMORY, DP)
        }

        override fun visitZero(zero: Node.Zero) = builder.apply {
            const4(0, TEMP)
            arrayPutByte(TEMP, MEMORY, DP)
        }

        override fun visitLoop(loop: Node.Loop) = builder.apply {
            val startLabel = "start_${loop.hashCode()}"
            val endLabel = "end_${loop.hashCode()}"
            label(startLabel)
            arrayGetByte(TEMP, MEMORY, DP)
            ifEqualZero(TEMP, endLabel)
            loop.bodyAccept(this@DexComposer)
            goto16(startLabel)
            label(endLabel)
        }

        override fun visitPrint(print: Node.Print) = builder.apply {
            arrayGetByte(TEMP, MEMORY, DP)
            invokeStatic("LMain;", "print", listOf("B"), "V", TEMP)
        }

        override fun visitRead(read: Node.Read) = builder.apply {
            invokeStatic("LMain;", "read", emptyList(), "B")
            moveResult(TEMP)
            arrayPutByte(TEMP, MEMORY, DP)
        }

        companion object {
            private const val MEMORY = 0
            private const val DP = 1
            private const val TEMP = 2
        }
    }

    private fun ClassDefEditor.addMethod(
        name: String,
        visibility: Visibility,
        modifiers: EnumSet<MethodModifier>,
        parameters: List<String>,
        returnType: String,
        registers: Int = 0,
        composer: InstructionBuilder.() -> Unit
    ): ClassDefEditor {
        with(addMethod(name, visibility, modifiers, parameters, returnType).addCode()) {
            InstructionBuilder.of(this).apply {
                composer(this)
                prependInstruction(0, this.getInstructionSequence())
                finishEditing(registers)
            }
        }
        return this
    }
}
