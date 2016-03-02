package com.yonyou.search.analyzers;


import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

public class PinyinNGramTokenFilter extends TokenFilter {
    private final int minGram;
    private final int maxGram;
    private final boolean isNGramChinese;
    private final CharTermAttribute termAtt = (CharTermAttribute) addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = (OffsetAttribute) addAttribute(OffsetAttribute.class);
    private char[] curTermBuffer;
    private int curTermLength;
    private int curGramSize;
    private int tokStart;


    public PinyinNGramTokenFilter(TokenStream input, int minGram, int maxGram) {

        this(input, minGram, maxGram, false);

    }


    public PinyinNGramTokenFilter(TokenStream input, int minGram, int maxGram, boolean isNGramChinese) {

        super(input);


        if (minGram < 1) {

            throw new IllegalArgumentException("minGram must be greater than zero");

        }

        if (minGram > maxGram) {

            throw new IllegalArgumentException("minGram must not be greater than maxGram");

        }

        this.minGram = minGram;

        this.maxGram = maxGram;

        this.isNGramChinese = isNGramChinese;

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


    public final boolean incrementToken() throws IOException {

        while (true) {

            if (this.curTermBuffer == null) {

                if (!this.input.incrementToken()) {

                    return false;

                }

                if ((!this.isNGramChinese) && (containsChinese(this.termAtt.toString()))) {

                    return true;

                }

                this.curTermBuffer = ((char[]) this.termAtt.buffer().clone());


                this.curTermLength = this.termAtt.length();

                this.curGramSize = this.minGram;

                this.tokStart = this.offsetAtt.startOffset();

            }


            if (this.curGramSize <= this.maxGram) {

                if (this.curGramSize >= this.curTermLength) {

                    clearAttributes();

                    this.offsetAtt.setOffset(this.tokStart + 0, this.tokStart + this.curTermLength);

                    this.termAtt.copyBuffer(this.curTermBuffer, 0, this.curTermLength);

                    this.curTermBuffer = null;

                    return true;

                }

                int start = 0;

                int end = start + this.curGramSize;

                clearAttributes();

                this.offsetAtt.setOffset(this.tokStart + start, this.tokStart + end);

                this.termAtt.copyBuffer(this.curTermBuffer, start, this.curGramSize);

                this.curGramSize += 1;

                return true;

            }


            this.curTermBuffer = null;

        }

    }


    public void reset() throws IOException {

        super.reset();

        this.curTermBuffer = null;

    }

}