package eu.jameshamilton.bf.backend.llvm

import eu.jameshamilton.bf.backend.CodeGenerator
import eu.jameshamilton.bf.frontend.Node
import java.io.File

class LlvmCodeGenerator : CodeGenerator {
    override fun generate(program: Node.Program, output: File?) {
        output?.writeText(program.accept(LlvmComposer()))
    }

    class LlvmComposer : Node.Visitor<String> {
        private var ptr = 0
        private var n = 0

        override fun visitProgram(program: Node.Program): String = declare(4) { (memory, ptr, i, j) ->
            this.ptr = ptr
            """
            |define i32 @main() {
            |    %$memory = alloca [30000 x i8], align 16
            |    %$ptr = alloca i8*, align 8
            |    %$i = bitcast [30000 x i8]* %$memory to i8*
            |    call void @llvm.memset.p0i8.i64(i8* align 16 %$i, i8 0, i64 30000, i1 false)
            |    %$j = getelementptr inbounds [30000 x i8], [30000 x i8]* %$memory, i64 0, i64 0
            |    store i8* %$j, i8** %$ptr, align 8 
            |${program.bodyAccept(this, ::concat)}
            |    ret i32 0
            |}
            |
            |declare void @llvm.memset.p0i8.i64(i8* nocapture writeonly, i8, i64, i1 immarg)
            |declare i32 @putchar(i32)
            |declare i32 @getchar()
            |
            """.trimMargin()
        }

        override fun visitMove(move: Node.Move): String = declare(2) { (i, j) ->
            """
            |    %$i = load i8*, i8** %$ptr, align 8
            |    %$j = getelementptr inbounds i8, i8* %$i, i64 ${move.amount}
            |    store i8* %$j, i8** %$ptr, align 8
            """.trimMargin()
        }

        override fun visitAdd(add: Node.Add): String = declare(5) { (a, b, c, d, e) ->
            """
            |    %$a = load i8*, i8** %$ptr, align 8
            |    %$b = load i8, i8* %$a, align 1
            |    %$c = sext i8 %$b to i32
            |    %$d = add nsw i32 %$c, ${add.amount}
            |    %$e = trunc i32 %$d to i8
            |    store i8 %$e, i8* %$a, align 1
            """.trimMargin()
        }

        override fun visitZero(zero: Node.Zero): String = declare(1) { (a) ->
            """
            |%$a = load i8*, i8** %$ptr, align 8
            |store i8 0, i8* %$a, align 1
            """.trimMargin()
        }

        override fun visitLoop(loop: Node.Loop): String = declare(6) { (cond, a, b, c, d, body) ->
            """
            |    br label %$cond
            |$cond:
            |    %$a = load i8*, i8** %$ptr, align 8
            |    %$b = load i8, i8* %$a, align 1
            |    %$c = sext i8 %$b to i32
            |    %$d = icmp ne i32 %$c, 0
            |    br i1 %$d, label %$body, label %end$cond
            |$body:
            |${loop.bodyAccept(this, ::concat)}
            |    br label %$cond
            |end$cond:
            """.trimMargin()
        }

        override fun visitPrint(print: Node.Print): String = declare(4) { (a, b, c, d) ->
            """
            |    %$a = load i8*, i8** %$ptr, align 8
            |    %$b = load i8, i8* %$a, align 1
            |    %$c = sext i8 %$b to i32
            |    %$d = call i32 @putchar(i32 %$c)
            """.trimMargin()
        }

        override fun visitRead(read: Node.Read): String = declare(3) { (a, b, c) ->
            """
            |    %$a = call i32 @getchar()
            |    %$b = trunc i32 %$a to i8
            |    %$c = load i8*, i8** %$ptr, align 8
            |    store i8 %$b, i8* %$c, align 1
            """.trimMargin()
        }

        private fun <R> declare(n: Int, block: (List<Int>) -> R): R {
            this.n += n
            return block((this.n - n + 1..this.n).toList())
        }

        private operator fun <T> List<T>.component6() = this[5]

        private fun concat(a: String, b: String) = "$a\n$b"
    }
}
