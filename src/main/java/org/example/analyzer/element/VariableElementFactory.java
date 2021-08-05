package org.example.analyzer.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VariableElementFactory
{
    private static Map<String, VariableElement> definitions = new HashMap<>();

    public VariableElementFactory()
    {

    }

    public void registerVariableElement(String displayName, String className, String varName, String hint, Class resultType)
    {
        VariableElement e = new VariableElement(displayName, className,varName, hint, resultType);
        definitions.put(e.name, e);
    }

    public VariableElement create(String className, String varName)
    {
        VariableElement e = definitions.get(VariableElement.buildName(className ,varName));
        if (e == null)
            return null;

        VariableElement ret = new VariableElement(e.displayName, e.className, e.varName, e.hint, e.resultType);
        return  ret;
    }
}
