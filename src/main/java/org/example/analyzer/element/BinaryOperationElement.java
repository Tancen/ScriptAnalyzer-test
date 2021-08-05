package org.example.analyzer.element;

import javafx.util.Pair;
import org.example.analyzer.Priority;

public class BinaryOperationElement extends IElement
{
    /*
    二元运算值的 name 为运算符
    * */
    public BinaryOperationElement(String displayName, String name, String hint)
    {
        this.displayName = displayName;
        this.name = name;
        this.hint = hint;
        this.params.add(new IElement.Param("值1", "值1", "", null));
        this.params.add(new IElement.Param("值2", "值2", "", null));
    }

    @Override
    public int getType()
    {
        return ElementType.ELEMENT_TYPE_BINARY_OPERATION;
    }

    @Override
    public Pair<Boolean, String> checkParams(ClassDictionary classDictionary)
    {
        boolean success = false;
        String errString = "";
        this.resultType = null;

        do
        {
            if (params.get(0).value == null)
            {
                errString = "值1不能为空";
                break;
            }

            if (params.get(1).value == null)
            {
                errString = "值2不能为空";
                break;
            }

            //重新递归检查params
            Pair<Boolean, String> err = params.get(0).value.checkParams(classDictionary);
            if (!err.getKey())
            {
                errString = err.getValue();
                break;
            }

            err = params.get(1).value.checkParams(classDictionary);
            if (!err.getKey())
            {
                errString = err.getValue();
                break;
            }

            Class t1 = params.get(0).value.resultType, t2 = params.get(1).value.resultType;
            assert (t1 != null && t2 != null);

            err = t1.isAccept(name, t2.name);
            if (!err.getKey())
            {
                errString = "[" + line + ":" + column + "]: 类型 '" + t1.name + "': 不能接受类型 '" + t2.name + "' 作用于操作符 '" + name + "'";
                break;
            }

            this.resultType = classDictionary.lookup(err.getValue());
            if (this.resultType == null)
            {
                errString = "[" + line + ":" + column + "]:未知类型 '" + err.getValue() + "'";
                break;
            }
            success = true;
        } while (false);

        return new Pair<Boolean, String>(success, errString);
    }

    public String toExpressionString()
    {
        //左右参数的优先级比自身高且不是函数、字面值的情况下需要补上括号
        String ret = "";
        boolean l = false, r = false;
        IElement v1 = params.get(0).value, v2 = params.get(1).value;
        if (!Priority.check(v1.name, name) && !(v1 instanceof FunctionElement) && !(v1 instanceof LiteralConstantElement))
            l = true;
        if (l)
            ret += "(";

        ret += params.get(0).value.toExpressionString();
        if (l)
            ret += ")";

        ret += " " + name + " ";
        if (!Priority.check(v2.name, name) && !(v2 instanceof FunctionElement) && !(v2 instanceof LiteralConstantElement))
            r = true;
        if (r)
            ret += "(";

        ret += params.get(1).value.toExpressionString();
        if (r)
            ret += ")";

        return ret;
    }
}
