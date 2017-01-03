package com.yonyou.search.analyzer;


import com.yonyou.iuap.search.analyzer.utils.Pinyin4jUtil;

public class TestDuoyinzi {

    /***************************************************************************
     * Test
     *
     * @param args
     */
    public static void main(String[] args) {
        String str = "曾宪盛";
        System.out.println("小写输出：" + Pinyin4jUtil.getPinyinToLowerCase(str));
        System.out.println("大写输出：" + Pinyin4jUtil.getPinyinToUpperCase(str));
        System.out.println("首字母大写输出：" + Pinyin4jUtil.getPinyinFirstToUpperCase(str));
        System.out.println("简拼输出：" + Pinyin4jUtil.getPinyinJianPin(str));

    }
}
