package eu.jameshamilton.bf.backend.wasm

import eu.jameshamilton.bf.backend.CodeGenerator
import eu.jameshamilton.bf.debug
import eu.jameshamilton.bf.frontend.Node
import java.io.File

class WasmCodeGenerator : CodeGenerator {

    override fun generate(program: Node.Program, output: File?) {
        val wasm = program.accept(WasmComposer())
        if (debug) println(wasm)
        output?.writeText(wasm)
    }

    class WasmComposer : Node.Visitor<String> {

        var indent = 0

        override fun visitProgram(program: Node.Program): String = """
        |(module
        |    ;; fd_write(fd, io_vec_addr, bytes_to_write, bytes_written)
        |    (import "wasi_unstable" "fd_write" (func ${'$'}fd_write (param i32 i32 i32 i32) (result i32)))
        |    ;; fd_read(fd, io_vec_addr, bytes_to_read, bytes_read)
        |    (import "wasi_unstable" "fd_read" (func ${'$'}fd_read (param i32 i32 i32 i32) (result i32)))
        |    (import "wasi_unstable" "proc_exit" (func ${'$'}proc_exit (param i32)))
        |
        |    (memory 1)
        |
        |    (func ${'$'}_start (local $DP i32)
        |        ;; IO_VEC_ARRAY_LENGTH will always be 1, so set it once.
        |        (i32.store (i32.const $IO_VEC_ARRAY_LENGTH) (i32.const 1))
        | 
        |        ${program.bodyAccept(this, ::concat) ?: ""}
        | 
        |        (call ${'$'}proc_exit (i32.const 0))
        |    )
        |
        |    (export "memory" (memory 0))
        |    (export "_start" (func ${'$'}_start))
        |)
        """.trimMargin()

        override fun visitMove(move: Node.Move): String = """
        |    local.get $DP
        |    i32.const ${move.amount}
        |    i32.add
        |    local.set $DP
        """.trimMargin()

        override fun visitAdd(add: Node.Add): String = """
        |    local.get $DP
        |    (i32.load8_u (local.get $DP))
        |    i32.const ${add.amount}
        |    i32.add
        |    i32.store8
        """.trimMargin()

        override fun visitZero(zero: Node.Zero): String = """
        |    (i32.store8 (local.get $DP) (i32.const 0))
        """.trimMargin()

        override fun visitLoop(loop: Node.Loop): String = """
        |    block
        |        loop
        |            (i32.load8_u (local.get $DP))
        |            i32.eqz
        |            br_if 1 ;; branch to parent block
        |            ${loop.bodyAccept(this, ::concat)}    
        |            br 0 ;; branch to beginning of loop
        |        end    
        |    end
        """.trimMargin()

        override fun visitPrint(print: Node.Print): String = """
        |    ;; put the current DP into the iovec address.
        |    (i32.store (i32.const $IO_VEC_ARRAY_ADDR) (local.get $DP))
        |    ;; IO_VEC_ARRAY_LENGTH is already set to 1
        |    (call ${'$'}fd_write 
        |        (i32.const $STDOUT)
        |        (i32.const $IO_VEC_ARRAY_ADDR)
        |        (i32.const 1) ;; write 1 byte
        |        (i32.const $IO_BYTES_WRITTEN_ADDR)
        |    )
        |    drop ;; the fd_write return value
        """.trimMargin()

        override fun visitRead(read: Node.Read): String = """
        |    ;; put the current DP into the iovec address.
        |    (i32.store (i32.const $IO_VEC_ARRAY_ADDR) (local.get $DP))
        |    ;; IO_VEC_ARRAY_LENGTH is already set to 1
        |    (call ${'$'}fd_read 
        |        (i32.const $STDIN)
        |        (i32.const $IO_VEC_ARRAY_ADDR)
        |        (i32.const 1) ;; read 1 byte
        |        (i32.const $IO_BYTES_READ_ADDR)
        |    )
        |    drop ;; the fd_read return value
        """.trimMargin()

        private fun concat(a: String, b: String) = "$a\n$b"

        companion object {
            private const val DP = "${'$'}ptr"

            private const val STDOUT = 1
            private const val STDIN = 0

            private const val IO_VEC_ARRAY_ADDR = 30_000
            private const val IO_VEC_ARRAY_LENGTH = IO_VEC_ARRAY_ADDR + 4
            private const val IO_BYTES_WRITTEN_ADDR = IO_VEC_ARRAY_ADDR + 8
            private const val IO_BYTES_READ_ADDR = IO_BYTES_WRITTEN_ADDR
        }
    }
}
