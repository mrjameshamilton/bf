package eu.jameshamilton.bf.backend.jvm

import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.AccessConstants.STATIC
import proguard.classfile.ClassPool
import proguard.classfile.ProgramClass
import proguard.classfile.TypeConstants.*
import proguard.classfile.VersionConstants.CLASS_VERSION_1_8
import proguard.classfile.editor.ClassBuilder
import proguard.classfile.util.ClassUtil.internalMethodReturnType
import proguard.classfile.editor.CompactCodeAttributeComposer as Composer

/**
 * The `outline` helper function moves the code in the provided `composer` block to
 * a separate static method in the specified class, which is then called from the
 * current composer context.
 *
 * The parameters (types as specified in the `descriptor`) are automatically placed
 * onto the stack before the `composer` code, and the return value on the top of the stack
 * (type as specified in the `descriptor`) is returned.
 *
 * If the method already exists it is not created again.
 *
 * In the following example, the current composer is `composer` and a separate method called
 * `Util.add(II)I` will be generated and called from the current `composer` context:
 *
 * ```
 * with (composer) {
 *     getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
 *     iconst_1()
 *     iconst_2()
 *     outline(programClassPool, "Util", "add", "(II)I") {
 *         iadd()
 *     }
 *     invokevirtual("java/io/PrintStream", "print", "(I)V")
 * }
 * ```
 */
fun Composer.outline(
    programClassPool: ClassPool,
    className: String,
    name: String,
    descriptor: String,
    composer: Composer.() -> Composer
): Composer {
    val utilClass = programClassPool.getClass(className) ?: ClassBuilder(
        CLASS_VERSION_1_8,
        PUBLIC,
        className,
        "java/lang/Object"
    ).programClass.apply { programClassPool.addClass(this) } as ProgramClass

    return invokestatic(
        utilClass,
        utilClass.findMethod(name, descriptor) ?: ClassBuilder(utilClass as ProgramClass)
            .addAndReturnMethod(PUBLIC or STATIC, name, descriptor, 65_535) {
                for ((offset, type) in parameterSequence(descriptor)) {
                    when (type) {
                        "I", "B", "C", "S", "Z" -> it.iload(offset)
                        "D" -> it.dload(offset)
                        "F" -> it.fload(offset)
                        "J" -> it.lload(offset)
                        else -> it.aload(offset)
                    }
                }

                composer(it)

                when (internalMethodReturnType(descriptor)) {
                    "I", "B", "C", "S", "Z" -> it.ireturn()
                    "D" -> it.dreturn()
                    "F" -> it.freturn()
                    "J" -> it.dreturn()
                    "V" -> it.return_()
                    else -> it.areturn()
                }
            }
    )
}

private fun parameterSequence(descriptor: String) = sequence {
    var parameterIndex = 0
    var parameterOffset = 0
    var index = 1
    while (index < descriptor.length) {
        var nextIndex = index + 1
        var parameterSize = 1
        var c = descriptor[index]
        when (c) {
            LONG, DOUBLE -> parameterSize = 2
            CLASS_START -> nextIndex = descriptor.indexOf(CLASS_END, nextIndex) + 1
            ARRAY -> {
                do {
                    c = descriptor[nextIndex]
                } while (descriptor[nextIndex++] == ARRAY)
                if (c == CLASS_START) nextIndex = descriptor.indexOf(CLASS_END, nextIndex) + 1
            }
            METHOD_ARGUMENTS_CLOSE -> break
        }

        yield(ParameterInfo(parameterOffset, descriptor.substring(index, nextIndex)))

        index = nextIndex
        parameterOffset += parameterSize
        parameterIndex++
    }
}

private data class ParameterInfo(val offset: Int, val type: String)
