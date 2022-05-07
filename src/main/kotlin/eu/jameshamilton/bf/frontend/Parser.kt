package eu.jameshamilton.bf.frontend

import eu.jameshamilton.bf.frontend.TokenType.*

class Parser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): Node.Program {
        val statements = mutableListOf<Node>()
        while (!isAtEnd()) {
            statements.add(instruction())
        }
        return Node.Program(statements)
    }

    private fun instruction(): Node = when {
        match(LEFT_BRACKET) -> {
            val body = mutableListOf<Node>()
            while (!check(RIGHT_BRACKET) && !isAtEnd()) {
                body += instruction()
            }
            consume(RIGHT_BRACKET, "Expected matching ']'.")
            // [+] or [-] zero out the current memory cell, so can be
            // replaced by a specific zero-ing instruction.
            if (body.size == 1 && body.single() is Node.Add) {
                Node.Zero
            } else {
                Node.Loop(body)
            }
        }
        match(RIGHT_BRACKET) -> throw error(previous(), "Unexpected ']'. ")
        match(GREATER) -> Node.Move(1)
        match(LESS) -> Node.Move(-1)
        match(PLUS) -> Node.Add(1)
        match(MINUS) -> Node.Add(-1)
        match(DOT) -> Node.Print
        match(COMMA) -> Node.Read
        else -> throw error(peek(), "Expected bf command.")
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()

        throw error(peek(), message)
    }

    private fun error(token: Token, message: String): ParseError {
        return ParseError(message, token.line)
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) if (check(type)) {
            advance()
            return true
        }
        return false
    }

    private fun check(type: TokenType): Boolean = when {
        isAtEnd() -> false
        else -> peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd() = peek().type == EOF

    private fun peek(): Token = tokens[current]

    private fun previous(): Token = tokens[current - 1]

    class ParseError(override val message: String, val line: Int) : RuntimeException(message)
}
