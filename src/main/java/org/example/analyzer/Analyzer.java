package org.example.analyzer;

import javafx.util.Pair;
import org.example.analyzer.element.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Analyzer
{
    public static Pair<IElement, String> toElement(String e, FunctionElementFactory funcFactory, VariableElementFactory varFactory, ClassDictionary classDictionary)
    {
        //词法解析
        SimpleLexicalAnalyzer.Scanner scanner = new SimpleLexicalAnalyzer.Scanner();
        char[] chars = new char[e.length() + 1];
        e.getChars(0, e.length(), chars, 0);
        chars[e.length()] = '\0';
        List<LexicalToken> tokens = new LinkedList<>();
        for (int i = 0; i < chars.length; i++)
        {
            SimpleLexicalAnalyzer.Scanner.WriteResult r = scanner.write(chars[i]);
            if (r.isOk)
            {
                if (r.lexeme != null)
                    tokens.add(r.lexeme);
                i -= r.retract;
            }
            else
            {
                return new Pair<>(null, "[" + r.line + ":" + r.position +  "]: 未识别标识符");
            }
        }

        SimpleSyntaxAnalyzer syntaxAnalyzer = new SimpleSyntaxAnalyzer();
        Pair<IElement, String> ret = syntaxAnalyzer.analyze(tokens, funcFactory, varFactory, classDictionary);
        if (ret.getKey() != null)
        {
            if (!syntaxAnalyzer.isFinished())
            {
                return new Pair<>(null, "表达式不完整");
            }
        }

        return ret;
    }
}
