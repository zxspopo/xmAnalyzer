package com.yonyou.search.analyzer.test;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.ngram.NGramTokenizer;

import com.yonyou.iuap.search.analyzer.PinyinNameTransformTokenFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public final class PinyinNGramAnalyzer extends Analyzer {
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer tokenizer = new StandardTokenizer(reader);
        TokenStream ts = new NGramTokenFilter(tokenizer,1,3);
        TokenStream result = new PinyinNameTransformTokenFilter(ts,false,1);
        return new TokenStreamComponents(tokenizer, result);
    }
}
