package eu.jameshamilton.bf.backend

import eu.jameshamilton.bf.frontend.Node
import java.io.File

interface CodeGenerator {
    fun generate(program: Node.Program, output: File? = null)
}
