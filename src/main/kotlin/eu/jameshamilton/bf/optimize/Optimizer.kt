package eu.jameshamilton.bf.optimize

import eu.jameshamilton.bf.frontend.Node

class Optimizer {

    fun optimize(program: Node.Program, passes: Int = 1): Node.Program {
        var result = program
        for (pass in 0 until passes) {
            result = result.accept(Optimizer()) as Node.Program
        }
        return result
    }

    private class Optimizer : Node.Visitor<Node> {

        override fun visitProgram(program: Node.Program): Node = Node.Program(
            removeUnreachableLoops(
                shrink(program.body).map { it.accept(this) }
            )
        )

        override fun visitMove(move: Node.Move): Node = move

        override fun visitAdd(add: Node.Add): Node = add

        override fun visitZero(zero: Node.Zero): Node = zero

        override fun visitLoop(loop: Node.Loop): Node =
            Node.Loop(shrink(loop.body).map { it.accept(this) })

        override fun visitPrint(print: Node.Print): Node = print

        override fun visitRead(read: Node.Read): Node = read

        private fun shrink(list: List<Node>): List<Node> = list.fold(mutableListOf<Node>()) { acc, node ->
            val previous = acc.lastOrNull()
            when {
                // consecutive zero-ing instructions can be removed
                previous is Node.Zero && node is Node.Zero -> {}
                // merge consecutive Add and Move instructions
                previous is Node.Add && node is Node.Add ->
                    acc[acc.lastIndex] = Node.Add(previous.amount + node.amount)
                previous is Node.Move && node is Node.Move ->
                    acc[acc.lastIndex] = Node.Move(previous.amount + node.amount)
                else -> acc.add(node)
            }
            acc
        }.filterNot {
            when (it) {
                is Node.Add -> it.amount == 0
                is Node.Move -> it.amount == 0
                is Node.Loop -> it.body.isEmpty()
                else -> false
            }
        }

        private fun removeUnreachableLoops(body: List<Node>): List<Node> {
            // If there are no memory updates before a top-level loop, it won't be
            // executed, since it will always jump to the end of the loop.
            val firstMemoryUpdate = body.indexOfFirst { it is Node.Add || it is Node.Read }

            return body
                .filterIndexed { index, node -> !(node is Node.Loop && (index < firstMemoryUpdate || firstMemoryUpdate == -1)) }
                .filterIndexed { index, node -> !(node is Node.Zero && (index < firstMemoryUpdate || firstMemoryUpdate == -1)) }
        }
    }
}
