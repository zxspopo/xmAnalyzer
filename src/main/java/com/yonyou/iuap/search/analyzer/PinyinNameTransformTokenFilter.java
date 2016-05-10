package com.yonyou.iuap.search.analyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yonyou.iuap.search.analyzer.utils.CartesianProductUtils;


public class PinyinNameTransformTokenFilter extends TokenFilter {

    private static final Logger logger = LoggerFactory.getLogger(PinyinNameTransformTokenFilter.class);

    private boolean isOutChinese = true;
    private final CharTermAttribute termAtt = (CharTermAttribute) addAttribute(CharTermAttribute.class);
    HanyuPinyinOutputFormat outputFormat = new HanyuPinyinOutputFormat();
    private boolean _isFirstChar = false;
    private int _minTermLenght = 2;
    private char[] curTermBuffer;
    private int curTermLength;
    private boolean outChinese = true;

    private String[] chineseTerms;

    private AtomicInteger idx = new AtomicInteger(0);

    public PinyinNameTransformTokenFilter(TokenStream input) {
        this(input, false, 2);
    }

    public PinyinNameTransformTokenFilter(TokenStream input, boolean isFirstChar) {
        this(input, isFirstChar, 2);
    }

    public PinyinNameTransformTokenFilter(TokenStream input, boolean isFirstChar, int minTermLenght) {
        this(input, isFirstChar, minTermLenght, true);
    }

    public PinyinNameTransformTokenFilter(TokenStream input, boolean isFirstChar, int minTermLenght,
            boolean isOutChinese) {
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

    public final boolean incrementToken() throws IOException {
        logger.warn(" idx is " + idx.get());
        while (true) {
            if (this.curTermBuffer == null) {
                if (!this.input.incrementToken()) {
                    idx.set(0);
                    return false;
                }
                this.curTermBuffer = ((char[]) this.termAtt.buffer().clone());
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
                        if (chineseTerms == null) {
                            chineseTerms = GetPyString(chinese);
                        }
                        int index = idx.get();
                        if (index < chineseTerms.length) {
                            clearAttributes();
                            this.termAtt.copyBuffer(chineseTerms[index].toCharArray(), 0, chineseTerms[index].length());
                            idx.getAndIncrement();
                            return true;
                        } else {
                            chineseTerms = null;
                        }
                    } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
                        badHanyuPinyinOutputFormatCombination.printStackTrace();
                    }
                    this.curTermBuffer = null;
                    idx.set(0);
                    return true;
                }

            }
            logger.warn(" idx is " + idx.get());
            idx.set(0);
            this.curTermBuffer = null;
        }
    }

    public void reset() throws IOException {
        super.reset();
    }

    private String[] GetPyString(String chinese) throws BadHanyuPinyinOutputFormatCombination {
        String[] jianpinTable = findPinyin(chinese, true);
        String[] quanpinTable = findPinyin(chinese, false);
        String[] partTable = new String[jianpinTable.length + quanpinTable.length];
        System.arraycopy(jianpinTable, 0, partTable, 0, jianpinTable.length);
        System.arraycopy(quanpinTable, 0, partTable, jianpinTable.length, quanpinTable.length);

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
                        child = new String[array.length];
                        for (int j = 0; j < array.length; j++) {
                            StringBuilder sb = new StringBuilder();
                            String s = array[j];
                            if (isFirstChar) {
                                char c = s.charAt(0);
                                sb.append(c);
                            } else {
                                sb.append(s);
                            }
                            child[j] = sb.toString();
                        }
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
            if (wordEntry != null && (wordEntry.isChinese() == isChinese)) {
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

}
