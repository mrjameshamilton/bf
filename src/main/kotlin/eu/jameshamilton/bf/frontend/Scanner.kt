package eu.jameshamilton.bf.frontend

import eu.jameshamilton.bf.frontend.TokenType.*

class Scanner(private val source: String) {

    private var start = 0
    private var current = 0
    private var line = 1
    private val tokens = mutableListOf<Token>()

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }

        tokens.add(Token(EOF, line))
        return tokens
    }

    private fun scanToken() {
        when (advance()) {
            '[' -> addToken(LEFT_BRACKET)
            ']' -> addToken(RIGHT_BRACKET)
            ',' -> addToken(COMMA)
            '.' -> addToken(DOT)
            '-' -> addToken(MINUS)
            '+' -> addToken(PLUS)
            '<' -> addToken(LESS)
            '>' -> addToken(GREATER)
            '\n' -> line++
        }
    }

    private fun addToken(type: TokenType) =
        tokens.add(Token(type, line))

    private fun advance(): Char = source[current++]

    private fun isAtEnd() = current >= source.length
}
