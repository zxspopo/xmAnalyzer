package com.yonyou.search.analyzer.test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;
import org.junit.Before;
import org.junit.Test;

import com.yonyou.iuap.search.analyzer.PinyinNameTransformTokenFilter;

/**
 * Created by zengxs on 2016/12/31.
 */
public class TestHighlight {

    public Analyzer analyzer;

    @Before
    public void init() {
        analyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String text, Reader reader) {
                Tokenizer tokenizer = new StandardTokenizer(reader);
                TokenStream ngramFilter = new NGramTokenFilter(tokenizer, 1, 3);
                TokenStream pinyinTokenFilter = new PinyinNameTransformTokenFilter(ngramFilter, false, 1);
                TokenStream tokenOrderingFilter = new TokenOrderingFilter(pinyinTokenFilter, 10);
                return new TokenStreamComponents(tokenizer, tokenOrderingFilter);
            }
        };
    }

    @Test
    public void testHighlight() throws IOException {
        TokenStream ts = analyzer.tokenStream(null, new StringReader("百度123"));
        ts.reset();
        OffsetAttribute offsetAttribute = ts.addAttribute(OffsetAttribute.class);
        PositionIncrementAttribute positionIncrementAttribute = ts.addAttribute(PositionIncrementAttribute.class);
        CharTermAttribute charTermAttribute = ts.addAttribute(CharTermAttribute.class);
        TypeAttribute typeAttribute = ts.addAttribute(TypeAttribute.class);
        int position = 0;
        while (ts.incrementToken()) {
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

        ts.close();
    }

}


final class TokenOrderingFilter extends TokenFilter {
    private final int windowSize;
    private final LinkedList<OrderedToken> queue = new LinkedList<>();
    private boolean done = false;
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

    protected TokenOrderingFilter(TokenStream input, int windowSize) {
        super(input);
        this.windowSize = windowSize;
    }

    @Override
    public boolean incrementToken() throws IOException {
        while (!done && queue.size() < windowSize) {
            if (!input.incrementToken()) {
                done = true;
                break;
            }

            // reverse iterating for better efficiency since we know the
            // list is already sorted, and most token start offsets will be too.
            ListIterator<OrderedToken> iter = queue.listIterator(queue.size());
            while (iter.hasPrevious()) {
                if (offsetAtt.startOffset() >= iter.previous().startOffset) {
                    // insertion will be before what next() would return (what
                    // we just compared against), so move back one so the insertion
                    // will be after.
                    iter.next();
                    break;
                }
            }
            OrderedToken ot = new OrderedToken();
            ot.state = captureState();
            ot.startOffset = offsetAtt.startOffset();
            iter.add(ot);
        }

        if (queue.isEmpty()) {
            return false;
        } else {
            restoreState(queue.removeFirst().state);
            return true;
        }
    }

}


class OrderedToken {
    AttributeSource.State state;
    int startOffset;
}
