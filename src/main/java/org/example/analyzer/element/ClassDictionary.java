package org.example.analyzer.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClassDictionary
{
    private Map<String, Class> types = new HashMap<String, Class>()  //key 为关键字名称, value 为关键字对应的类型
    {
        {
            Class t;
            t = new Class(Class.TYPE_NUMBER, new ArrayList<Class.Accept>()
            {
                {
                    add(new Class.Accept("~", "", Class.TYPE_NUMBER));
                    add(new Class.Accept("*", Class.TYPE_NUMBER, Class.TYPE_NUMBER));
                    add(new Class.Accept("/", Class.TYPE_NUMBER, Class.TYPE_NUMBER));
                    add(new Class.Accept("%", Class.TYPE_NUMBER, Class.TYPE_NUMBER));
                    add(new Class.Accept("+", Class.TYPE_NUMBER, Class.TYPE_NUMBER));
                    add(new Class.Accept("+", Class.TYPE_STRING, Class.TYPE_STRING));
                    add(new Class.Accept("-", "", Class.TYPE_NUMBER));
                    add(new Class.Accept("-", Class.TYPE_NUMBER, Class.TYPE_NUMBER));
                    add(new Class.Accept(">>", Class.TYPE_NUMBER, Class.TYPE_NUMBER));
                    add(new Class.Accept("<<", Class.TYPE_NUMBER, Class.TYPE_NUMBER));
                    add(new Class.Accept("&", Class.TYPE_NUMBER, Class.TYPE_NUMBER));
                    add(new Class.Accept("^", Class.TYPE_NUMBER, Class.TYPE_NUMBER));
                    add(new Class.Accept("|", Class.TYPE_NUMBER, Class.TYPE_NUMBER));
                    add(new Class.Accept(">", Class.TYPE_NUMBER, Class.TYPE_BOOLEAN));
                    add(new Class.Accept(">=", Class.TYPE_NUMBER, Class.TYPE_BOOLEAN));
                    add(new Class.Accept("<", Class.TYPE_NUMBER, Class.TYPE_BOOLEAN));
                    add(new Class.Accept("<=", Class.TYPE_NUMBER, Class.TYPE_BOOLEAN));
                    add(new Class.Accept("!=", Class.TYPE_NUMBER, Class.TYPE_BOOLEAN));
                    add(new Class.Accept("==", Class.TYPE_NUMBER, Class.TYPE_BOOLEAN));
                }
            });
            put(t.name, t);

            t = new Class(Class.TYPE_STRING, new ArrayList<Class.Accept>()
            {
                {
                    add(new Class.Accept("+", Class.TYPE_STRING, Class.TYPE_STRING));
                    add(new Class.Accept("+", Class.TYPE_NUMBER, Class.TYPE_STRING));
                }
            });
            put(t.name, t);

            t = new Class(Class.TYPE_BOOLEAN, new ArrayList<Class.Accept>()
            {
                {
                    add(new Class.Accept("||", Class.TYPE_BOOLEAN, Class.TYPE_BOOLEAN));
                    add(new Class.Accept("&&", Class.TYPE_BOOLEAN, Class.TYPE_BOOLEAN));
                    add(new Class.Accept("!", "", Class.TYPE_BOOLEAN));
                }
            });
            put(t.name, t);
        }
    };

    public ClassDictionary()
    {

    }

    public void add(Class c)
    {
        types.put(c.name, c);
    }

    public Class lookup(String name)
    {
        return types.get(name);
    }

    public Class arrayOf(String name)
    {
        Class t = types.get(name);
        if (t != null)
        {
            Class ret = new Class(t.name + "[]", new ArrayList<Class.Accept>());
            return ret;
        }

        return null;
    }
}
