# xmAnalyzer
用来对姓名进行拼音分词。支持简拼，全拼，性全拼+其余简拼。 

#配置如下

```
<!-- 拼音 -->
  <fieldType name="text_pinyin_xingming" class="solr.TextField" positionIncrementGap="0"> 
     <analyzer type="index"> 
     	 <tokenizer class="org.apache.lucene.analysis.cn.smart.SmartChineseSentenceTokenizerFactory"/> 
	     <filter class="com.yonyou.search.analyzers.PinyinNameTransformTokenFilterFactory" minTermLenght="1"/>
	     
     </analyzer>
     <analyzer type="query">
         <tokenizer class="solr.WhitespaceTokenizerFactory"/>
         <filter class="solr.LowerCaseFilterFactory"/>
      </analyzer>
  </fieldType>

```
