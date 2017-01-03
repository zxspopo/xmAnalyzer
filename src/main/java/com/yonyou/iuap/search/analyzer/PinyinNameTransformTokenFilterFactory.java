package com.yonyou.iuap.search.analyzer;

import java.util.Map;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

public class PinyinNameTransformTokenFilterFactory extends TokenFilterFactory {
    private boolean isFirstChar = false;
    private int minTermLenght = 2;


    public PinyinNameTransformTokenFilterFactory(Map<String, String> args) {
        super(args);

        this.isFirstChar = getBoolean(args, "isFirstChar", false);
        this.minTermLenght = getInt(args, "minTermLenght", 2);

        if (!args.isEmpty())
            throw new IllegalArgumentException("Unknown parameters: " + args);
    }

    public TokenFilter create(TokenStream input) {
        return new PinyinNameTransformTokenFilter(input, this.isFirstChar, this.minTermLenght);
    }
}
