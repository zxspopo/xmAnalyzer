package com.yonyou.search.analyzer.test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PackedTokenAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;
import org.junit.Test;

public class TestIndex {
    private static final SmartChineseAnalyzer defaultAnalyzer = new SmartChineseAnalyzer(true);

    private static final PinyinNGramAnalyzer PinyinAnalyzer = new PinyinNGramAnalyzer();

    @Test
    public void testPinyin2() throws Exception {
        System.out.println("testPinyin");
        Date begin = new Date();

        String content = "百度123";
        PringString1(content);

        Date end = new Date();
        long SearchTime = end.getTime() - begin.getTime();
        System.out.println("用时:" + SearchTime / 1000.0D + "(秒)");
    }


    public void PringString1(String value) throws Exception {
        TokenStream ts = PinyinAnalyzer.tokenStream(null, new StringReader(value));

        ts.reset();
        OffsetAttribute offsetAttribute = ts.addAttribute(OffsetAttribute.class);
        PositionIncrementAttribute positionIncrementAttribute = ts.addAttribute(PositionIncrementAttribute.class);
        CharTermAttribute charTermAttribute = ts.addAttribute(CharTermAttribute.class);
        TypeAttribute typeAttribute = ts.addAttribute(TypeAttribute.class);
        String chinese = "";
        System.out.println("startoffset \t endoffset \t positionLength \t result");
        int position = 0;
        List<AttributeSource.State> stateList = new ArrayList<>();
        while (ts.incrementToken()) {
            stateList.add(ts.captureState());
            int increment = positionIncrementAttribute.getPositionIncrement();
            if (increment > 0) {
                position = position + increment;
                System.out.print(position + ":");
            }
            int startOffset = offsetAttribute.startOffset();
            int endOffset = offsetAttribute.endOffset();
            String term = charTermAttribute.toString();
            System.out.println("[" + term + "]" + ":(" + startOffset + "-->" + endOffset + "):" + typeAttribute.type());
        }

        System.out.println(chinese);
        ts.close();
    }

    @Test
    public void getPinyinString() throws Exception {
        String value = "百度123";
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
            if ((containsChinese(chinese)) && (chinese.length() >= 1)) {
                allPinyin = allPinyin + PinyinHelper.toHanyuPinyinString(chinese, outputFormat, "") + " ";
            }

        }

        System.out.println(allPinyin);
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
}
