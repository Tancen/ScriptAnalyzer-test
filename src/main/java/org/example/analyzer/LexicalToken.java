package org.example.analyzer;

public class LexicalToken
{
    public LexicalTokenType type;
    public int line = 0;
    public int position = -1;
    public int length = 0;
    public String value = "";
}