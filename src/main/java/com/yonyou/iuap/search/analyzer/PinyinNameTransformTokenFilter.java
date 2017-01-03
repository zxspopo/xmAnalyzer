package com.yonyou.iuap.search.analyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yonyou.iuap.search.analyzer.utils.CartesianProductUtils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 拼音分词器，转换索引简拼，全拼，姓全拼+名简拼 如果不是姓名(>4个汉字字符)，只处理全拼和简拼
 */
public class PinyinNameTransformTokenFilter extends TokenFilter {

    private static final Logger logger = LoggerFactory.getLogger(PinyinNameTransformTokenFilter.class);

    private final CharTermAttribute termAtt = (CharTermAttribute) addAttribute(CharTermAttribute.class);

    private OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

    private PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);

    private PositionLengthAttribute posLenAtt = addAttribute(PositionLengthAttribute.class);

    private static final HanyuPinyinOutputFormat outputFormat = new HanyuPinyinOutputFormat();

    private static final ThreadLocal<Variable> context = new ThreadLocal<Variable>() {
        @Override
        protected Variable initialValue() {
            return new Variable();
        }
    };

    private boolean _isFirstChar = false;
    private int _minTermLenght = 2;

    public PinyinNameTransformTokenFilter(TokenStream input) {
        this(input, false, 2);
    }

    public PinyinNameTransformTokenFilter(TokenStream input, boolean isFirstChar) {
        this(input, isFirstChar, 2);
    }

    public PinyinNameTransformTokenFilter(TokenStream input, boolean isFirstChar, int minTermLenght) {
        super(input);
        this._isFirstChar = isFirstChar;
        this._minTermLenght = minTermLenght;
        if (this._minTermLenght < 1) {
            this._minTermLenght = 1;
        }
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

    public final boolean incrementToken() throws IOException {
        while (true) {
            if (context.get().getCurTermBuffer() == null) {
                if (!this.input.incrementToken()) {
                    context.remove();
                    reset();
                    return false;
                }
                context.get().setResetFlag(false);
                context.get().setCurTermBuffer((char[]) this.termAtt.buffer().clone());
                context.get().setCurTermLength(this.termAtt.length());
                context.get().setTokStart(this.termAtt.length());
                context.get().setTokStart(offsetAtt.startOffset());
                context.get().setTokEnd(offsetAtt.endOffset());
                context.get().setCurPosInc(posIncAtt.getPositionIncrement());
                context.get().setCurPosLen(posLenAtt.getPositionLength());
            }

            this.termAtt.copyBuffer(context.get().getCurTermBuffer(), 0, context.get().getCurTermLength());
            offsetAtt.setOffset(context.get().getTokStart(), context.get().getTokEnd());
            posIncAtt.setPositionIncrement(context.get().getCurPosInc());
            context.get().setCurPosInc(0);
            posLenAtt.setPositionLength(context.get().getCurPosLen());
            if (context.get().getChineseTerms() == null) {
                String chinese = this.termAtt.toString();
                if (containsChinese(chinese) && chinese.length() >= this._minTermLenght) {
                    try {
                        context.get().setChineseTerms(getPyString(chinese));
                    } catch (BadHanyuPinyinOutputFormatCombination e) {
                        logger.error("Fail to getPinyin from [" + chinese + "]", e);
                    }
                } else {
                    context.remove();
                }
                return true;
            }

            if (context.get().getChineseTerms() != null) {
                int index = context.get().getIdx().get();
                if (index < context.get().getChineseTerms().length) {
                    clearAttributes();
                    this.termAtt.copyBuffer(context.get().getChineseTerms()[index].toCharArray(), 0, context.get()
                            .getChineseTerms()[index].length());
                    offsetAtt.setOffset(context.get().getTokStart(), context.get().getTokEnd());
                    posIncAtt.setPositionIncrement(context.get().getCurPosInc());
                    posLenAtt.setPositionLength(context.get().getCurPosLen());
                    context.get().getIdx().getAndIncrement();
                    return true;
                }
            }
            context.remove();
            return true;
        }
    }

    public void reset() throws IOException {
        super.reset();
    }

    /**
     * 处理汉字的拼音，如果汉字<=4,输出全拼，简拼，1（全拼）+n-1(简拼)，2（全拼）+n-1(简拼) <br/>
     * 如果汉字>4，输出全拼，简拼
     *
     * @param chinese
     * @return
     * @throws BadHanyuPinyinOutputFormatCombination
     */
    private String[] getPyString(String chinese) throws BadHanyuPinyinOutputFormatCombination {
        String[] jianpinTable = findPinyin(chinese, true);
        String[] quanpinTable = findPinyin(chinese, false);
        String[] partTable = new String[jianpinTable.length + quanpinTable.length];
        System.arraycopy(jianpinTable, 0, partTable, 0, jianpinTable.length);
        System.arraycopy(quanpinTable, 0, partTable, jianpinTable.length, quanpinTable.length);
        if (getChineseCount(chinese) > 4) {
            return partTable;
        }

        if (chinese.length() > 1) {
            String[] singleNameTable = findJianpin(chinese.substring(0, 1), chinese.substring(1));
            String[] tmp = new String[partTable.length + singleNameTable.length];
            System.arraycopy(partTable, 0, tmp, 0, partTable.length);
            System.arraycopy(singleNameTable, 0, tmp, partTable.length, singleNameTable.length);
            partTable = tmp;
        }

        if (chinese.length() > 2) {
            String[] doubleNameTable = findJianpin(chinese.substring(0, 2), chinese.substring(2));
            String[] tmp = new String[partTable.length + doubleNameTable.length];
            System.arraycopy(partTable, 0, tmp, 0, partTable.length);
            System.arraycopy(doubleNameTable, 0, tmp, partTable.length, doubleNameTable.length);
            partTable = tmp;
        }
        return partTable;
    }

    private String[] findJianpin(String beginwords, String endWords) throws BadHanyuPinyinOutputFormatCombination {
        String[] beginWordsList = findPinyin(beginwords, false);
        String[] endWordsList = findPinyin(endWords, true);
        String[][] table = new String[][] {beginWordsList, endWordsList};
        return CartesianProductUtils.calc(table);
    }

    private String[] findPinyin(String chinese, boolean isFirstChar) throws BadHanyuPinyinOutputFormatCombination {
        WordEntry entry = splitChineseAndEnglish(chinese);
        int len = 0;
        List<String[][]> childTableList = new ArrayList<String[][]>();
        while (entry != null) {
            String words = entry.getWords().toString();
            if (entry.isChinese()) {
                len += words.length();
                String[][] table = new String[words.length()][];
                childTableList.add(table);
                for (int i = 0; i < words.length(); i++) {
                    String[] array = PinyinHelper.toHanyuPinyinStringArray(words.charAt(i), this.outputFormat);
                    String[] child;
                    if ((array != null) && (array.length != 0)) {
                        Set<String> jianpinSet = new LinkedHashSet<>();
                        for (int j = 0; j < array.length; j++) {
                            StringBuilder sb = new StringBuilder();
                            String s = array[j];
                            if (isFirstChar) {
                                char c = s.charAt(0);
                                sb.append(c);
                            } else {
                                sb.append(s);
                            }
                            jianpinSet.add(sb.toString());
                        }
                        child = jianpinSet.toArray(new String[] {});
                    } else {
                        child = new String[] {chinese.charAt(i) + ""};
                    }
                    table[i] = child;
                }
            } else {
                len += 1;
                String[][] table = new String[][] {new String[] {words}};
                childTableList.add(table);
            }
            entry = entry.next;
        }
        String[][] table = new String[len][];
        int i = 0;
        for (String[][] child : childTableList) {
            for (int j = 0; j < child.length; j++) {
                table[i + j] = child[j];
            }
            i += child.length;
        }

        // String[][] table = new String[chinese.length()][];
        // for (int i = 0; i < chinese.length(); i++) {
        // String[] array = PinyinHelper.toHanyuPinyinStringArray(chinese.charAt(i),
        // this.outputFormat);
        // String[] child;
        // if ((array != null) && (array.length != 0)) {
        // child = new String[array.length];
        // for (int j = 0; j < array.length; j++) {
        // StringBuilder sb = new StringBuilder();
        // String s = array[j];
        // if (isFirstChar) {
        // char c = s.charAt(0);
        // sb.append(c);
        // } else {
        // sb.append(s);
        // }
        // child[j] = sb.toString();
        // }
        // } else {
        // child = new String[]{chinese.charAt(i) + ""};
        // }
        // table[i] = child;
        // }
        return CartesianProductUtils.calc(table);
    }

    private int getChineseCount(String chinese) {
        int count = 0;
        for (char c : chinese.toCharArray()) {
            // 是中文或者a-z或者A-Z转换拼音
            if (String.valueOf(c).matches("[\\u4E00-\\u9FA5]+")) {
                count++;
            }
        }
        return count;
    }

    private WordEntry splitChineseAndEnglish(String chinese) {
        WordEntry currentEntry = null;
        for (char c : chinese.toCharArray()) {
            // 是中文或者a-z或者A-Z转换拼音
            if (String.valueOf(c).matches("[\\u4E00-\\u9FA5]+")) {
                currentEntry = addToEntry(c, true, currentEntry);
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || String.valueOf(c).matches("\\d|\\s")) {
                currentEntry = addToEntry(c, false, currentEntry);
            } else {
                // 不识别的字符暫不處理
                currentEntry = addToEntry(' ', false, currentEntry);
            }
        }
        return currentEntry.getFirst();
    }

    private WordEntry addToEntry(char c, boolean isChinese, WordEntry wordEntry) {
        if (wordEntry == null) {
            wordEntry = new WordEntry();
            wordEntry.setChinese(isChinese);
            wordEntry.getWords().append(c);
        } else {
            if (wordEntry.isChinese() == isChinese) {
                wordEntry.getWords().append(c);
            } else {
                WordEntry nextEntry = new WordEntry();
                nextEntry.setChinese(isChinese);
                nextEntry.getWords().append(c);
                wordEntry.next = nextEntry;
                nextEntry.prev = wordEntry;
                return nextEntry;
            }
        }
        return wordEntry;
    }

    private static class WordEntry {
        private StringBuffer words = new StringBuffer();
        private boolean isChinese;
        private WordEntry next;
        private WordEntry prev;

        public StringBuffer getWords() {
            return words;
        }

        public void setWords(StringBuffer words) {
            this.words = words;
        }

        public boolean isChinese() {
            return isChinese;
        }

        public void setChinese(boolean chinese) {
            isChinese = chinese;
        }

        public WordEntry getNext() {
            return next;
        }

        public void setNext(WordEntry next) {
            this.next = next;
        }

        public WordEntry getPrev() {
            return prev;
        }

        public void setPrev(WordEntry prev) {
            this.prev = prev;
        }

        public WordEntry getFirst() {
            WordEntry first = prev == null ? this : prev;
            while (first.prev != null) {
                first = first.prev;
            }
            return first;
        }
    }

    private static class Variable {

        private char[] curTermBuffer;
        private int curTermLength;
        private boolean outChinese = true;
        private int tokStart;
        private int tokEnd;
        private int curPosInc;
        private int curPosLen;
        private String[] chineseTerms;
        private boolean resetFlag = true;

        public boolean isResetFlag() {
            return resetFlag;
        }

        public void setResetFlag(boolean resetFlag) {
            this.resetFlag = resetFlag;
        }

        private AtomicInteger idx = new AtomicInteger(0);

        public int getCurTermLength() {
            return curTermLength;
        }

        public void setCurTermLength(int curTermLength) {
            this.curTermLength = curTermLength;
        }

        public boolean isOutChinese() {
            return outChinese;
        }

        public void setOutChinese(boolean outChinese) {
            this.outChinese = outChinese;
        }

        public int getTokStart() {
            return tokStart;
        }

        public void setTokStart(int tokStart) {
            this.tokStart = tokStart;
        }

        public int getTokEnd() {
            return tokEnd;
        }

        public void setTokEnd(int tokEnd) {
            this.tokEnd = tokEnd;
        }

        public int getCurPosInc() {
            return curPosInc;
        }

        public void setCurPosInc(int curPosInc) {
            this.curPosInc = curPosInc;
        }

        public int getCurPosLen() {
            return curPosLen;
        }

        public void setCurPosLen(int curPosLen) {
            this.curPosLen = curPosLen;
        }

        public String[] getChineseTerms() {
            return chineseTerms;
        }

        public void setChineseTerms(String[] chineseTerms) {
            this.chineseTerms = chineseTerms;
        }

        public AtomicInteger getIdx() {
            return idx;
        }

        public void setIdx(AtomicInteger idx) {
            this.idx = idx;
        }

        public char[] getCurTermBuffer() {
            return curTermBuffer;
        }

        public void setCurTermBuffer(char[] curTermBuffer) {
            this.curTermBuffer = curTermBuffer;
        }


    }

}
