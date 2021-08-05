package org.example.analyzer;

public enum LexicalTokenType
{
    UNKNOWN,
    ID,       //id
    SIGN_ADD,       //加
    SIGN_MINUS,     //减
    SIGN_ASTERISK,  // 星号 *
    SIGN_SLASH,     // 斜杠 /
    SIGN_LESS,    // <
    SIGN_GREATER,   // >
    SIGN_EQUALS,    // =
    SIGN_EXCLAMDOWN, // !
    SIGN_AMPERSAND, // &
    SIGN_VERTICAL_BAR, // |
    SIGN_CARET, // ^
    SIGN_DOT,   // .
    SIGN_COMMA,   // ,
    SIGN_TILDE,   // ~
    SIGN_PAREN_LEFT, // (
    SIGN_PAREN_RIGHT, // )
    SIGN_PERCENT, // %
    SIGN_SEMICOLON, // ;
    NUMBER,    //数字
    STRING,         //字符串
    END         //文法输入结束
}
