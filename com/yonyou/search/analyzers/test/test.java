package com.yonyou.search.analyzers.test;

import com.yonyou.search.analyzers.PinyinNGramAnalyzer;
import com.yonyou.search.analyzers.PinyinNameTransformTokenFilter;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.StringReader;
import java.util.Date;

public class test {
    private static final SmartChineseAnalyzer defaultAnalyzer = new SmartChineseAnalyzer(true);

    private static final PinyinNGramAnalyzer PinyinAnalyzer = new PinyinNGramAnalyzer();

    public static void main(String[] args) throws Exception {
        testPinyin2();
    }

    public static void testPinyin() throws Exception {
        System.out.println("testPinyin");
        Date begin = new Date();

        String content = "testGetHotWords 用时 中华人民共和国 公民 日本 人人有责 曾宪盛";
        PringString2(content);

        Date end = new Date();
        long SearchTime = end.getTime() - begin.getTime();
        System.out.println("testGetHotWords用时:" + SearchTime / 1000.0D + "(秒)");
    }


    public static void testPinyin2() throws Exception {
        System.out.println("testPinyin");
        Date begin = new Date();

        String content = "wxc订制品1";
        PringString2(content);

        Date end = new Date();
        long SearchTime = end.getTime() - begin.getTime();
        System.out.println("用时:" + SearchTime / 1000.0D + "(秒)");
    }

    public static String getPinyinString(String value)
            throws Exception {
        HanyuPinyinOutputFormat outputFormat = new HanyuPinyinOutputFormat();
        outputFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);

        outputFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);

        TokenStream ts = defaultAnalyzer.tokenStream(null, new StringReader(value));

        ts.reset();
        CharTermAttribute ta = (CharTermAttribute) ts.addAttribute(CharTermAttribute.class);

        String allPinyin = "";

        String chinese = "";

        while (ts.incrementToken()) {
            chinese = ta.toString();
            if ((containsChinese(chinese)) &&
                    (chinese.length() >= 1)) {
                allPinyin = allPinyin + PinyinHelper.toHanyuPinyinString(chinese, outputFormat, "") + " ";
            }

        }

        System.out.println(allPinyin);
        return allPinyin;
    }

    public static boolean containsChinese(String s) {
        if ((null == s) || ("".equals(s.trim())))
            return false;
        for (int i = 0; i < s.length(); i++) {
            if (isChinese(s.charAt(i)))
                return true;
        }
        return false;
    }

    public static boolean isChinese(char a) {
        int v = a;
        return (v >= 19968) && (v <= 171941);
    }

    public static void PringString2(String value)
            throws Exception {

        StopAnalyzer standardAnalyzer = new StopAnalyzer();

        TokenStream ts = standardAnalyzer.tokenStream(null, new StringReader(value));

        PinyinNameTransformTokenFilter tokenFilter = new PinyinNameTransformTokenFilter(ts, false, 1);

        tokenFilter.reset();
        CharTermAttribute ta = (CharTermAttribute) tokenFilter.addAttribute(CharTermAttribute.class);

        String chinese = "";

        while (tokenFilter.incrementToken()) {
            chinese = chinese + ta.toString() + " ";
        }

        System.out.println(chinese);
    }

    public static void PringString1(String value)
            throws Exception {
        TokenStream ts = PinyinAnalyzer.tokenStream(null, new StringReader(value));

        ts.reset();
        CharTermAttribute ta = (CharTermAttribute) ts.addAttribute(CharTermAttribute.class);

        String chinese = "";

        while (ts.incrementToken()) {
            System.out.println(ta.toString());
            chinese = chinese + ta.toString() + " ";
        }

        System.out.println(chinese);
    }
}

/* Location:           D:\env\solrcloud-single\solr-8983\tomcat\apache-tomcat-8.0.24\webapps\solr\WEB-INF\lib\pinyinAnalyzer4.3.1.jar
 * Qualified Name:     com.shentong.search.analyzers.test.test
 * JD-Core Version:    0.6.2
 */