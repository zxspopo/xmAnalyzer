package com.yonyou.search.analyzers;

import java.io.IOException;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class PinyinTransformTokenFilter extends TokenFilter
{
    private boolean isOutChinese = true;
    private final CharTermAttribute termAtt = (CharTermAttribute)addAttribute(CharTermAttribute.class);
    HanyuPinyinOutputFormat outputFormat = new HanyuPinyinOutputFormat();
    private boolean _isFirstChar = false;
    private int _minTermLenght = 2;
    private char[] curTermBuffer;
    private int curTermLength;
    private boolean outChinese = true;

    public PinyinTransformTokenFilter(TokenStream input)
    {
        this(input, false, 2);
    }

    public PinyinTransformTokenFilter(TokenStream input, boolean isFirstChar) {
        this(input, isFirstChar, 2);
    }

    public PinyinTransformTokenFilter(TokenStream input, boolean isFirstChar, int minTermLenght) {
        this(input, isFirstChar, minTermLenght, true);
    }
    public PinyinTransformTokenFilter(TokenStream input, boolean isFirstChar, int minTermLenght, boolean isOutChinese) {
        super(input);
        this._isFirstChar = isFirstChar;
        this._minTermLenght = minTermLenght;
        if (this._minTermLenght < 1) {
            this._minTermLenght = 1;
        }
        this.isOutChinese = isOutChinese;
        this.outputFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        this.outputFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
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

    public final boolean incrementToken() throws IOException
    {
        while (true) {
            if (this.curTermBuffer == null) {
                if (!this.input.incrementToken()) {
                    return false;
                }
                this.curTermBuffer = ((char[])this.termAtt.buffer().clone());
                this.curTermLength = this.termAtt.length();
            }

            if ((this.isOutChinese) && (this.outChinese)) {
                this.outChinese = false;
                this.termAtt.copyBuffer(this.curTermBuffer, 0, this.curTermLength);
                return true;
            }
            this.outChinese = true;
            String chinese = this.termAtt.toString();

            if (containsChinese(chinese)) {
                this.outChinese = true;
                if (chinese.length() >= this._minTermLenght) {
                    try {
                        String chineseTerm = GetPyString(chinese);
                        this.termAtt.copyBuffer(chineseTerm.toCharArray(), 0, chineseTerm.length());
                    } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
                        badHanyuPinyinOutputFormatCombination.printStackTrace();
                    }
                    this.curTermBuffer = null;
                    return true;
                }

            }

            this.curTermBuffer = null;
        }
    }

    public void reset()
            throws IOException
    {
        super.reset();
    }

    private String GetPyString(String chinese)
            throws BadHanyuPinyinOutputFormatCombination
    {
        String chineseTerm;
        if (this._isFirstChar) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < chinese.length(); i++) {
                String[] array = PinyinHelper.toHanyuPinyinStringArray(chinese.charAt(i), this.outputFormat);
                if ((array != null) && (array.length != 0))
                {
                    String s = array[0];
                    char c = s.charAt(0);

                    sb.append(c);
                }
            }
            chineseTerm = sb.toString();
        } else {
            chineseTerm = PinyinHelper.toHanyuPinyinString(chinese, this.outputFormat, "");
        }
        return chineseTerm;
    }
}