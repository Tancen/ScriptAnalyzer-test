package org.example.analyzer.element;

import javafx.util.Pair;

public class UnaryOperationElement extends IElement
{
    /*
    一元运算元素的 name 为运算符
    * */

    public UnaryOperationElement(String displayName, String name, String hint)
    {
        this.displayName = displayName;
        this.name = name;
        this.hint = hint;
        this.params.add(new IElement.Param("值1", "值1", "", null));
    }

    @Override
    public int getType()
    {
        return ElementType.ELEMENT_TYPE_UNARY_OPERATION;
    }

    @Override
    public Pair<Boolean, String> checkParams(ClassDictionary classDictionary)
    {
        boolean success = false;
        String errString = "";

        do
        {
            if (params.get(0).value == null )
            {
                errString = "[" + line + ":" + column + "]: 元素1不能为空";
                break;
            }

            //重新递归检查params
            Pair<Boolean, String> err = params.get(0).value.checkParams(classDictionary);
            if (!err.getKey())
            {
                errString = err.getValue();
                break;
            }

            Class t = params.get(0).value.resultType;
            if (t == null || !(err = t.isAccept(name, "")).getKey())
            {
                errString = "[" + line + ":" + column + "]: 不能接受类型 '" + params.get(0).value.resultType.name + "' 作用于操作符 '" + name + "'";
                break;
            }

            this.resultType = classDictionary.lookup(err.getValue());
            assert (this.resultType != null);
            success = true;
        } while (false);

        return new Pair<>(success, errString);
    }

    public String toExpressionString()
    {
        String ret = name;
        //如果参数值不是一个函数且参数值有子参数, 需要补上()
        boolean l = false;
        if (! (params.get(0).value instanceof FunctionElement) && !params.get(0).value.params.isEmpty())
            l = true;

        if (l)
            ret += "(" ;
        ret += params.get(0).value.toExpressionString();
        if (l)
            ret += ")";

        return ret;
    }
}

