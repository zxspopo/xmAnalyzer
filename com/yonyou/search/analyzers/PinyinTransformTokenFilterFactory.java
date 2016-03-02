package com.yonyou.search.analyzers;

import java.util.Map;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

public class PinyinTransformTokenFilterFactory extends TokenFilterFactory {
    private boolean isFirstChar = false;
    private boolean isOutChinese = true;
    private int minTermLenght = 2;


    public PinyinTransformTokenFilterFactory(Map<String, String> args) {
        super(args);

        this.isFirstChar = getBoolean(args, "isFirstChar", false);
        this.isFirstChar = getBoolean(args, "isFirstChar", false);
        this.isOutChinese = getBoolean(args, "isOutChinese", true);
        this.minTermLenght = getInt(args, "minTermLenght", 2);

        if (!args.isEmpty())
            throw new IllegalArgumentException("Unknown parameters: " + args);
    }

    public TokenFilter create(TokenStream input) {
        return new PinyinTransformTokenFilter(input, this.isFirstChar, this.minTermLenght, this.isOutChinese);
    }
}

/* Location:           D:\env\solrcloud-single\solr-8983\tomcat\apache-tomcat-8.0.24\webapps\solr\WEB-INF\lib\pinyinAnalyzer4.3.1.jar
 * Qualified Name:     com.shentong.search.analyzers.PinyinTransformTokenFilterFactory
 * JD-Core Version:    0.6.2
 */