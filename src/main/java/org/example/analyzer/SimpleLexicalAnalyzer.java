package org.example.analyzer;


public class SimpleLexicalAnalyzer
{
    public static class Scanner
    {
        private State m_state = State.IDLE;
        private int m_position = -1;
        private int m_startPosition = -1;
        private int m_line = 1;
        private int m_length = 0;
        private StringBuffer m_value = new StringBuffer();

        public Scanner()
        {

        }

        public static class WriteResult
        {
            public boolean isOk = false;
            public int retract = 0;
            public int line = 0;
            public int position = 0;
            public LexicalToken lexeme;
        }

        private enum State {
            IDLE,
            ID,
            SIGN_DOUBLE_QUOTATION, // "
            SIGN_SINGLE_QUOTATION, // '

            NUMBER_LEADING_ZERO, NUMBER_NUMBER0, NUMBER_DOT, NUMBER_NUMBER1, NUMBER_E, NUMBER_INTEGER, NUMBER_X, NUMBER_HEX,
        }

        private void append(char ch)
        {
            m_value.append(ch);
            m_length++;
        }

        private LexicalToken done()
        {
            LexicalToken l = new LexicalToken();
            l.length = m_length;
            l.position = m_startPosition;
            l.line = m_line;
            l.value = m_value.toString();

            m_length = 0;
            m_value.setLength(0);
            m_state = State.IDLE;
            return l;
        }

        private LexicalToken done(LexicalTokenType type)
        {
            LexicalToken l = done();
            l.type = type;
            return l;
        }

        public WriteResult write(char ch)
        {
            m_position++;

            WriteResult ret = new WriteResult();
            ret.isOk = true;
            ret.retract = 0;

            switch (m_state)
            {
                case IDLE:
                    if (ch == '_' || Character.isLetter(ch))
                    {
                        m_state = State.ID;
                        m_startPosition = m_position;
                        append(ch);
                    }
                    else if (ch == '0')
                    {
                        m_state = State.NUMBER_LEADING_ZERO;
                        m_startPosition = m_position;
                        append(ch);
                    }
                    else if (Character.isDigit(ch))
                    {
                        m_state = State.NUMBER_NUMBER0;
                        m_startPosition = m_position;
                        append(ch);
                    }
                    else if (ch == '\'')
                    {
                        m_state = State.SIGN_SINGLE_QUOTATION;
                        m_startPosition = m_position;
                    }
                    else if (ch == '\"')
                    {
                        m_state = State.SIGN_DOUBLE_QUOTATION;
                        m_startPosition = m_position;
                    }
                    else if (ch == ' ' || ch == '\t' || ch == '\r')
                    {
                        break;
                    }
                    else if (ch == '\n')
                    {
                        m_line++;
                        m_position = -1;
                        break;
                    }
                    else if (ch == '\0')
                    {
                        m_startPosition = m_position;
                        append(ch);
                        ret.lexeme = done(LexicalTokenType.END);
                        break;
                    }
                    else
                    {
                        LexicalTokenType type = LexicalTokenType.UNKNOWN;
                        ret.isOk = true;
                        switch(ch)
                        {
                            case '+':
                                type = LexicalTokenType.SIGN_ADD;
                                break;

                            case '-':
                                type = LexicalTokenType.SIGN_MINUS;
                                break;

                            case '*':
                                type = LexicalTokenType.SIGN_ASTERISK;
                                break;

                            case '/':
                                type = LexicalTokenType.SIGN_SLASH;
                                break;

                            case '<':
                                type = LexicalTokenType.SIGN_LESS;
                                break;

                            case '>':
                                type = LexicalTokenType.SIGN_GREATER;
                                break;

                            case '=':
                                type = LexicalTokenType.SIGN_EQUALS;
                                break;

                            case '!':
                                type = LexicalTokenType.SIGN_EXCLAMDOWN;
                                break;

                            case '&':
                                type = LexicalTokenType.SIGN_AMPERSAND;
                                break;

                            case '|':
                                type = LexicalTokenType.SIGN_VERTICAL_BAR;
                                break;

                            case '^':
                                type = LexicalTokenType.SIGN_CARET;
                                break;

                            case '.':
                                type = LexicalTokenType.SIGN_DOT;
                                break;

                            case ',':
                                type = LexicalTokenType.SIGN_COMMA;
                                break;

                            case '~':
                                type = LexicalTokenType.SIGN_TILDE;
                                break;

                            case '(':
                                type = LexicalTokenType.SIGN_PAREN_LEFT;
                                break;

                            case ')':
                                type = LexicalTokenType.SIGN_PAREN_RIGHT;
                                break;

                            case '%':
                                type = LexicalTokenType.SIGN_PERCENT;
                                break;

                            case ';':
                                type = LexicalTokenType.SIGN_SEMICOLON;
                                break;

                            default:
                                ret.isOk = false;
                                break;
                        }
                        if (ret.isOk)
                        {
                            m_startPosition = m_position;
                            append(ch);
                            ret.lexeme = done(type);
                        }
                        else
                            done();
                    }
                    break;

                case SIGN_SINGLE_QUOTATION:
                    if (ch == '\'')
                    {
                        if (m_value.charAt(m_value.length() - 1) != '\\')
                        {
                            ret.lexeme = done(LexicalTokenType.STRING);
                            break;
                        }
                        append(ch);
                    }
                    else if (ch != '\0')
                    {
                        append(ch);
                    }
                    else
                    {
                        done();
                        ret.isOk = false;
                    }
                    break;

                case SIGN_DOUBLE_QUOTATION:
                    if (ch == '\"')
                    {
                        if (m_value.charAt(m_value.length() - 1) != '\\')
                        {
                            ret.lexeme = done(LexicalTokenType.STRING);
                            break;
                        }
                        append(ch);
                    }
                    else if (ch != '\0')
                    {
                        append(ch);
                    }
                    else
                    {
                        done();
                        ret.isOk = false;
                    }
                    break;

                case ID:
                    if (ch == '_' || Character.isLetter(ch) || Character.isDigit(ch))
                    {
                        append(ch);
                    }
                    else
                    {
                        ret.lexeme = done(LexicalTokenType.ID);
                        ret.retract = 1;
                    }
                    break;

                case NUMBER_LEADING_ZERO:
                    if (ch == 'x' || ch == 'X')
                    {
                        append(ch);
                        m_state = State.NUMBER_X;
                    }
                    else if (Character.isDigit(ch))
                    {
                        append(ch);
                        m_state = State.NUMBER_NUMBER0;
                    }
                    else if (ch == '.')
                    {
                        append(ch);
                        m_state = State.NUMBER_DOT;
                    }
                    else
                    {
                        ret.lexeme = done(LexicalTokenType.NUMBER);
                        ret.retract = 1;
                    }
                    break;

                case NUMBER_X:
                    if (Character.isDigit(ch) || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'))
                    {
                        append(ch);
                        m_state = State.NUMBER_HEX;
                    }
                    else
                    {
                        done();
                        ret.isOk = false;
                    }

                    break;

                case NUMBER_HEX:
                    if (Character.isDigit(ch) || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'))
                    {
                        append(ch);
                    }
                    else
                    {
                        ret.lexeme = done(LexicalTokenType.NUMBER);
                        ret.retract = 1;
                    }
                    break;

                case NUMBER_NUMBER0:
                    if (Character.isDigit(ch))
                    {
                        append(ch);
                    }
                    else if (ch == '.')
                    {
                        append(ch);
                        m_state = State.NUMBER_DOT;
                    }
                    else
                    {
                        ret.lexeme = done(LexicalTokenType.NUMBER);
                        ret.retract = 1;
                    }
                    break;

                case NUMBER_DOT:
                    if (Character.isDigit(ch))
                    {
                        append(ch);
                        m_state = State.NUMBER_NUMBER1;
                    }
                    else
                    {
                        done();
                        ret.isOk = false;
                    }
                    break;

                case NUMBER_NUMBER1:
                    if (Character.isDigit(ch))
                    {
                        append(ch);
                    }
                    else if (ch == 'e' || ch == 'E')
                    {
                        append(ch);
                        m_state = State.NUMBER_E;
                    }
                    else
                    {
                        ret.lexeme = done(LexicalTokenType.NUMBER);
                        ret.retract = 1;
                    }
                    break;

                case NUMBER_E:
                    if (ch == '+' || ch == '-' || Character.isDigit(ch))
                    {
                        append(ch);
                        m_state = State.NUMBER_INTEGER;
                    }
                    else
                    {
                        done();
                        ret.isOk = false;
                    }
                    break;

                case NUMBER_INTEGER:
                    if (Character.isDigit(ch))
                    {
                        append(ch);
                    }
                    else
                    {
                        ret.lexeme = done(LexicalTokenType.NUMBER);
                        ret.retract = 1;
                    }
                    break;
            }

            m_position -= ret.retract;
            ret.line = m_line;
            ret.position = m_position;

            return ret;
        }
    }


}
