package org.example.analyzer.element;

import javafx.util.Pair;

import java.util.ArrayList;

public class FunctionElement extends IElement
{
    String className;
    String funcName;
    Leading leading = null;

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
    函数的name为 类名.函数名, 如 math.abs
    * */
    public FunctionElement(String displayName, String className, String funcName, String hint, ArrayList<Param> params, Class resultType)
    {
        this.className = className;
        this.funcName = funcName;
        this.displayName = displayName;
        this.name = buildName(className ,funcName);
        this.hint = hint;
        this.params = params == null ? new ArrayList<Param>() : params;
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

    public String getFuncName()
    {
        return funcName;
    }

    @Override
    public int getType()
    {
        return ElementType.ELEMENT_TYPE_FUNCTION;
    }

    @Override
    public Pair<Boolean, String> checkParams(ClassDictionary classDictionary)
    {
        boolean success = true;
        String errString = "";

        for (int i = 0; i < params.size(); i++)
        {
            Param param = params.get(i);

            if (param.value == null)
            {
                errString = "[" + line + ":" + column + "]: 函数 '"+  displayName + "' 参数 '" + param.displayName + "' 不能为空";
                success = false;
                break;
            }

            //重新递归检查params
            Pair<Boolean, String> err = param.value.checkParams(classDictionary);
            if (!err.getKey())
            {
                errString = err.getValue();
                success = false;
                break;
            }

            //获取当前参数需要接收的类型
            Class t = classDictionary.lookup(param.type);
            if (t == null)
            {
                errString = "[" + param.value.getLine() + ":" + param.value.getColumn() + "]: 函数 '"+  displayName + "' 参数 '" + param.displayName + "' 未知类型 '" + param.type + "'";
                success = false;
                break;
            }

            //检查参数是否能转换
            if (t != param.value.resultType)
            {
                errString = "[" + param.value.getLine()  + ":" + param.value.getColumn() + "]: 函数 '"+  displayName + "' 参数 '" + param.displayName + "' 不能接收类型 '" + param.value.resultType.getName() + "'";
                success = false;
                break;
            }
        }

        return new Pair<>(success, errString);
    }

    public String toExpressionString()
    {
        String ret = "";
        if (leading != null)
            ret += leading.toString() + ".";
        ret += funcName + "(";
        for (int i = 0; i < params.size(); i++)
        {
            ret += params.get(i).value.toExpressionString();
            if (i < params.size() - 1)
                ret += ", ";
        }
        ret += ")";
        return ret;
    }
}
