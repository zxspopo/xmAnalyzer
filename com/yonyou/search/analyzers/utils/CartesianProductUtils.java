package com.yonyou.search.analyzers.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 计算笛卡尔积
 * Created by zengxs on 2016/2/25.
 */
public class CartesianProductUtils {


    public static String[] calc(String[][] table) {
        int len = table.length;
        int loop = 1;
        int[] counter = new int[len];
        int countIndex = len - 1;
        for (int i = 0; i < len; i++) {
            loop *= table[i].length;
        }
        String[] result = new String[loop];
        for (int i = 0; i < loop; i++) {
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < len; j++) {
                builder.append(table[j][counter[j]]);
            }
            result[i] = builder.toString();
            countIndex = handle(countIndex, counter, table);
        }
        return result;
    }


    public static int handle(int counterIndex, int[] counter, String[][] table) {
        counter[counterIndex]++;
        if (counter[counterIndex] >= table[counterIndex].length) {
            counter[counterIndex] = 0;
            counterIndex--;
            if (counterIndex >= 0) {
                handle(counterIndex, counter, table);
            }
            counterIndex = table.length - 1;
        }
        return counterIndex;
    }
}
