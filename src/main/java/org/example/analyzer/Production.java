package org.example.analyzer;

import javafx.util.Pair;
import org.example.analyzer.element.*;
import org.example.analyzer.element.Class;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class Production
{
    abstract static class Reducer
    {
        class Result
        {
            public Result(boolean isOk, String errString, Map<String, Object> attributes)
            {
                this.isOk = isOk;
                this.errString = errString;
                this.attributes = attributes;
            }

            boolean isOk;
            String errString;
            Map<String, Object> attributes;
        }

        abstract Result reduce(SimpleSyntaxAnalyzer analyzer, Production production);
    }

    private int id;
    private int nonterminal;
    private Reducer reducer;

    public Production(int id, int nonterminal, Reducer reducer)
    {
        this.id = id;
        this.nonterminal = nonterminal;
        this.reducer = reducer;
    }

    public int getId()
    {
        return id;
    }

    public int getNonterminal()
    {
        return nonterminal;
    }

    Reducer.Result reduce(SimpleSyntaxAnalyzer analyzer)
    {
        return this.reducer.reduce(analyzer, this);
    }

    static class UnaryOperationReducer extends Reducer
    {
        String displayName;
        String name;
        String hint;

        public UnaryOperationReducer(String displayName, String name, String hint)
        {
            this.displayName = displayName;
            this.name = name;
            this.hint = hint;
        }

        @Override
        Result reduce(SimpleSyntaxAnalyzer analyzer, Production production)
        {
            final Map<String, Object> attributes = analyzer.getAttributeFromStack(-1);
            final Map<String, Object> attributes2 = analyzer.getAttributeFromStack(-2);
            analyzer.pop(2);

            IElement e = new UnaryOperationElement(displayName, name, hint);
            e.setParam(0, (IElement) attributes.get(analyzer.ATTRIBUTE_ELEMENT));
            e.setLine((int)attributes2.get(analyzer.ATTRIBUTE_POSITION_LINE));
            e.setColumn((int)attributes2.get(analyzer.ATTRIBUTE_POSITION_COLUMN));
            final Pair<Boolean, String> err = e.checkParams(analyzer.classDictionary);
            if (!err.getKey())
                return new Result(false, err.getValue(), null);

            Map<String, Object> newAttributes = new HashMap<>();
            newAttributes.putAll(attributes);
            newAttributes.put(analyzer.ATTRIBUTE_ELEMENT, e);
            return new Result(true, "", newAttributes);
        }
    }

    static class OneSymbolBinaryOperationReducer extends Reducer
    {
        String displayName;
        String name;
        String hint;

        public OneSymbolBinaryOperationReducer(String displayName, String name, String hint)
        {
            this.displayName = displayName;
            this.name = name;
            this.hint = hint;
        }

        @Override
        Result reduce(SimpleSyntaxAnalyzer analyzer, Production production)
        {
            final Map<String, Object> attributes1 = analyzer.getAttributeFromStack(-3);
            final Map<String, Object> attributes2 = analyzer.getAttributeFromStack(-1);
            final Map<String, Object> attributes3 = analyzer.getAttributeFromStack(-2);
            analyzer.pop(3);

            IElement p1 = (IElement) attributes1.get(analyzer.ATTRIBUTE_ELEMENT);
            IElement p2 = (IElement) attributes2.get(analyzer.ATTRIBUTE_ELEMENT);
            IElement e = new BinaryOperationElement(displayName, name, hint);
            e.setParam(0, p1);
            e.setParam(1, p2);
            e.setLine((int)attributes3.get(analyzer.ATTRIBUTE_POSITION_LINE));
            e.setColumn((int)attributes3.get(analyzer.ATTRIBUTE_POSITION_COLUMN));
            final Pair<Boolean, String> err = e.checkParams(analyzer.classDictionary);
            if (!err.getKey())
                return new Result(false, err.getValue(), null);

            Map<String, Object> newAttributes = new HashMap<>();
            newAttributes.put(analyzer.ATTRIBUTE_ELEMENT, e);
            return new Result(true, "", newAttributes);
        }
    }

    static class TwoSymbolBinaryOperationReducer extends Reducer
    {
        String displayName;
        String name;
        String hint;

        public TwoSymbolBinaryOperationReducer(String displayName, String name, String hint)
        {
            this.displayName = displayName;
            this.name = name;
            this.hint = hint;
        }

        @Override
        Result reduce(SimpleSyntaxAnalyzer analyzer, Production production)
        {
            final Map<String, Object> attributes1 = analyzer.getAttributeFromStack(-4);
            final Map<String, Object> attributes2 = analyzer.getAttributeFromStack(-1);
            final Map<String, Object> attributes3 = analyzer.getAttributeFromStack(-3);
            analyzer.pop(4);

            IElement e = new BinaryOperationElement(displayName, name, hint);
            IElement p1 = (IElement) attributes1.get(analyzer.ATTRIBUTE_ELEMENT);
            IElement p2 = (IElement) attributes2.get(analyzer.ATTRIBUTE_ELEMENT);
            e.setParam(0, p1);
            e.setParam(1, p2);
            e.setLine((int)attributes3.get(analyzer.ATTRIBUTE_POSITION_LINE));
            e.setColumn((int)attributes3.get(analyzer.ATTRIBUTE_POSITION_COLUMN));
            final Pair<Boolean, String> err = e.checkParams(analyzer.classDictionary);
            if (!err.getKey())
                return new Result(false, err.getValue(), null);

            Map<String, Object> newAttributes = new HashMap<>();
            newAttributes.put(analyzer.ATTRIBUTE_ELEMENT, e);
            return new Result(true, "", newAttributes);
        }
    }

    static class RedirectOperationReducer extends Reducer
    {
        public RedirectOperationReducer()
        {
        }

        @Override
        Result reduce(SimpleSyntaxAnalyzer analyzer, Production production)
        {
            final Map<String, Object> attributes = analyzer.getAttributeFromStack(-1);
            analyzer.pop(1);
            return new Result(true, "", attributes);
        }
    }

    static class LiteralConstantReducer extends Reducer
    {
        LexicalTokenType tokenType;

        public LiteralConstantReducer(LexicalTokenType tokenType)
        {
            this.tokenType = tokenType;
        }

        @Override
        Result reduce(SimpleSyntaxAnalyzer analyzer, Production production)
        {
            final Map<String, Object> attributes = analyzer.getAttributeFromStack(-1);
            analyzer.pop(1);

            assert (tokenType == attributes.get(analyzer.ATTRIBUTE_TERMINAL_TYPE));
            assert (tokenType != null);
            String value = (String) attributes.get(analyzer.ATTRIBUTE_TERMINAL_VALUE);
            assert (value != null);
            int line = (int)attributes.get(analyzer.ATTRIBUTE_POSITION_LINE);
            int column = (int)attributes.get(analyzer.ATTRIBUTE_POSITION_COLUMN);

            IElement e = null;
            Class aClass = null;
            Map<String, Object> newAttributes = new HashMap<>();
            switch (tokenType)
            {
                case ID:    //此处id一定是一个全局变量或类名
                    e = analyzer.varFactory.create("", value);    //全局变量
                    aClass = analyzer.classDictionary.lookup(value);   //类名
                    if (e != null && aClass != null)   //类名和变量名冲突
                    {
                        return new Result(false, "[" + line + ":" + column + "]: '" + value + "' 已被申明为一个类", null);
                    }

                    if (aClass != null)
                    {
                        //如果是类名，带回原始值与终结符类型
                        newAttributes.put(analyzer.ATTRIBUTE_TERMINAL_VALUE, value);
                        newAttributes.put(analyzer.ATTRIBUTE_TERMINAL_TYPE, LexicalTokenType.ID);
                        newAttributes.put(analyzer.ATTRIBUTE_POSITION_LINE, line);
                        newAttributes.put(analyzer.ATTRIBUTE_POSITION_COLUMN, column);
                        return new Result(true, "", newAttributes);
                    }
                    if (e == null)
                    {
                        return new Result(false, "[" + line + ":" + column + "]: 未知变量 '" + value + "'", null);
                    }
                    break;
                case NUMBER:
                    aClass = analyzer.classDictionary.lookup(Class.TYPE_NUMBER);
                    e = new LiteralConstantElement(value, "数字 " + value, aClass);
                    break;
                case STRING:
                    aClass = analyzer.classDictionary.lookup(Class.TYPE_STRING);
                    e = new LiteralConstantElement(value,"字符串 " + value, aClass);
                    break;
            }

            assert (e != null);
            e.setLine(line);
            e.setColumn(column);
            newAttributes.put(analyzer.ATTRIBUTE_ELEMENT, e);
            return new Result(true, "", newAttributes);
        }
    }

    static class MemberReducer extends Reducer
    {
        static public final int TYPE_MEMBER_FUNCTION = 1;
        static public final int TYPE_MEMBER_VARIABLE = 2;

        int memberType;

        public MemberReducer(int memberType)
        {
            this.memberType = memberType;
        }

        @Override
        Result reduce(SimpleSyntaxAnalyzer analyzer, Production production)
        {
            final Map<String, Object> attributes1 = analyzer.getAttributeFromStack(-3); //e
            final Map<String, Object> attributes2 = analyzer.getAttributeFromStack(-1); //func/id
            final Map<String, Object> attributes3 = analyzer.getAttributeFromStack(-2);
            analyzer.pop(3);

            int line = (int)attributes3.get(analyzer.ATTRIBUTE_POSITION_LINE);
            int column = (int)attributes3.get(analyzer.ATTRIBUTE_POSITION_COLUMN);

            String typeName = null;
            // 成员名称可能时一个函数, 由 FUNC->FUNC_NAME()、FUNC_NAME->id产生, 也可能是一个变量, 由id 产生
            String memberName = null;
            switch (memberType)
            {
                case TYPE_MEMBER_VARIABLE:
                    memberName = (String) attributes2.get(analyzer.ATTRIBUTE_TERMINAL_VALUE);
                    break;
                case TYPE_MEMBER_FUNCTION:
                    memberName = (String) attributes2.get(analyzer.ATTRIBUTE_MEMBER_NAME);
                    break;
            }
            assert(memberName != null);

            FunctionElement.Leading lf = null;
            VariableElement.Leading lv = null;

            /* e 可能是个类名, 也可能是个表达式 */
            // e 是类名
            LexicalTokenType tokenType1 = (LexicalTokenType)attributes1.get(analyzer.ATTRIBUTE_TERMINAL_TYPE);
            if (tokenType1 != null)
            {
                assert (tokenType1 == LexicalTokenType.ID);
                typeName = (String)attributes1.get(analyzer.ATTRIBUTE_TERMINAL_VALUE);
                lf = new FunctionElement.StringLeading(typeName);
                lv = new VariableElement.StringLeading(typeName);
            }
            //e 是表达式
            else
            {
                IElement e = (IElement)attributes1.get(analyzer.ATTRIBUTE_ELEMENT);
                typeName = e.getResultType().getName();
                lf = new FunctionElement.ExpressionLeading(e);
                lv = new VariableElement.ExpressionLeading(e);
            }
            assert (!typeName.isEmpty());

            IElement e = null;
            switch (memberType)
            {
                case TYPE_MEMBER_VARIABLE:
                    e = analyzer.varFactory.create(typeName, memberName);
                    if (e != null)
                        ((VariableElement)e).setLeading(lv);
                    break;
                case TYPE_MEMBER_FUNCTION:
                    e = analyzer.funcFactory.create(typeName, memberName);
                    if (e != null)
                        ((FunctionElement)e).setLeading(lf);
                    ArrayList<IElement> args = (ArrayList<IElement> )attributes2.get(analyzer.ATTRIBUTE_ARGS);
                    if (e != null)
                    {
                        if (e.getParams().size() != args.size())
                        {
                            return new Result(false, "[" + line + ":" + column + "]: 函数 '" + e.getName() + "' 不能接受 " + args.size() + " 个参数", null);
                        }

                        for (int i = 0; i < args.size(); i++)
                        {
                            e.setParam(i, args.get(i));
                        }

                        Pair<Boolean, String> err = e.checkParams(analyzer.classDictionary);
                        if (!err.getKey())
                            return new Result(false, err.getValue(), null);
                    }
                    break;
                default:
                    assert (false);
            }

            if (e == null)
            {
                return new Result(false, "[" + line + ":" + column + "]: 类型 '" + typeName + "' 不存在成员 '" + memberName + "'", null);
            }

            e.setLine(line);
            e.setColumn(column);

            Map<String, Object> newAttributes = new HashMap<>();
            newAttributes.put(analyzer.ATTRIBUTE_ELEMENT, e);
            return new Result(true, "", newAttributes);
        }
    }

}
