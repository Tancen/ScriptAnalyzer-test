package org.example.analyzer.element;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
*   元素类型
*   name 为元素类型的名称, 比如字符串为 String, 数字为 Number, 函数为函数名全称(如 Math.add), + 为 + ..等
* */
public class Class
{
    static final public String TYPE_NUMBER = "Number";
    static final public String TYPE_STRING = "String";
    static final public String TYPE_BOOLEAN = "Boolean";

    public static class Accept
    {
        public Accept(String operation, String otherType, String resultType)
        {
            this.operation = operation;
            this.otherType = otherType;
            this.resultType = resultType;
        }

        public Accept()
        {
        }

        String operation;
        String otherType;
        String resultType;
    }

    String name;

    /*
    * mappingAccept记录该类型在某个操作符下是否能接受另外一个类型，且返回何种类型的数据
    * 一个类型在某个操作符下可接受的另一个类型可以是多个，比如 "1" + 2  和 "1" + "2" 在某些语言下都是成立的
    * 当操作符为空时, 表示是否可以进行直接(隐式)转换
    * 当操作符是一元运算符, 那么其它类型则为空
    * */
    Map<String /* 操作符 */, Map<String /*  另一个类型 */, String /* 接受后的返回类型 */ >> mappingAccept = new HashMap<>();

    public Class(String name, List<Accept> accepts)
    {
        this.name = name;

        for (Accept a : accepts)
        {
            Map<String, String> set = mappingAccept.get(a.operation);
            if (set == null)
            {
                set = new HashMap<>();
                mappingAccept.put(a.operation, set);
            }
            set.put(a.otherType, a.resultType);
        }
    }

    public Class(String name)
    {
        this(name, new ArrayList<Accept>());
    }

    public String getName()
    {
        return name;
    }

    public void addAccept(String operation, String otherType, String resultType)
    {
        Map<String, String> m = mappingAccept.get(operation);
        if (m == null)
        {
            m = new HashMap<>();
            mappingAccept.put(operation, m);
        }
        m.put(otherType, resultType);
    }

    public void removeAccept(String operation, String otherType)
    {
        Map<String, String> m = mappingAccept.get(operation);
        if (m != null)
            m.remove(otherType);
    }

    public Pair<Boolean, String> isAccept(String operation, String otherType)
    {
        Map<String, String> m = mappingAccept.get(operation);
        String t;
        if (m != null && (t = m.get(otherType)) != null)
        {
            return new Pair<>(true, t);
        }

        return new Pair<>(false, null);
    }

    public Pair<Boolean, String> isAccept(String otherType)
    {
        return isAccept("", otherType);
    }
}


