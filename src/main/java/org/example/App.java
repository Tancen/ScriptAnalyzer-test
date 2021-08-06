package org.example;

import javafx.util.Pair;
import org.example.analyzer.Analyzer;
import org.example.analyzer.element.*;
import org.example.analyzer.element.Class;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class App
{
    static void test()
    {
        FunctionElementFactory funcFactory = new FunctionElementFactory();
        VariableElementFactory varFactory = new VariableElementFactory();
        ClassDictionary classDictionary = new ClassDictionary();
        init(funcFactory, varFactory, classDictionary);

        String strings[] = {
                "v +3  > math.sin(0.8 + 0x55)",
                "getQuantity(1, 2, 3, startTime, endTime) * 3",
                "(v + 2) * 3 > 100 || (v / 3) != 4",
                "(v + 2) * '3'",
                "math.sin(0.8 + \"0x55\")",
                "v - -v + 2"
        };

        for (String s : strings)
        {
            System.out.println("testing : " + s);
            Pair<IElement, String> result = Analyzer.toElement(s, funcFactory, varFactory, classDictionary);
            if (result.getKey() == null)
            {
                System.out.println(result.getValue());
            }
            else
            {
                System.out.println(result.getKey().toJson());
                System.out.println(result.getKey().toExpressionString());
            }
            System.out.println("\n");
        }
    }

    static void init(FunctionElementFactory funcFactory, VariableElementFactory varFactory, ClassDictionary classDictionary)
    {
        String classQuantity = "Quantity";
        String classSamplePointList = "SamplePointList";
        String classMath = "math";

        //ClassDictionary
        Class c = classDictionary.lookup(Class.TYPE_NUMBER);
        c.addAccept("+", classQuantity, classQuantity);
        c.addAccept("-", classQuantity, classQuantity);
        c.addAccept("*", classQuantity, classQuantity);
        c.addAccept("/", classQuantity, classQuantity);
        c.addAccept("%", classQuantity, classQuantity);

        c = new Class(classQuantity);
        c.addAccept("+", Class.TYPE_NUMBER, classQuantity);
        c.addAccept("-", Class.TYPE_NUMBER, classQuantity);
        c.addAccept("*", Class.TYPE_NUMBER, classQuantity);
        c.addAccept("-", Class.TYPE_NUMBER, classQuantity);
        classDictionary.add(c);

        c = new Class(classSamplePointList);
        classDictionary.add(c);

        c = new Class(classMath);
        classDictionary.add(c);

        ArrayList<IElement.Param> params;
        FunctionElement e;

        //funcFactory
        //getQuantity
        params = new ArrayList<>();
        params.add(new IElement.Param("测量点id", "测量点id, 0表示本测量点", Class.TYPE_NUMBER));
        params.add(new IElement.Param("测量编码", "测量编码", Class.TYPE_NUMBER));
        params.add(new IElement.Param("测量编号", "测量编号", Class.TYPE_NUMBER));
        params.add(new IElement.Param("开始时间", "开始时间", Class.TYPE_STRING));
        params.add(new IElement.Param("结束时间", "结束时间", Class.TYPE_STRING,  null));
        funcFactory.registerFunctionElement("查询测量点用量", "","getQuantity", "从一个测量点中查询用量", params, classDictionary.lookup(classQuantity));

        //getMeasuringValue
        params = new ArrayList<>();
        params.add(new IElement.Param("测量点id", "测量点id, 0表示本测量点", Class.TYPE_NUMBER));
        params.add(new IElement.Param("测量编码", "测量编码", Class.TYPE_NUMBER));
        params.add(new IElement.Param("测量编号", "测量编号", Class.TYPE_NUMBER));
        params.add(new IElement.Param("开始时间", "开始时间", Class.TYPE_STRING));
        params.add(new IElement.Param("结束时间", "结束时间", Class.TYPE_STRING,  null));
        params.add(new IElement.Param("分组规则", "分组规则, 值0表示全部,1表示时,2表示天,3表示月,4表示年", Class.TYPE_NUMBER,  null));
        funcFactory.registerFunctionElement("查询采集数据", "","getMeasuringValue", "根据分组查询采集数据, 当分组规则不为0时, 每个分组仅返回第一条数据",
                params, classDictionary.lookup(classSamplePointList));

        //getMeasureParamValue
        params = new ArrayList<>();
        params.add(new IElement.Param("测量点id", "测量点id, 0表示本测量点", Class.TYPE_NUMBER));
        params.add(new IElement.Param("属性名称", "属性名称", Class.TYPE_STRING));
        funcFactory.registerFunctionElement("查询测量点属性值", "","getMeasureParamValue", "查询测量点属性值", params, classDictionary.lookup(Class.TYPE_STRING));

        //getQuantityByContainer
        params = new ArrayList<>();
        params.add(new IElement.Param("测量点id", "测量点id, 0表示本测量点", Class.TYPE_NUMBER));
        params.add(new IElement.Param("容器名称", "容器名称", Class.TYPE_STRING));
        params.add(new IElement.Param("测量项名称", "测量项名称", Class.TYPE_STRING));
        params.add(new IElement.Param("开始时间", "开始时间", Class.TYPE_STRING));
        params.add(new IElement.Param("结束时间", "结束时间", Class.TYPE_STRING,  null));
        funcFactory.registerFunctionElement("根据容器查询测量点用量", "","getQuantityByContainer", "根据容器查询测量点用量", params, classDictionary.lookup(classQuantity));

        //getRentalAreaProperty
        params = new ArrayList<>();
        params.add(new IElement.Param("场地id", "场地id, 场地id", Class.TYPE_NUMBER));
        params.add(new IElement.Param("属性名称", "属性名称", Class.TYPE_STRING));
        funcFactory.registerFunctionElement("获取指定场地的属性值", "","getRentalAreaProperty", "获取指定场地的属性值", params, classDictionary.lookup(Class.TYPE_STRING));

        /* math */
        //math.abs
        params = new ArrayList<>();
        params.add(new IElement.Param("值", "值", Class.TYPE_NUMBER));
        funcFactory.registerFunctionElement("求绝对值", "math","abs", "求绝对值", params, classDictionary.lookup(Class.TYPE_NUMBER));

        //math.round(d)
        params = new ArrayList<>();
        params.add(new IElement.Param("值", "值", Class.TYPE_NUMBER));
        funcFactory.registerFunctionElement("四舍五入", "math","round", "四舍五入", params, classDictionary.lookup(Class.TYPE_NUMBER));

        //math.floor(d)
        params = new ArrayList<>();
        params.add(new IElement.Param("值", "值", Class.TYPE_NUMBER));
        funcFactory.registerFunctionElement("向下取整", "math","floor", "向下取整", params, classDictionary.lookup(Class.TYPE_NUMBER));

        //math.ceil(d)
        params = new ArrayList<>();
        params.add(new IElement.Param("值", "值", Class.TYPE_NUMBER));
        funcFactory.registerFunctionElement("向上取整", "math","ceil", "向上取整", params, classDictionary.lookup(Class.TYPE_NUMBER));

        //math.sqrt(d)
        params = new ArrayList<>();
        params.add(new IElement.Param("值", "值", Class.TYPE_NUMBER));
        funcFactory.registerFunctionElement("求平方根", "math","sqrt", "求平方根", params, classDictionary.lookup(Class.TYPE_NUMBER));

        //math.pow(d1,d2)
        params = new ArrayList<>();
        params.add(new IElement.Param("值1", "值2", Class.TYPE_NUMBER));
        params.add(new IElement.Param("值1", "值2", Class.TYPE_NUMBER));
        funcFactory.registerFunctionElement("求次方", "math","pow", "求次方", params, classDictionary.lookup(Class.TYPE_NUMBER));

        //math.log(d)
        params = new ArrayList<>();
        params.add(new IElement.Param("值", "值", Class.TYPE_NUMBER));
        funcFactory.registerFunctionElement("求自然对数", "math","log", "求自然对数", params, classDictionary.lookup(Class.TYPE_NUMBER));

        //math.log10(d)
        params = new ArrayList<>();
        params.add(new IElement.Param("值", "值", Class.TYPE_NUMBER));
        funcFactory.registerFunctionElement("求10为底的对数", "math","log10", "求10为底的对数", params, classDictionary.lookup(Class.TYPE_NUMBER));

        //math.sin(d)
        params = new ArrayList<>();
        params.add(new IElement.Param("值", "值", Class.TYPE_NUMBER));
        funcFactory.registerFunctionElement("正弦函数", "math","sin", "正弦函数", params, classDictionary.lookup(Class.TYPE_NUMBER));

        //math.cos(d)
        params = new ArrayList<>();
        params.add(new IElement.Param("值", "值", Class.TYPE_NUMBER));
        funcFactory.registerFunctionElement("余弦函数", "math","cos", "余弦函数", params, classDictionary.lookup(Class.TYPE_NUMBER));

        //math.tan(d)
        params = new ArrayList<>();
        params.add(new IElement.Param("值", "值", Class.TYPE_NUMBER));
        funcFactory.registerFunctionElement("正切函数", "math","tan", "正切函数", params, classDictionary.lookup(Class.TYPE_NUMBER));

        //math.atan(d)
        params = new ArrayList<>();
        params.add(new IElement.Param("值", "值", Class.TYPE_NUMBER));
        funcFactory.registerFunctionElement("反正切函数", "math","atan", "反正切函数", params, classDictionary.lookup(Class.TYPE_NUMBER));

        //math.acos(d)
        params = new ArrayList<>();
        params.add(new IElement.Param("值", "值", Class.TYPE_NUMBER));
        funcFactory.registerFunctionElement("反余弦函数", "math","acos", "反余弦函数", params, classDictionary.lookup(Class.TYPE_NUMBER));

        //math.asin(d)
        params = new ArrayList<>();
        params.add(new IElement.Param("值", "值", Class.TYPE_NUMBER));
        funcFactory.registerFunctionElement("反正弦函数", "math","asin", "反正弦函数", params, classDictionary.lookup(Class.TYPE_NUMBER));


        //varFactory
        varFactory.registerVariableElement("当前值",  "", "v", "当前值", classDictionary.lookup(Class.TYPE_NUMBER));
        varFactory.registerVariableElement("起始时间",  "", "startTime", "起始时间", classDictionary.lookup(Class.TYPE_STRING));
        varFactory.registerVariableElement("结束时间",  "", "endTime", "结束时间", classDictionary.lookup(Class.TYPE_STRING));
    }

    public static void main( String[] args )
    {
        test();
    }
}
