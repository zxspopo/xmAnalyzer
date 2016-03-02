package com.yonyou.search.analyzers.test;

import com.yonyou.search.analyzers.utils.Pinyin4jUtil;

/*******************************************************************************
 * pinyin4j is a plug-in, you can kind of Chinese characters into phonetic.Multi-tone character,Tone
 * Detailed view http://pinyin4j.sourceforge.net/
 *
 * @author Administrator
 * @ClassName: Pinyin4jUtil
 * @Description: TODO
 * @author wang_china@foxmail.com
 * @date Jan 13, 2012 9:28:28 AM
 */
public class TestDuoyinzi {

    /***************************************************************************
     * Test
     *
     * @Name: Pinyin4jUtil.java
     * @Description: TODO
     * @author: wang_chian@foxmail.com
     * @version: Jan 13, 2012 9:49:27 AM
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
