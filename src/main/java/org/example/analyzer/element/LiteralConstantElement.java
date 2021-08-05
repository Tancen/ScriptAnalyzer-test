package org.example.analyzer.element;

import javafx.util.Pair;

public class LiteralConstantElement extends IElement
{
    /*
    字面值的 name 为值本身
    * */
    public LiteralConstantElement(String name, String hint, Class resultType)
    {
        this.displayName = name;
        this.name = name;
        this.hint = hint;
        this.resultType = resultType;
    }

    @Override
    public int getType()
    {
        return ElementType.ELEMENT_TYPE_LITERAL_CONSTANT;
    }

    @Override
    public Pair<Boolean, String> checkParams(ClassDictionary classDictionary)
    {
        return new Pair<>(true, "");
    }

    public String toExpressionString()
    {
        return name;
    }
}
