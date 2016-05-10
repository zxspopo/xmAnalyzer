package com.yonyou.iuap.search.analyzer;

import java.util.Map;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

public class PinyinNGramTokenFilterFactory extends TokenFilterFactory {
    private int minGram = 2;
    private int maxGram = 20;
    private boolean isNGramChinese = false;

    public PinyinNGramTokenFilterFactory(Map<String, String> args) {
        super(args);

        this.minGram = getInt(args, "minGram", 2);
        this.maxGram = getInt(args, "maxGram", 20);
        this.isNGramChinese = getBoolean(args, "isNGramChinese", false);
        if (!args.isEmpty())
            throw new IllegalArgumentException("Unknown parameters: " + args);
    }

    public TokenFilter create(TokenStream input) {
        return new PinyinNGramTokenFilter(input, this.minGram, this.maxGram, this.isNGramChinese);
    }
}
