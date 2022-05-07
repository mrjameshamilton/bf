package eu.jameshamilton.bf.backend.jvm

import eu.jameshamilton.bf.backend.CodeGenerator
import eu.jameshamilton.bf.debug
import eu.jameshamilton.bf.frontend.Node
import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.AccessConstants.STATIC
import proguard.classfile.ClassPool
import proguard.classfile.VersionConstants.CLASS_VERSION_1_8
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.editor.ClassBuilder
import proguard.classfile.visitor.AllMethodVisitor
import proguard.classfile.visitor.ClassPrinter
import proguard.classfile.visitor.ClassVersionFilter
import proguard.io.DataEntry
import proguard.io.DataEntryClassWriter
import proguard.io.DataEntryWriter
import proguard.io.FixedFileWriter
import proguard.io.JarWriter
import proguard.io.ZipWriter
import proguard.preverify.CodePreverifier
import java.io.Closeable
import java.io.File
import java.io.OutputStream
import proguard.classfile.editor.CompactCodeAttributeComposer as Composer

class JvmCodeGenerator(private val execute: Boolean = false) : CodeGenerator {

    private val programClassPool: ClassPool = ClassPool()

    override fun generate(program: Node.Program, output: File?) {
        ClassBuilder(
            CLASS_VERSION_1_8,
            PUBLIC,
            "Main",
            "java/lang/Object",
        ).addMethod(PUBLIC or STATIC, "main", "([Ljava/lang/String;)V", 65_535) {
            program.accept(BytecodeComposer(it))
        }.programClass.apply {
            with(programClassPool) {
                addClass(this@apply)
                if (debug) {
                    classesAccept(ClassPrinter())
                }
                classesAccept(
                    ClassVersionFilter(
                        CLASS_VERSION_1_8,
                        AllMethodVisitor(
                            AllAttributeVisitor(
                                CodePreverifier(false)
                            )
                        )
                    )
                )
            }
        }

        if (execute) {
            ClassPoolClassLoader(programClassPool)
                .loadClass("Main")
                .declaredMethods
                .single { it.name == "main" }
                .invoke(null, emptyArray<String>())
        } else if (output != null) {
            writeJar(programClassPool, output, "Main")
        }
    }

    companion object {
        private const val MEMORY = 0
        private const val DP = 1
    }

    private inner class BytecodeComposer(private val composer: Composer) : Node.Visitor<Composer> {

        override fun visitProgram(program: Node.Program) = with(composer) {
            iconst(30_000)
            newarray(8 /* byte array */)
            astore(MEMORY)
            iconst_0()
            istore(DP)
            program.bodyAccept(this@BytecodeComposer)
            return_()
        }

        override fun visitMove(move: Node.Move) = with(composer) {
            iinc(DP, move.amount)
        }

        override fun visitAdd(add: Node.Add) = with(composer) {
            iconst(add.amount)
            aload(MEMORY)
            iload(DP)
            outline(programClassPool, "Util", "add", "(I[BI)V") {
                dup2_x1() // AMOUNT, MEMORY, DP -> MEMORY, DP, AMOUNT, MEMORY, DP
                baload()
                iadd()
                bastore()
            }
        }

        override fun visitZero(zero: Node.Zero) = with(composer) {
            aload(MEMORY)
            iload(DP)
            iconst_0()
            bastore()
        }

        override fun visitLoop(loop: Node.Loop) = with(composer) {
            val (start, end) = arrayOf(createLabel(), createLabel())
            label(start)
            aload(MEMORY)
            iload(DP)
            baload()
            ifeq(end)
            loop.bodyAccept(this@BytecodeComposer)
            goto_(start)
            label(end)
        }

        override fun visitPrint(print: Node.Print) = with(composer) {
            aload(MEMORY)
            iload(DP)
            outline(programClassPool, "Util", "print", "([BI)V") {
                getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
                dup_x2().pop() // MEMORY, DP, out -> out, MEMORY, DP
                baload()
                invokevirtual("java/io/PrintStream", "print", "(C)V")
            }
        }

        override fun visitRead(read: Node.Read) = with(composer) {
            aload(MEMORY)
            iload(DP)
            outline(programClassPool, "Util", "read", "([BI)V") {
                getstatic("java/lang/System", "in", "Ljava/io/InputStream;")
                dup_x2().pop() // MEMORY, DP, in -> in, MEMORY, DP
                iconst_1()
                invokevirtual("java/io/InputStream", "read", "([BII)I")
                pop()
            }
        }
    }

    private fun writeJar(programClassPool: ClassPool, file: File, mainClass: String) {
        class MyJarWriter(zipEntryWriter: DataEntryWriter) : JarWriter(zipEntryWriter), Closeable {
            override fun createManifestOutputStream(manifestEntry: DataEntry): OutputStream {
                val outputStream = super.createManifestOutputStream(manifestEntry)
                outputStream.writer().apply {
                    appendLine("Main-Class: $mainClass")
                    flush()
                }
                return outputStream
            }

            override fun close() {
                super.close()
            }
        }

        val jarWriter = MyJarWriter(
            ZipWriter(
                FixedFileWriter(
                    file
                )
            )
        )

        jarWriter.use {
            programClassPool.classesAccept(
                DataEntryClassWriter(it)
            )
        }
    }
}
