package org.example.analyzer.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FunctionElementFactory
{
    private static Map<String, FunctionElement> definitions = new HashMap<>();

    public FunctionElementFactory()
    {

    }

    public void registerFunctionElement(String displayName, String className, String funcName, String hint, ArrayList<IElement.Param> params, Class resultType)
    {
        FunctionElement e = new FunctionElement(displayName, className,funcName, hint, params, resultType);
        definitions.put(e.name, e);
    }

    public FunctionElement create(String className, String funcName)
    {
        FunctionElement e = definitions.get(FunctionElement.buildName(className ,funcName));
        if (e == null)
            return null;

        ArrayList<IElement.Param> params = new ArrayList<>();
        for (IElement.Param param : e.params)
        {
            params.add(new IElement.Param(param.displayName, param.hint, param.type, null));
        }
        FunctionElement ret = new FunctionElement(e.displayName, e.className, e.funcName, e.hint, params, e.resultType);
        return  ret;
    }
}
