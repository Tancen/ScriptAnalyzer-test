package org.example.analyzer;

import javafx.util.Pair;
import org.example.analyzer.element.*;

import java.util.*;

/*
语法解析器采用 LALR(2) 文法
S->E
E->(E)
E->!E
E->~E
E->E*E
E->E/E
E->E%E
E->E+E
E->E-E
E->E>>E
E->E<<E
E->E>E
E->E>=E
E->E<E
E->E<=E
E->E!=E
E->E==E
E->E&E
E->E^E
E->E|E
E->E&&E
E->E||E
E->OBJ
OBJ->id
OBJ->number
OBJ->string
E->FUNC
E->E.id
E->E.FUNC
FUNC->FUNC_NAME()
FUNC->FUNC_NAME(ARGS_BODY)
FUNC_NAME->id
ARGS_BODY ->E
ARGS_BODY ->ARGS_BODY,E
E->-E

*/
public class SimpleSyntaxAnalyzer
{
    static class State
    {
        public static class Error
        {
            boolean isOk;
            boolean shifted;
            String errString;
        }

        private static class Key
        {
            public Key(LexicalTokenType l1, LexicalTokenType l2)
            {
                this.l1 = l1;
                this.l2 = l2;
            }

            @Override
            public boolean equals(Object o)
            {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Key key = (Key) o;
                return l1 == key.l1 && l2 == key.l2;
            }

            @Override
            public int hashCode()
            {
                return Objects.hash(l1, l2);
            }

            LexicalTokenType l1;
            LexicalTokenType l2;
        }

        private static class Action
        {
            public enum Type { SHIFT /* 移入 */ , REDUCE /* 规约 */};

            public Action(Type type, LexicalTokenType acceptFollowing, Set<LexicalTokenType> excludeFollowing, int v)
            {
                this.acceptFollowing = acceptFollowing;
                this.excludeFollowing = excludeFollowing;
                this.type = type;
                this.v = v;
            }

            /*
            * 对第二个词素类型限定
            * acceptFollowing 与 excludeFollowing 不能相同
            * */
            LexicalTokenType acceptFollowing;     //接收第二个
            Set<LexicalTokenType> excludeFollowing;     //排除第二个

            Type type;
            int v;      //type 为移入时, v 表示状态id, type 为规约时 v 表示产生式id
        }

        final private int id;
        Map<Key, Action> mappingAction = new HashMap<>();
        Map<Integer, Integer> mappingGoto = new HashMap<>();    //key 为非终结符id, value 为状态

        State(int id)
        {
            this.id = id;
        }

        //添加移入映射, 参数 state 为状态的id
        void addShiftMapping(LexicalTokenType acceptLeading, LexicalTokenType acceptFollowing,
            Set<LexicalTokenType> excludeFollowing, int state)
        {
            assert(!excludeFollowing.contains(acceptFollowing));
            assert(acceptLeading != null);

            mappingAction.put(new Key(acceptLeading, acceptFollowing), new Action(Action.Type.SHIFT, acceptFollowing, excludeFollowing, state));
        }

        //添加GOTO映射, 参数 nonterminal 为非终结符id, state 为状态的id
        void addGotoMapping(int nonterminal, int state)
        {
            mappingGoto.put(nonterminal, state);
        }

        //添加规约映射, 参数 production 为产生式的id
        void addReduceMapping(LexicalTokenType acceptLeading, LexicalTokenType acceptFollowing,
                Set<LexicalTokenType> excludeFollowing, int production)
        {
            assert(!excludeFollowing.contains(acceptFollowing));
            assert(acceptLeading != null);

            mappingAction.put(new Key(acceptLeading, acceptFollowing), new Action(Action.Type.REDUCE, acceptFollowing, excludeFollowing, production));
        }

        /*  向状态机输入词素
            返回值的key为是否有错误, value 为是否接收当前词素, 未接收的词素需要下次继续向状态机输入
         */
        Error write(SimpleSyntaxAnalyzer analyzer, LexicalToken t1, LexicalToken t2)
        {
            Error ret = new Error();
            ret.isOk = false;
            ret.shifted = false;

            assert (t1 != null);
            do
            {
                //优先考虑组合key, 组合key找不到action时，再通过单独key找
                Action action = null;
                if (t2 != null)
                    action= mappingAction.get(new Key(t1.type, t2.type));

                if (action == null)
                {
                    action = mappingAction.get(new Key(t1.type, null));
                    if (action == null)
                    {
                        ret.errString = "[" +  t1.line + ":" + t1.position + "]: 异常符号 " + t1.value;
                        break;
                    }
                }

                //如果有依赖 follow, 检查follow是否匹配
                if (action.acceptFollowing != null && (t2 == null || t2.type != action.acceptFollowing))
                {
                    ret.errString = "[" +  t2.line + ":" + t2.position + "]: 异常符号 " + t2.value;
                    break;
                }

                //检查 follow 是否合格
                if (t2 != null && action.excludeFollowing.contains(t2.type))
                {
                    ret.errString = "[" +  t2.line + ":" + t2.position + "]: 异常符号 " + t2.value;
                    break;
                }

                //移入
                if (action.type == Action.Type.SHIFT)
                {
                    analyzer.shiftTerminal(t1, action.v);
                    ret.shifted = true;
                }
                //规约
                else if (action.type == Action.Type.REDUCE)
                {
                    Pair<Boolean, String> e = analyzer.reduce(action.v);
                    if(!e.getKey())
                    {
                        ret.errString = e.getValue();
                        break;
                    }
                }
                else
                {
                    assert (false);
                }

                ret.isOk = true;
            } while (false);

            return ret;
        }

        //向状态机输入非终结符
        Pair<Boolean, String> write(SimpleSyntaxAnalyzer analyzer, int nonterminal, Map<String, Object> attributes)
        {
            Integer state = mappingGoto.get(nonterminal);
            if (state == null)
            {
                return new Pair<>(false, "语法错误");
            }

            analyzer.shiftNonterminal(nonterminal, state, attributes);
            return new Pair<>(true, "");
        }
    }

    static class Symbol
    {
        public static enum Type { TERMINAL, NONTERMINAL };
        Type type;
        int id;

        public Symbol(Type type, int id)
        {
            this.type = type;
            this.id = id;
        }
    }

    final int STATE_ACC = -1;
    final int NOTERMINAL_S = 0;
    final int NOTERMINAL_E = 1;
    final int NOTERMINAL_OBJ = 2;
    final int NOTERMINAL_FUNC = 3;
    final int NOTERMINAL_FUNC_NAME = 4;
    final int NOTERMINAL_ARGS = 5;
    boolean finished = false;

    final String ATTRIBUTE_ELEMENT = "Element";
    final String ATTRIBUTE_TERMINAL_VALUE = "TerminalValue";
    final String ATTRIBUTE_TERMINAL_TYPE = "TerminalType";
    final String ATTRIBUTE_MEMBER_NAME = "MemberName";
    final String ATTRIBUTE_ARGS = "Args";
    final String ATTRIBUTE_POSITION_LINE = "PositionLine";
    final String ATTRIBUTE_POSITION_COLUMN = "PositionColumn";

    Map<Integer, State> mappingState = new HashMap<>();     //状态映射表, key 为 value 的id
    Map<Integer, Production> mappingProduction = new HashMap<>();  //产生式状态映射表, key 为 value 的id

    LinkedList<State>  stackState = new LinkedList<>();    //状态栈
    LinkedList<Symbol>  stackSymbol = new LinkedList<>();     //符号栈
    LinkedList<Map<String, Object>>  stackAttribute = new LinkedList<>();     //属性栈
    FunctionElementFactory funcFactory = new FunctionElementFactory();
    VariableElementFactory varFactory = new VariableElementFactory();
    ClassDictionary classDictionary = new ClassDictionary();

    public SimpleSyntaxAnalyzer()
    {
        initStateTable();
        initProductionTable();
    }

    private void initProductionTable()
    {
        //S->E
        mappingProduction.put(1, new Production(1, NOTERMINAL_S, new Production.RedirectOperationReducer()));

        //E->(E)
        mappingProduction.put(2, new Production(2, NOTERMINAL_E, new Production.Reducer()
        {
            @Override
            Result reduce(SimpleSyntaxAnalyzer analyzer, Production production)
            {
                final Map<String, Object> attributes1 = analyzer.getAttributeFromStack(-2);
                final Map<String, Object> attributes2 = analyzer.getAttributeFromStack(-3);
                analyzer.pop(3);

                Map<String, Object> newAttributes = new HashMap<>();
                newAttributes.putAll(attributes1);
                newAttributes.put(ATTRIBUTE_POSITION_LINE, attributes2.get(ATTRIBUTE_POSITION_LINE));
                newAttributes.put(ATTRIBUTE_POSITION_COLUMN, attributes2.get(ATTRIBUTE_POSITION_COLUMN));
                return new Result(true, "", newAttributes);
            }
        }));

        //E->!E
        mappingProduction.put(3, new Production(3, NOTERMINAL_E,
                new Production.UnaryOperationReducer("逻辑非", "!", "逻辑非")));

        //E->~E
        mappingProduction.put(4, new Production(4, NOTERMINAL_E,
                new Production.UnaryOperationReducer("一元非运算", "~", "一元非运算")));

        //E->E*E
        mappingProduction.put(5, new Production(5, NOTERMINAL_E,
                new Production.OneSymbolBinaryOperationReducer("乘", "*", "乘法")));

        //E->E/E
        mappingProduction.put(6, new Production(6, NOTERMINAL_E,
                new Production.OneSymbolBinaryOperationReducer("除", "/", "除法")));

        //E->E%E
        mappingProduction.put(7, new Production(7, NOTERMINAL_E,
                new Production.OneSymbolBinaryOperationReducer("模", "%", "求余")));

        //E->E+E
        mappingProduction.put(8, new Production(8, NOTERMINAL_E,
                new Production.OneSymbolBinaryOperationReducer("加", "+", "加法")));

        //E->E-E
        mappingProduction.put(9, new Production(9, NOTERMINAL_E,
                new Production.OneSymbolBinaryOperationReducer("减", "-", "减法")));

        //E->E>>E
        mappingProduction.put(10, new Production(10, NOTERMINAL_E,
                new Production.TwoSymbolBinaryOperationReducer("右移", ">>", "位运算右移")));

        //E->E<<E
        mappingProduction.put(11, new Production(11, NOTERMINAL_E,
                new Production.TwoSymbolBinaryOperationReducer("左移", "<<", "位运算左移")));
        //E->E>E
        mappingProduction.put(12, new Production(12, NOTERMINAL_E,
                new Production.OneSymbolBinaryOperationReducer("大于", ">", "逻辑运算符大于")));

        //E->E>=E
        mappingProduction.put(13, new Production(13, NOTERMINAL_E,
                new Production.TwoSymbolBinaryOperationReducer("大于等于", ">=", "逻辑运算符大于等于")));

        //E->E<E
        mappingProduction.put(14, new Production(14, NOTERMINAL_E,
                new Production.OneSymbolBinaryOperationReducer("小于", "<", "逻辑运算符小于")));

        //E->E<=E
        mappingProduction.put(15, new Production(15, NOTERMINAL_E,
                new Production.TwoSymbolBinaryOperationReducer("小于等于", "<=", "逻辑运算符小于等于")));

        //E->E!=E
        mappingProduction.put(16, new Production(16, NOTERMINAL_E,
                new Production.TwoSymbolBinaryOperationReducer("不等于", "!=", "逻辑运算符不等于")));

        //E->E==E
        mappingProduction.put(17, new Production(17, NOTERMINAL_E,
                new Production.TwoSymbolBinaryOperationReducer("等于", "==", "逻辑运算符等于")));

        //E->E&E
        mappingProduction.put(18, new Production(18, NOTERMINAL_E,
                new Production.OneSymbolBinaryOperationReducer("位与运算", "&", "位与运算")));

        //E->E^E
        mappingProduction.put(19, new Production(19, NOTERMINAL_E,
                new Production.OneSymbolBinaryOperationReducer("位异或运算", "^", "位异或运算")));

        //E->E|E
        mappingProduction.put(20, new Production(20, NOTERMINAL_E,
                new Production.OneSymbolBinaryOperationReducer("位或运算", "|", "位或运算")));

        //E->E&&E
        mappingProduction.put(21, new Production(21, NOTERMINAL_E,
                new Production.TwoSymbolBinaryOperationReducer("且", "&&", "逻辑且运算")));

        //E->E||E
        mappingProduction.put(22, new Production(22, NOTERMINAL_E,
                new Production.TwoSymbolBinaryOperationReducer("或", "||", "逻辑或运算")));

        //E->OBJ
        mappingProduction.put(23, new Production(23, NOTERMINAL_E, new Production.RedirectOperationReducer()));

        //OBJ->id
        mappingProduction.put(24, new Production(24, NOTERMINAL_OBJ, new Production.LiteralConstantReducer(LexicalTokenType.ID)));

        //OBJ->number
        mappingProduction.put(25, new Production(25, NOTERMINAL_OBJ, new Production.LiteralConstantReducer(LexicalTokenType.NUMBER)));

        //OBJ->string
        mappingProduction.put(26, new Production(26, NOTERMINAL_OBJ, new Production.LiteralConstantReducer(LexicalTokenType.STRING)));

        //E->FUNC
        mappingProduction.put(27, new Production(27, NOTERMINAL_E, new Production.RedirectOperationReducer()));

        //E->E.id
        mappingProduction.put(28, new Production(28, NOTERMINAL_E, new Production.MemberReducer(Production.MemberReducer.TYPE_MEMBER_VARIABLE)));

        //E->E.FUNC
        mappingProduction.put(29, new Production(29, NOTERMINAL_E, new Production.MemberReducer(Production.MemberReducer.TYPE_MEMBER_FUNCTION)));

        //FUNC->FUNC_NAME()
        mappingProduction.put(30, new Production(30, NOTERMINAL_FUNC, new Production.Reducer()
        {
            @Override
            Result reduce(SimpleSyntaxAnalyzer analyzer, Production production)
            {
                final Map<String, Object> attributes1 = analyzer.getAttributeFromStack(-3); //func_name
                analyzer.pop(3);

                int line = (int)attributes1.get(ATTRIBUTE_POSITION_LINE);
                int column = (int)attributes1.get(ATTRIBUTE_POSITION_COLUMN);
                /*
                * FUNC在规约时有2种情况
                * 1、为类的成员函数, 如 E.b() //t.b()
                *                         ^
                * 2、为全局函数, 如 createA()
                *                         ^
                * 针对情况1, E 会早于 b() 规约, 这样并不会影响 E->E.FUNC 的正确性, 此时需要
                * 将 ATTRIBUTE_MEMBER_NAME, ATTRIBUTE_ARGS 记录到属性中, 后续由 E->E.FUNC 去处理
                * 针对情况2, 由于 createA 会在全局记录, 此时需要将 ATTRIBUTE_ELEMENT 记录到属性中
                * 由于此时并不知道整体是情况1还是情况2，因此需要将 ATTRIBUTE_MEMBER_NAME,ATTRIBUTE_ARGS 和 ATTRIBUTE_ELEMENT 都记录到属性中
                * */
                Map<String, Object> newAttributes = new HashMap<>();

                //情况1
                String funcName = (String)attributes1.get(ATTRIBUTE_MEMBER_NAME);
                assert(funcName != null);
                newAttributes.put(ATTRIBUTE_MEMBER_NAME, funcName);
                newAttributes.put(ATTRIBUTE_ARGS, new ArrayList<IElement>());
                newAttributes.put(ATTRIBUTE_POSITION_LINE, line);
                newAttributes.put(ATTRIBUTE_POSITION_COLUMN, column);

                //情况2
                IElement e = analyzer.funcFactory.create("", funcName);
                if (e != null)
                {
                    e.setLine(line);
                    e.setColumn(column);
                    newAttributes.put(ATTRIBUTE_ELEMENT, e);
                }
                return new Result(true, "", newAttributes);
            }
        }));

        //FUNC->FUNC_NAME(ARGS)
        mappingProduction.put(31, new Production(31, NOTERMINAL_FUNC, new Production.Reducer()
        {
            @Override
            Result reduce(SimpleSyntaxAnalyzer analyzer, Production production)
            {
                final Map<String, Object> attributes1 = analyzer.getAttributeFromStack(-4); //func_name
                final Map<String, Object> attributes2 = analyzer.getAttributeFromStack(-2); //args
                analyzer.pop(4);

                int line = (int)attributes1.get(ATTRIBUTE_POSITION_LINE);
                int column = (int)attributes1.get(ATTRIBUTE_POSITION_COLUMN);

                /*
                 * 同 FUNC->FUNC_NAME(), 但多了参数, 因此需要在此处进行参数填充及校验
                 * */
                Map<String, Object> newAttributes = new HashMap<>();

                //情况1
                String funcName = (String)attributes1.get(analyzer.ATTRIBUTE_MEMBER_NAME);
                assert(funcName != null);
                newAttributes.put(ATTRIBUTE_POSITION_LINE, line);
                newAttributes.put(ATTRIBUTE_POSITION_COLUMN, column);
                newAttributes.put(analyzer.ATTRIBUTE_MEMBER_NAME, funcName);

                //args
                ArrayList<IElement> args = (ArrayList)attributes2.get(ATTRIBUTE_ARGS);
                assert(args != null);

                //情况2
                FunctionElement e = analyzer.funcFactory.create("", funcName);
                if (e != null)
                {
                    e.setLine(line);
                    e.setColumn(column);

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

                    newAttributes.put(analyzer.ATTRIBUTE_ELEMENT, e);
                }
                else
                {
                    newAttributes.put(ATTRIBUTE_ARGS, args);    //写回参数列表
                }

                return new Result(true, "", newAttributes);
            }
        }));

        //FUNC_NAME->id
        mappingProduction.put(32, new Production(32, NOTERMINAL_FUNC_NAME, new Production.Reducer()
        {
            @Override
            Result reduce(SimpleSyntaxAnalyzer analyzer, Production production)
            {
                final Map<String, Object> attributes = analyzer.getAttributeFromStack(-1);
                analyzer.pop(1);

                Map<String, Object> newAttributes = new HashMap<>();

                String funcName = (String)attributes.get(ATTRIBUTE_TERMINAL_VALUE);

                newAttributes.put(ATTRIBUTE_MEMBER_NAME, funcName);
                newAttributes.put(ATTRIBUTE_POSITION_LINE, attributes.get(ATTRIBUTE_POSITION_LINE));
                newAttributes.put(ATTRIBUTE_POSITION_COLUMN, attributes.get(ATTRIBUTE_POSITION_COLUMN));

                return new Result(true, "", newAttributes);
            }
        }));

        //ARGS ->E
        mappingProduction.put(33, new Production(33, NOTERMINAL_ARGS, new Production.Reducer()
        {
            @Override
            Result reduce(SimpleSyntaxAnalyzer analyzer, Production production)
            {
                final Map<String, Object> attributes = analyzer.getAttributeFromStack(-1);
                analyzer.pop(1);

                Map<String, Object> newAttributes = new HashMap<>();

                //args
                ArrayList<IElement> args = new ArrayList<>();
                args.add((IElement)attributes.get(ATTRIBUTE_ELEMENT));

                newAttributes.put(ATTRIBUTE_ARGS, args);
                newAttributes.put(ATTRIBUTE_POSITION_LINE, attributes.get(ATTRIBUTE_POSITION_LINE));
                newAttributes.put(ATTRIBUTE_POSITION_COLUMN, attributes.get(ATTRIBUTE_POSITION_COLUMN));

                return new Result(true, "", newAttributes);
            }
        }));

        //ARGS ->ARGS,E
        mappingProduction.put(34, new Production(34, NOTERMINAL_ARGS, new Production.Reducer()
        {
            @Override
            Result reduce(SimpleSyntaxAnalyzer analyzer, Production production)
            {
                Map<String, Object> attributes1 = analyzer.getAttributeFromStack(-3);  // args
                Map<String, Object> attributes2 = analyzer.getAttributeFromStack(-1);  // e
                analyzer.pop(3);

                ArrayList<IElement> args = (ArrayList<IElement>) attributes1.get(ATTRIBUTE_ARGS);
                IElement e = (IElement)attributes2.get(ATTRIBUTE_ELEMENT);
                args.add(e);

                return new Result(true, "", attributes1);
            }
        }));

        //E->-E
        mappingProduction.put(35, new Production(35, NOTERMINAL_E,
                new Production.UnaryOperationReducer("负", "-", "负")));

    }

    private void initStateTable()
    {
        /* 初始化状态表 */

        final int STATE_SIZE = 65;
        Object[] argsShiftMapping = new Object[STATE_SIZE];

        //移入动作
        argsShiftMapping[0] = new Object[][]
        {
            {  LexicalTokenType.NUMBER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 52 /*state*/},
            {  LexicalTokenType.ID /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 51 /*state*/},
            {  LexicalTokenType.STRING /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 53 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 63 /*state*/},
            {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 46 /*state*/},
            {  LexicalTokenType.SIGN_TILDE /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 48 /*state*/},
            {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
            {  LexicalTokenType.SIGN_PAREN_LEFT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 43 /*state*/},
        };

        argsShiftMapping[1] = new Object[][]
        {
            {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*state*/},
            {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*state*/},
            {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*state*/},
            {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*state*/},
            {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*state*/},
            {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*state*/},
            {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 34 /*state*/},
            {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 36 /*state*/},
            {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 18 /*state*/},
            {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*state*/},
            {  LexicalTokenType.SIGN_EQUALS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*state*/},
            {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
            {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, STATE_ACC /*state*/},
        };
        argsShiftMapping[2] = argsShiftMapping[0];
        argsShiftMapping[3] = new Object[][]
        {
            {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
        };
        argsShiftMapping[4] = new Object[][]
        {
            {  LexicalTokenType.NUMBER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 52 /*state*/},
            {  LexicalTokenType.ID /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 51 /*state*/},
            {  LexicalTokenType.STRING /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 53 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 63 /*state*/},
            {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 46 /*state*/},
            {  LexicalTokenType.SIGN_TILDE /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 48 /*state*/},
            {  LexicalTokenType.SIGN_PAREN_LEFT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 43 /*state*/},
        };
        argsShiftMapping[5] = argsShiftMapping[3];
        argsShiftMapping[6] = argsShiftMapping[4];
        argsShiftMapping[7] = argsShiftMapping[3];
        argsShiftMapping[8] = argsShiftMapping[2];
        argsShiftMapping[9] = new Object[][]
        {
            {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*state*/},
            {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*state*/},
            {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*state*/},
            {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
        };
        argsShiftMapping[10] = argsShiftMapping[2];
        argsShiftMapping[11] = argsShiftMapping[9];
        argsShiftMapping[12] = new Object[][]
        {
            {  LexicalTokenType.NUMBER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 52 /*state*/},
            {  LexicalTokenType.ID /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 51 /*state*/},
            {  LexicalTokenType.STRING /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 53 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 63 /*state*/},
            {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 46 /*state*/},
            {  LexicalTokenType.SIGN_TILDE /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 48 /*state*/},
            {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 13 /*state*/},
            {  LexicalTokenType.SIGN_EQUALS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 15 /*state*/},
            {  LexicalTokenType.SIGN_PAREN_LEFT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 43 /*state*/},
        };
        argsShiftMapping[13] = argsShiftMapping[4];
        argsShiftMapping[14] = new Object[][]
        {
            {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*state*/},
            {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*state*/},
            {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*state*/},
            {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*state*/},
            {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
        };
        argsShiftMapping[15] = argsShiftMapping[4];
        argsShiftMapping[16] = new Object[][]
        {
            {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*state*/},
            {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*state*/},
            {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*state*/},
            {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*state*/},
            {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  LexicalTokenType.SIGN_LESS/*acceptFollowing*/, null/*excludeFollowing*/, 18 /*state*/},
            {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  LexicalTokenType.SIGN_GREATER/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*state*/},
            {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
        };
        argsShiftMapping[17] = argsShiftMapping[16];
        argsShiftMapping[18] = new Object[][]
        {
            {  LexicalTokenType.NUMBER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 52 /*state*/},
            {  LexicalTokenType.ID /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 51 /*state*/},
            {  LexicalTokenType.STRING /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 53 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 63 /*state*/},
            {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 46 /*state*/},
            {  LexicalTokenType.SIGN_TILDE /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 48 /*state*/},
            {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 19 /*state*/},
            {  LexicalTokenType.SIGN_EQUALS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 21 /*state*/},
            {  LexicalTokenType.SIGN_PAREN_LEFT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 43 /*state*/},
        };
        argsShiftMapping[19] = argsShiftMapping[4];
        argsShiftMapping[20] = new Object[][]
        {
            {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*state*/},
            {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*state*/},
            {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*state*/},
            {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*state*/},
            {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
        };
        argsShiftMapping[21] = argsShiftMapping[4];
        argsShiftMapping[22] = argsShiftMapping[16];
        argsShiftMapping[23] = argsShiftMapping[16];
        argsShiftMapping[24] = new Object[][]
        {
            {  LexicalTokenType.SIGN_EQUALS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*state*/},
        };
        argsShiftMapping[25] = argsShiftMapping[4];
        argsShiftMapping[26] = new Object[][]
        {
            {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*state*/},
            {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*state*/},
            {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*state*/},
            {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*state*/},
            {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 18 /*state*/},
            {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*state*/},
            {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
        };
        argsShiftMapping[27] = new Object[][]
        {
            {  LexicalTokenType.SIGN_EQUALS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*state*/},
        };
        argsShiftMapping[28] = argsShiftMapping[4];
        argsShiftMapping[29] = argsShiftMapping[26];
        argsShiftMapping[30] = new Object[][]
        {
            {  LexicalTokenType.NUMBER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 52 /*state*/},
            {  LexicalTokenType.ID /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 51 /*state*/},
            {  LexicalTokenType.STRING /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 53 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 63 /*state*/},
            {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 46 /*state*/},
            {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 32 /*state*/},
            {  LexicalTokenType.SIGN_TILDE /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 48 /*state*/},
            {  LexicalTokenType.SIGN_PAREN_LEFT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 43 /*state*/},
        };
        argsShiftMapping[31] = new Object[][]
        {
            {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*state*/},
            {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*state*/},
            {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*state*/},
            {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*state*/},
            {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*state*/},
            {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 18 /*state*/},
            {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*state*/},
            {  LexicalTokenType.SIGN_EQUALS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*state*/},
            {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
        };
        argsShiftMapping[32] = argsShiftMapping[4];
        argsShiftMapping[33] = new Object[][]
        {
            {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*state*/},
            {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*state*/},
            {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*state*/},
            {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*state*/},
            {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*state*/},
            {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 34 /*state*/},
            {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 36 /*state*/},
            {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 18 /*state*/},
            {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*state*/},
            {  LexicalTokenType.SIGN_EQUALS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*state*/},
            {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
        };
        argsShiftMapping[34] = argsShiftMapping[4];
        argsShiftMapping[35] = new Object[][]
        {
            {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*state*/},
            {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*state*/},
            {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*state*/},
            {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*state*/},
            {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*state*/},
            {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*state*/},
            {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 18 /*state*/},
            {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*state*/},
            {  LexicalTokenType.SIGN_EQUALS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*state*/},
            {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
        };
        argsShiftMapping[36] = new Object[][]
        {
            {  LexicalTokenType.NUMBER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 52 /*state*/},
            {  LexicalTokenType.ID /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 51 /*state*/},
            {  LexicalTokenType.STRING /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 53 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 63 /*state*/},
            {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 46 /*state*/},
            {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 38 /*state*/},
            {  LexicalTokenType.SIGN_TILDE /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 48 /*state*/},
            {  LexicalTokenType.SIGN_PAREN_LEFT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 43 /*state*/},
        };
        argsShiftMapping[37] = new Object[][]
        {
            {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*state*/},
            {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*state*/},
            {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*state*/},
            {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*state*/},
            {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*state*/},
            {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*state*/},
            {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 34 /*state*/},
            {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 18 /*state*/},
            {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*state*/},
            {  LexicalTokenType.SIGN_EQUALS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*state*/},
            {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
        };
        argsShiftMapping[38] = argsShiftMapping[4];
        argsShiftMapping[39] = new Object[][]
        {
            {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*state*/},
            {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*state*/},
            {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*state*/},
            {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*state*/},
            {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*state*/},
            {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*state*/},
            {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 34 /*state*/},
            {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 36 /*state*/},
            {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 18 /*state*/},
            {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*state*/},
            {  LexicalTokenType.SIGN_EQUALS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*state*/},
            {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
        };
        argsShiftMapping[40] = new Object[][]
        {
            {  LexicalTokenType.ID /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 41 /*state*/},
        };
        argsShiftMapping[41] = new Object[][]{};
        argsShiftMapping[42] = new Object[][]{};
        argsShiftMapping[43] = argsShiftMapping[4];
        argsShiftMapping[44] = new Object[][]
        {
            {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*state*/},
            {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*state*/},
            {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*state*/},
            {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*state*/},
            {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*state*/},
            {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*state*/},
            {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*state*/},
            {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 34 /*state*/},
            {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 36 /*state*/},
            {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 18 /*state*/},
            {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*state*/},
            {  LexicalTokenType.SIGN_EQUALS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*state*/},
            {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
            {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 45 /*state*/},
        };
        argsShiftMapping[45] = new Object[][]{};
        argsShiftMapping[46] = argsShiftMapping[4];
        argsShiftMapping[47] = new Object[][]
        {
            {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
        };
        argsShiftMapping[48] = argsShiftMapping[4];
        argsShiftMapping[49] = argsShiftMapping[47];
        argsShiftMapping[50] = new Object[][]{};
        argsShiftMapping[51] = new Object[][]{};
        argsShiftMapping[52] = new Object[][]{};
        argsShiftMapping[53] = new Object[][]{};
        argsShiftMapping[54] = new Object[][]{};
        argsShiftMapping[55] = new Object[][]
        {
            {  LexicalTokenType.SIGN_PAREN_LEFT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 56 /*state*/},
        };
        argsShiftMapping[56] = argsShiftMapping[36];
        argsShiftMapping[57] = new Object[][]{};
        argsShiftMapping[58] = new Object[][]
        {
            {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 60 /*state*/},
            {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 59 /*state*/},
        };
        argsShiftMapping[59] = new Object[][]{};
        argsShiftMapping[60] = argsShiftMapping[4];
        argsShiftMapping[61] = argsShiftMapping[39];
        argsShiftMapping[62] = argsShiftMapping[39];
        argsShiftMapping[63] = new Object[][]
        {
            {  LexicalTokenType.NUMBER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 52 /*state*/},
            {  LexicalTokenType.ID /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 51 /*state*/},
            {  LexicalTokenType.STRING /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 53 /*state*/},
            {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 46 /*state*/},
            {  LexicalTokenType.SIGN_TILDE /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 48 /*state*/},
            {  LexicalTokenType.SIGN_PAREN_LEFT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 43 /*state*/},
        };
        argsShiftMapping[64] = new Object[][]{
                {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 40 /*state*/},
        };

        //规约动作
        Object[] argsReduceMapping = new Object[STATE_SIZE];
        argsReduceMapping[0] = new Object[][]{};
        argsReduceMapping[1] = new Object[][]{};
        argsReduceMapping[2] = new Object[][]{};
        argsReduceMapping[3] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 5 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 5 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 5 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 5 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 5 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 5 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 5 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 5 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 5 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 5 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 5 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 5 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 5 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 5 /*production*/},
                };
        argsReduceMapping[4] = new Object[][]{};
        argsReduceMapping[5] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 6 /*production*/},
                };
        argsReduceMapping[6] = new Object[][]{};
        argsReduceMapping[7] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 7 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 7 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 7 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 7 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 7 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 7 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 7 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 7 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 7 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 7 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 7 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 7 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 7 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 7 /*production*/},
                };
        argsReduceMapping[8] = new Object[][]{};
        argsReduceMapping[9] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 8 /*production*/},
                };
        argsReduceMapping[10] = new Object[][]{};
        argsReduceMapping[11] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 9 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 9 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 9 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 9 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 9 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 9 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 9 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 9 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 9 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 9 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 9 /*production*/},
                };
        argsReduceMapping[12] = new Object[][]{};
        argsReduceMapping[13] = new Object[][]{};
        argsReduceMapping[14] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 10 /*production*/},
                };
        argsReduceMapping[15] = new Object[][]{};
        argsReduceMapping[16] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 13 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 13 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 13 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 13 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, LexicalTokenType.SIGN_LESS /*excludeFollowing*/, 13 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, LexicalTokenType.SIGN_GREATER/*excludeFollowing*/, 13 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 13 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 13 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 13 /*production*/},
                };
        argsReduceMapping[17] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, LexicalTokenType.SIGN_LESS /*excludeFollowing*/, 12 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, LexicalTokenType.SIGN_GREATER/*excludeFollowing*/, 12 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 12 /*production*/},
                };
        argsReduceMapping[18] = new Object[][]{};
        argsReduceMapping[19] = new Object[][]{};
        argsReduceMapping[20] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 11 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 11 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 11 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 11 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 11 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 11 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 11 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 11 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 11 /*production*/},
                };
        argsReduceMapping[21] = new Object[][]{};
        argsReduceMapping[22] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 15 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 15 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 15 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 15 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 15 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 15 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 15 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 15 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 15 /*production*/},
                };
        argsReduceMapping[23] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 14 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 14 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 14 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 14 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 14 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 14 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 14 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 14 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 14 /*production*/},
                };
        argsReduceMapping[24] = new Object[][]{};
        argsReduceMapping[25] = new Object[][]{};
        argsReduceMapping[26] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 16 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 16 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 16 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 16 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 16 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 16 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 16 /*production*/},
                };
        argsReduceMapping[27] = new Object[][]{};
        argsReduceMapping[28] = new Object[][]{};
        argsReduceMapping[29] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 17 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 17 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 17 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 17 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 17 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 17 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 17 /*production*/},
                };
        argsReduceMapping[30] = new Object[][]{};
        argsReduceMapping[31] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 18 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 18 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 18 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 18 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 18 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 18 /*production*/},
                };
        argsReduceMapping[32] = new Object[][]{};
        argsReduceMapping[33] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  LexicalTokenType.SIGN_AMPERSAND/*acceptFollowing*/, null/*excludeFollowing*/, 21 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 21 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 21 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 21 /*production*/},
                };
        argsReduceMapping[34] = new Object[][]{};
        argsReduceMapping[35] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  LexicalTokenType.SIGN_AMPERSAND/*acceptFollowing*/, null/*excludeFollowing*/, 19 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 19 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 19 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 19 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 19 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 19 /*production*/},
                };
        argsReduceMapping[36] = new Object[][]{};
        argsReduceMapping[37] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  LexicalTokenType.SIGN_AMPERSAND/*acceptFollowing*/, null/*excludeFollowing*/, 20 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 20 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 20 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 20 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 20 /*production*/},
                };
        argsReduceMapping[38] = new Object[][]{};
        argsReduceMapping[39] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  LexicalTokenType.SIGN_VERTICAL_BAR/*acceptFollowing*/, null/*excludeFollowing*/, 22 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 22 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 22 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 22 /*production*/},
                };
        argsReduceMapping[40] = new Object[][]{};
        argsReduceMapping[41] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*production*/},
                        {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_LEFT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 32 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 28 /*production*/},
                };
        argsReduceMapping[42] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 29 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 29 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 29 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 29 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 29 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 29 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 29 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 29 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 29 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 29 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 29 /*production*/},
                        {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 29 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 29 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 29 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 29 /*production*/},
                };
        argsReduceMapping[43] = new Object[][]{};
        argsReduceMapping[44] = new Object[][]{};
        argsReduceMapping[45] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*production*/},
                        {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 2 /*production*/},
                };
        argsReduceMapping[46] = new Object[][]{};
        argsReduceMapping[47] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 3 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 3 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 3 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 3 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 3 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 3 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 3 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 3 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 3 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 3 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 3 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 3 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 3 /*production*/},
                };
        argsReduceMapping[48] = new Object[][]{};
        argsReduceMapping[49] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 4 /*production*/},
                };
        argsReduceMapping[50] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 23 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 23 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 23 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 23 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 23 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 23 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 23 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 23 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 23 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 23 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 23 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 23 /*production*/},
                        {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 23 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 23 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 23 /*production*/},
                };
        argsReduceMapping[51] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*production*/},
                        {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_LEFT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 32 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 24 /*production*/},
                };
        argsReduceMapping[52] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*production*/},
                        {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 25 /*production*/},
                };
        argsReduceMapping[53] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 26 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 26 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 26 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 26 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 26 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 26 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 26 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 26 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 26 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 26 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 26 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 26 /*production*/},
                        {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 26 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 26 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 26 /*production*/},
                };
        argsReduceMapping[54] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*production*/},
                        {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 27 /*production*/},
                };
        argsReduceMapping[55] = new Object[][]{};
        argsReduceMapping[56] = new Object[][]{};
        argsReduceMapping[57] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*production*/},
                        {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 30 /*production*/},
                };
        argsReduceMapping[58] = new Object[][]{};
        argsReduceMapping[59] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 31 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 31 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 31 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 31 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 31 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 31 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 31 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 31 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 31 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 31 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 31 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 31 /*production*/},
                        {  LexicalTokenType.SIGN_DOT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 31 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 31 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 31 /*production*/},
                };
        argsReduceMapping[60] = new Object[][]{};
        argsReduceMapping[61] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 34 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 34 /*production*/},
                };
        argsReduceMapping[62] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 33 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 33 /*production*/},
                };
        argsReduceMapping[63] = new Object[][]{};
        argsReduceMapping[64] = new Object[][]
                {
                        {  LexicalTokenType.SIGN_ADD /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 35 /*production*/},
                        {  LexicalTokenType.SIGN_MINUS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 35 /*production*/},
                        {  LexicalTokenType.SIGN_ASTERISK /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 35 /*production*/},
                        {  LexicalTokenType.SIGN_PERCENT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 35 /*production*/},
                        {  LexicalTokenType.SIGN_SLASH /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 35 /*production*/},
                        {  LexicalTokenType.SIGN_EXCLAMDOWN /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 35 /*production*/},
                        {  LexicalTokenType.SIGN_AMPERSAND /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 35 /*production*/},
                        {  LexicalTokenType.SIGN_CARET /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 35 /*production*/},
                        {  LexicalTokenType.SIGN_VERTICAL_BAR /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 35 /*production*/},
                        {  LexicalTokenType.SIGN_LESS /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 35 /*production*/},
                        {  LexicalTokenType.SIGN_GREATER /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 35 /*production*/},
                        {  LexicalTokenType.SIGN_COMMA /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 35 /*production*/},
                        {  LexicalTokenType.SIGN_PAREN_RIGHT /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 35 /*production*/},
                        {  LexicalTokenType.END /*acceptLeading*/,  null/*acceptFollowing*/, null/*excludeFollowing*/, 35 /*production*/},
                };

        Object[] argsGotoMapping = new Object[STATE_SIZE];
        argsGotoMapping[0] = new Integer[]{ 1, 50, 54, 55, null};
        argsGotoMapping[1] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[2] = new Integer[]{ 3, 50, 54, 55, null};
        argsGotoMapping[3] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[4] = new Integer[]{ 5, 50, 54, 55, null};
        argsGotoMapping[5] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[6] = new Integer[]{ 7, 50, 54, 55, null};
        argsGotoMapping[7] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[8] = new Integer[]{ 9, 50, 54, 55, null};
        argsGotoMapping[9] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[10] = new Integer[]{ 11, 50, 54, 55, null};
        argsGotoMapping[11] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[12] = new Integer[]{ 17, 50, 54, 55, null};
        argsGotoMapping[13] = new Integer[]{ 14, 50, 54, 55, null};
        argsGotoMapping[14] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[15] = new Integer[]{ 16, 50, 54, 55, null};
        argsGotoMapping[16] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[17] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[18] = new Integer[]{ 23, 50, 54, 55, null};
        argsGotoMapping[19] = new Integer[]{ 20, 50, 54, 55, null};
        argsGotoMapping[20] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[21] = new Integer[]{ 22, 50, 54, 55, null};
        argsGotoMapping[22] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[23] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[24] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[25] = new Integer[]{ 26, 50, 54, 55, null};
        argsGotoMapping[26] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[27] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[28] = new Integer[]{ 26, 50, 54, 55, null};
        argsGotoMapping[29] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[30] = new Integer[]{ 31, 50, 54, 55, null};
        argsGotoMapping[31] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[32] = new Integer[]{ 33, 50, 54, 55, null};
        argsGotoMapping[33] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[34] = new Integer[]{ 35, 50, 54, 55, null};
        argsGotoMapping[35] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[36] = new Integer[]{ 37, 50, 54, 55, null};
        argsGotoMapping[37] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[38] = new Integer[]{ 39, 50, 54, 55, null};
        argsGotoMapping[39] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[40] = new Integer[]{ null, null, 42, 55, null};
        argsGotoMapping[41] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[42] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[43] = new Integer[]{ 44, 50, 54, 55, null};
        argsGotoMapping[44] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[45] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[46] = new Integer[]{ 47, 50, 54, 55, null};
        argsGotoMapping[47] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[48] = new Integer[]{ 49, 50, 54, 55, null};
        argsGotoMapping[49] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[50] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[51] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[52] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[53] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[54] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[55] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[56] = new Integer[]{ 62, 50, 54, 55, 58};
        argsGotoMapping[57] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[58] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[59] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[60] = new Integer[]{ 61, 50, 54, 55, 58};
        argsGotoMapping[61] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[62] = new Integer[]{ null, null, null, null, null};
        argsGotoMapping[63] = new Integer[]{ 64, 50, 54, 55, null};
        argsGotoMapping[64] = new Integer[]{ null, null, null, null, null};

        for (int i = 0; i < STATE_SIZE; i++)
        {
            State state = new State(i);
            Object[][] argShiftMapping = (Object[][]) argsShiftMapping[i];
            for (int j = 0; j < argShiftMapping.length; j++)
            {
                final Object[] r = argShiftMapping[j];
                assert (r.length == 4);

                Set<LexicalTokenType> excludeFollowing = new HashSet<LexicalTokenType>(){
                    {
                        if (r[2] != null)
                            add((LexicalTokenType)r[2]);
                    }
                };
                state.addShiftMapping((LexicalTokenType)r[0], (LexicalTokenType)r[1], excludeFollowing, (Integer) r[3]);
            }

            Object[][] argReduceMapping = (Object[][]) argsReduceMapping[i];
            assert (argReduceMapping != null);
            for (int j = 0; j < argReduceMapping.length; j++)
            {
                final Object[] r = argReduceMapping[j];
                assert (r.length == 4);

                Set<LexicalTokenType> excludeFollowing = new HashSet<LexicalTokenType>(){
                    {
                        if (r[2] != null)
                            add((LexicalTokenType)r[2]);
                    }
                };
                state.addReduceMapping((LexicalTokenType)r[0], (LexicalTokenType)r[1], excludeFollowing, (Integer) r[3]);
            }

            Integer[] argGotoMapping = (Integer[]) argsGotoMapping[i];
            assert (argGotoMapping.length == 5);
            if (argGotoMapping[0] != null)
                state.addGotoMapping(NOTERMINAL_E, argGotoMapping[0]);
            if (argGotoMapping[1] != null)
                state.addGotoMapping(NOTERMINAL_OBJ, argGotoMapping[1]);
            if (argGotoMapping[2] != null)
                state.addGotoMapping(NOTERMINAL_FUNC, argGotoMapping[2]);
            if (argGotoMapping[3] != null)
                state.addGotoMapping(NOTERMINAL_FUNC_NAME, argGotoMapping[3]);
            if (argGotoMapping[4] != null)
                state.addGotoMapping(NOTERMINAL_ARGS, argGotoMapping[4]);

            mappingState.put(i, state);
        }
    }

    public boolean isFinished()
    {
        return finished;
    }

    //移入元素-终结符, 并进入特定状态, 参数 state 为状态的id
    void shiftTerminal(final LexicalToken l, int state)
    {
        if (state == STATE_ACC)
        {
            finished = true;
            return ;
        }

        stackSymbol.addFirst(new Symbol(Symbol.Type.TERMINAL, l.type.ordinal()));
        stackAttribute.addFirst(new HashMap<String, Object>(){
            {
                put(ATTRIBUTE_TERMINAL_VALUE, l.value);
                put(ATTRIBUTE_TERMINAL_TYPE, l.type);
                put(ATTRIBUTE_POSITION_LINE, l.line);
                put(ATTRIBUTE_POSITION_COLUMN, l.position);
            }
        });
        gotoState(state);
    }

    //移入元素-非终结符, 并进入特定状态
    void shiftNonterminal(int nonterminal, int state, Map<String, Object> attributes)
    {
        stackSymbol.addFirst(new Symbol(Symbol.Type.NONTERMINAL, nonterminal));
        stackAttribute.addFirst(attributes);
        gotoState(state);
    }

    private void gotoState(int state)
    {
        State s = mappingState.get(state);
        assert (s != null);
        stackState.addFirst(s);
    }

    //规约, 用 id 产生式进行规约
    Pair<Boolean, String> reduce(int id)
    {
        Production p = mappingProduction.get(id);
        assert (p != null);
        Production.Reducer.Result r = p.reduce(this);
        if (!r.isOk)
            return new Pair<>(r.isOk, r.errString);
        assert(stackState.size() != 0);
        return stackState.getFirst().write(this, p.getNonterminal(), r.attributes);
    }

    void pop(int size)
    {
        for (int i = 0; i < size; i++)
        {
            stackState.removeFirst();
            stackSymbol.removeFirst();
            stackAttribute.removeFirst();
        }
    }

    // 从属性栈内获取属性集. -1 为第一个项(栈顶), -2 为第二个项..
    Map<String, Object> getAttributeFromStack(int index)
    {
        assert (index < 0);
        int i = -index - 1;
        return stackAttribute.get(i);
    }

    public Pair<IElement, String> analyze(List<LexicalToken> tokens, FunctionElementFactory funcFactory, VariableElementFactory varFactory, ClassDictionary classDictionary)
    {
        if (funcFactory != null)
            this.funcFactory = funcFactory;

        if (varFactory != null)
            this.varFactory = varFactory;

        if (classDictionary != null)
            this.classDictionary = classDictionary;

        stackState.clear();
        stackSymbol.clear();
        stackAttribute.clear();
        gotoState(0);   //初始状态0
        finished = false;

        IElement e = null;
        String errString = "";

        boolean success = true;

        int size = tokens.size();
        for (int i = 0; i < size;)
        {
            LexicalToken t1 = tokens.get(i), t2 = i < size - 1 ? tokens.get(i + 1) : null;

            State.Error err = stackState.getFirst().write(this, t1, t2);
            if (!err.isOk)
            {
                success = false;
                errString = err.errString;
                break;
            }
            else if (err.shifted)
            {
                i++;
            }
        }

        if (success)
        {
            e = (IElement)stackAttribute.getFirst().get(ATTRIBUTE_ELEMENT);
        }
        return new Pair<>(e, errString);
    }
}
