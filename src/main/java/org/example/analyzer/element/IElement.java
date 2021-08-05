package org.example.analyzer.element;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import javafx.util.Pair;
import java.util.ArrayList;

public abstract class IElement
{
    static public class Param
    {
        public Param() { }
        public Param(String displayName, String hint, String type, IElement value)
        {
            this.displayName = displayName;
            this.hint = hint;
            this.type = type;
            this.value = value;
        }

        public Param(String displayName, String hint, String type)
        {
            this(displayName, hint, type, null);
        }

        public String hint = "";       //参数提示
        public String type = "";       //参数接收的类型
        public String displayName = "";    //显示名称
        public IElement value = null;          //值
    }

    protected Class resultType = null;      //结果类型
    protected String displayName = "";            //显示名称, 如 Math.add 显示名称为加法
    protected String name = "";                   //名称, 如 Math.add
    protected String hint = "";                   // 提示, 如 Math.add 提示为 将元素1的值与元素2的值相加
    protected int line = 0;
    protected int column = 0;

    protected ArrayList<Param> params = new ArrayList<>();

    public abstract int getType();

    public Class getResultType()
    {
        return resultType;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getName()
    {
        return name;
    }

    public String getHint()
    {
        return hint;
    }

    public int getLine()
    {
        return line;
    }

    public void setLine(int line)
    {
        this.line = line;
    }

    public int getColumn()
    {
        return column;
    }

    public void setColumn(int column)
    {
        this.column = column;
    }

    public ArrayList<Param> getParams()
    {
        return params;
    }

    public boolean setResultType(Class type)
    {
        this.resultType = type;
        return true;
    }

    public boolean setParam(int index, IElement v)
    {
        /*
        虽然参数的值类型可能被限定, 但是在值表达式还未完全录入完毕时,是无法确定值的类型的.
        因此在此处不校验值类型, 由 checkParams 校验值是否正确
        */

        if (index < 0 || index >= params.size())
            return false;

        params.get(index).value = v;
        return true;
    }

    //检查参数是否合法, 返回是否合法及错误提示
    public abstract Pair<Boolean, String> checkParams(ClassDictionary classDictionary);

    //转成表达式
    public abstract String toExpressionString();

    public String toJson()
    {
        JSONObject root = new JSONObject();

        toJson(this, root);

        return root.toJSONString();
    }

    private void toJson(IElement e, JSONObject dst)
    {
        dst.put("type", e.getType());
        dst.put("name", e.getName());
        dst.put("displayName", e.getDisplayName());
        dst.put("hint", e.getHint());
        dst.put("resultType", e.getResultType().getName());

        JSONArray dstParams = new JSONArray();
        ArrayList<IElement.Param> params = e.getParams();
        for (IElement.Param param : params)
        {
            JSONObject dstParam = new JSONObject();
            dstParam.put("displayName", param.displayName);
            dstParam.put("hint", param.hint);
            dstParam.put("type", param.type);
            if (param.value != null)
            {
                JSONObject dstParamValue = new JSONObject();
                toJson(param.value, dstParamValue);
                dstParam.put("value", dstParamValue);
            }
            dstParams.add(dstParam);
        }

        dst.put("params", dstParams);
    }
}

