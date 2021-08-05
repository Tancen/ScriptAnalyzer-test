package org.example.analyzer;

import java.util.HashMap;
import java.util.Map;

public class Priority
{
    static Map<String, Integer> s_priorities = new HashMap<String, Integer>(){
        {
            put("()", 1);
            put("[]", 1);
            put(".", 1);
            put("!", 3);
            put("~", 3);
            put("*", 4);
            put("/", 4);
            put("%", 4);
            put("-", 5);
            put("+", 5);
            put("<<", 6);
            put(">>", 6);
            put(">", 7);
            put(">=", 7);
            put("<", 7);
            put("<=", 7);
            put("==", 8);
            put("!=", 8);
            put("&", 9);
            put("^", 10);
            put("|", 11);
            put("&", 12);
            put("||", 13);
        }
    };

    //返回 op1 的优先级是否高于 op2, (同级时返回true)
    public static boolean check(String op1, String op2)
    {
        Integer p1 = s_priorities.get(op1);
        Integer p2 = s_priorities.get(op2);

        if (p1 == null && p2 == null)
            return false;

        if (p1 == null)
            return true;

        if (p2 == null)
            return false;

        return p1 <= p2;
    }
}
