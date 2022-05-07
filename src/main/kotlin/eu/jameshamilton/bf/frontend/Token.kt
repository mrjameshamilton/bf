package eu.jameshamilton.bf.frontend

data class Token(val type: TokenType, val line: Int)

enum class TokenType {
    LEFT_BRACKET,
    RIGHT_BRACKET,
    PLUS,
    MINUS,
    COMMA,
    DOT,
    LESS,
    GREATER,
    EOF
}
