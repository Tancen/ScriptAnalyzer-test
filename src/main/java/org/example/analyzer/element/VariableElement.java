package org.example.analyzer.element;

import javafx.util.Pair;

import java.util.HashMap;
import java.util.Map;

public class VariableElement extends IElement
{
    String className;
    String varName;
    Leading leading = null;

    private static Map<String, VariableElement> s_definitions = new HashMap<String, VariableElement>()
    {
        {

        }
    };

    public interface Leading
    {
        String toString();
    };

    static public class StringLeading implements Leading
    {
        private String str;

        public StringLeading(String str)
        {
            this.str = str;
        }

        public String toString()
        {
            return str;
        }
    }

    static public class ExpressionLeading implements Leading
    {
        private IElement e;

        public ExpressionLeading(IElement e)
        {
            this.e = e;
        }

        public String toString()
        {
            return e.toExpressionString();
        }
    }

    /*
    变量的name为 类名.变量名, 如 math.PI
    className 必须在 ElementType 中注册
    * */
    public VariableElement(String displayName, String className, String varName, String hint, Class resultType)
    {
        this.className = className;
        this.varName = varName;
        this.displayName = displayName;
        this.name = buildName(className ,varName);
        this.hint = hint;
        this.resultType = resultType;
    }

    static String buildName(String className, String funcName)
    {
        if (className.isEmpty())
            return funcName;

        return className + "." + funcName;
    }

    public void setLeading(Leading leading)
    {
        this.leading = leading;
    }

    public String getClassName()
    {
        return className;
    }

    public String getVarName()
    {
        return varName;
    }

    @Override
    public int getType()
    {
        return ElementType.ELEMENT_TYPE_VARIABLE;
    }

    @Override
    public Pair<Boolean, String> checkParams(ClassDictionary classDictionary)
    {
        return new Pair<>(true, "");
    }

    public String toExpressionString()
    {
        String ret = "";
        if (leading != null)
            ret += leading.toString() + ".";
        ret += varName;
        return ret;
    }
}