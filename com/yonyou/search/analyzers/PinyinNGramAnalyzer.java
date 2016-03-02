package com.yonyou.search.analyzers;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cn.smart.SentenceTokenizer;
import org.apache.lucene.analysis.cn.smart.WordTokenFilter;

public final class PinyinNGramAnalyzer extends Analyzer {
    public Analyzer.TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer tokenizer = new SentenceTokenizer(reader);
        TokenStream result = new WordTokenFilter(tokenizer);
        result = new PinyinTransformTokenFilter(result, false, 2);
        result = new PinyinNGramTokenFilter(result, 2, 20);
        return new Analyzer.TokenStreamComponents(tokenizer, result);
    }
}

