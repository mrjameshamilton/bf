package eu.jameshamilton.bf

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class BfTest : FreeSpec({
    val resource = object {}.javaClass.getResource("/bf")
    val dir = File(resource.file)
    dir.walk().filter { it.extension == "bf" }.forEach { file ->
        file.name {
            execute(file) {
                main(arrayOf(file.absolutePath))
            }
        }
    }
})

fun execute(file: File, executor: (String) -> Unit) {
    val text = file.readText()
    val expected = text.replace(Regex("""[<>,.+\-\[\]]"""), "")
    val oldOut = System.out
    val oldErr = System.err
    val myOut = ByteArrayOutputStream()
    val myErr = ByteArrayOutputStream()
    System.setOut(PrintStream(myOut))
    System.setErr(PrintStream(myErr))
    shouldNotThrowAny {
        executor(text)
    }
    System.setOut(oldOut)
    System.setErr(oldErr)
    val errText = myErr.toByteArray().decodeToString()
    val outText = myOut.toByteArray().decodeToString()

    errText.trimEnd() shouldBe ""
    outText.trimEnd() shouldBe expected.trimEnd()
}
