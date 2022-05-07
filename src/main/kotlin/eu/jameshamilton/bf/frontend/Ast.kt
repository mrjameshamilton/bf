package eu.jameshamilton.bf.frontend

import kotlin.math.abs

sealed class Node {
    open fun <T> accept(visitor: Visitor<T>) = when (this) {
        is Move -> visitor.visitMove(this)
        is Add -> visitor.visitAdd(this)
        is Zero -> visitor.visitZero(this)
        is Loop -> visitor.visitLoop(this)
        is Print -> visitor.visitPrint(this)
        is Read -> visitor.visitRead(this)
        is Program -> visitor.visitProgram(this)
    }

    interface Visitor<R> {
        fun visitMove(move: Move): R
        fun visitAdd(add: Add): R
        fun visitZero(zero: Zero): R
        fun visitLoop(loop: Loop): R
        fun visitPrint(print: Print): R
        fun visitRead(read: Read): R
        fun visitProgram(program: Program): R
    }

    class Program(val body: List<Node>) : Node() {
        fun <T> bodyAccept(visitor: Visitor<T>) =
            body.forEach { it.accept(visitor) }

        fun <T> bodyAccept(visitor: Visitor<T>, merge: (T, T) -> T): T? =
            body.map { it.accept(visitor) }.reduceOrNull(merge)
    }

    class Loop(val body: List<Node>) : Node() {
        fun <T> bodyAccept(visitor: Visitor<T>) =
            body.forEach { it.accept(visitor) }

        fun <T> bodyAccept(visitor: Visitor<T>, merge: (T, T) -> T): T? =
            body.map { it.accept(visitor) }.reduceOrNull(merge)
    }

    class Move(val amount: Int) : Node()
    class Add(val amount: Int) : Node()
    object Zero : Node()
    object Print : Node()
    object Read : Node()
}

class AstPrinter : Node.Visitor<String> {
    override fun visitMove(move: Node.Move): String = (if (move.amount < 0) "<" else ">").repeat(abs(move.amount))

    override fun visitAdd(add: Node.Add): String = (if (add.amount < 0) "-" else "+").repeat(abs(add.amount))

    override fun visitZero(zero: Node.Zero): String = "[+]"

    override fun visitLoop(loop: Node.Loop): String = "[${loop.body.joinToString(separator = "") { it.accept(this) }}]"

    override fun visitPrint(print: Node.Print): String = "."

    override fun visitRead(read: Node.Read): String = (",")

    override fun visitProgram(program: Node.Program): String =
        program.body.joinToString(separator = "") { it.accept(this) }
}
