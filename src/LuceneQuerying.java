import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList.WeightedPhraseInfo;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.search.vectorhighlight.FieldTermStack;
import org.apache.lucene.search.vectorhighlight.FieldTermStack.TermInfo;
import org.apache.lucene.search.vectorhighlight.FragListBuilder;
import org.apache.lucene.search.vectorhighlight.FragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleFragListBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleFragmentsBuilder;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LuceneQuerying {
	static Path pathIdx = Paths.get("D:/Univ/ID/HW2/LuceneIndex");
	//static Path pathIdx = Paths.get("D:/proj/Eclipse/eclipse-workspace/luceneHW2/target/hw2_bin");
	static String fieldNome = "nome";
	static String fieldContenuto = "contenuto";
	public static void homework2_SearchQP(String queryString) throws Exception {
		// I need to use WhitespaceAnalyzer() on fieldNome,
		// but StandardAnalyzer() on fieldContenuto...
		// so use two QueryParser for the two analyzer,
		// and then "merge" the two parsed querystring into queryStringTrue.

		if (Files.exists(pathIdx))
		{

			Analyzer analyzer = new WhitespaceAnalyzer();
			// fieldContenuto is the field that QueryParser will search if you don't prefix it with a field.
			QueryParser queryParser = new QueryParser(fieldContenuto, analyzer);
			Analyzer analyzerStandard = new StandardAnalyzer();
			QueryParser queryParserStandard = new QueryParser(fieldContenuto, analyzerStandard);

			queryParser.setAllowLeadingWildcard(true); // Allow expensive wildcard like nome:?ro* AND contenuto:hiragana
			queryParserStandard.setAllowLeadingWildcard(true);

			try
			{
				Directory directory = FSDirectory.open(pathIdx);
				DirectoryReader reader = DirectoryReader.open(directory);

				// https://www.lucenetutorial.com/lucene-query-syntax.html
				Query query = queryParser.parse(queryString);
				Query queryStandard = queryParserStandard.parse(queryString);

				String sQuery = query.toString();
				String sQueryStandard = queryStandard.toString();

				if (sQuery != sQueryStandard)
				{
					// create qString that nome: is processed by WhiteSpaceAnalyzer and contenuto: by StandardAnalyzer
					String qString = "";
					int idx = sQueryStandard.indexOf(fieldNome+":");
					while (idx!=-1)
					{
						if (idx>0)
						{
							qString = qString + sQueryStandard.substring(0, idx);
							sQueryStandard = sQueryStandard.substring(idx);
						}
						int idxn = sQuery.indexOf(fieldNome+":");
						if (idxn>0)
							sQuery = sQuery.substring(idxn);
						idxn = sQuery.indexOf(fieldContenuto+":");
						if (idxn == -1)
						{
							qString = qString + sQuery;
							sQuery = "";
							sQueryStandard = "";
							idx = -1; // break
						}
						else
						{
							qString = qString + sQuery.substring(0, idxn);
							idx = sQueryStandard.indexOf(fieldContenuto+":");
							if (idx>0)
								sQueryStandard = sQueryStandard.substring(idx);
							idx = sQueryStandard.indexOf(fieldNome+":");
						}
					}
					qString = qString + sQueryStandard;
					// re-create query with new qString
					query = queryParser.parse(qString);
				}

				try {
					IndexSearcher searcher = new IndexSearcher(reader);
					if (runQuery(searcher, query, false, reader) == 0)
						System.out.println("No results found for query "+queryString);

					Pattern pattern = Pattern.compile("lucene.version=(.*?),");

					Matcher matcher = pattern.matcher(reader.toString());
					if (matcher.find()) {
						System.out.println("Current lucene version: " + matcher.group(1));
					}

				} finally {
					directory.close();
				}

			} catch (Exception e){

				// Deal with e as you please.
				//e may be any type of exception at all.
				System.out.println("The index probably doesn't exist, Exception: "+e.toString());

			}
		}
		else
			System.out.println("Path (Index) "+pathIdx.toString()+" does not exists!");
	}

/*
    // https://stackoverflow.com/a/68246947
    private void handleHit(ScoreDoc hit, Query query, DirectoryReader dirReader,
            IndexSearcher indexSearcher) throws IOException {

        boolean phraseHighlight = Boolean.TRUE;
        boolean fieldMatch = Boolean.TRUE;
        FieldQuery fieldQuery = new FieldQuery(query, dirReader, phraseHighlight, fieldMatch);
        FieldTermStack fieldTermStack = new FieldTermStack(dirReader, hit.doc, fieldContenuto, fieldQuery);
        FieldPhraseList fieldPhraseList = new FieldPhraseList(fieldTermStack, fieldQuery);

        // the following gives you access to positions and offsets:
        fieldPhraseList.getPhraseList().forEach(new Consumer<WeightedPhraseInfo>() {
			public void accept(WeightedPhraseInfo weightedPhraseInfo) {
//			    int phraseStartOffset = weightedPhraseInfo.getStartOffset(); // 19
//			    int phraseEndOffset = weightedPhraseInfo.getEndOffset();     // 34
			    weightedPhraseInfo.getTermsInfos().forEach(new Consumer<TermInfo>() {
					public void accept(TermInfo termInfo) {
					    String term = termInfo.getText();
					    int termPosition = termInfo.getPosition() + 1;
//					    int termStartOffset = termInfo.getStartOffset(); // 19     29
//					    int termEndOffset = termInfo.getEndOffset();     // 22     34
					    System.out.println("{\""+term+"\"@"+termPosition+"}");
					}
				});
			}
		});
    }
*/

	// https://stackoverflow.com/a/51650205
    private static void saveHitWordInList(Query query, IndexSearcher indexSearcher,
    	    int docId, HashSet<String> hitWords) throws IOException
    {
    	  if (query instanceof TermQuery)
    	    if (indexSearcher.explain(query, docId).isMatch())
    	      hitWords.add(((TermQuery) query).getTerm().toString().split(":")[1]);
    	  if (query instanceof BooleanQuery) {
    	    for (BooleanClause clause : (BooleanQuery) query) {
    	      saveHitWordInList(clause.getQuery(), indexSearcher, docId, hitWords);
    	    }
    	  }

    	  if (query instanceof MultiTermQuery) {
    	   // ((MultiTermQuery) query).setRewriteMethod(MultiTermQuery.SCORING_BOOLEAN_REWRITE);
    	    saveHitWordInList(query.rewrite(indexSearcher.getIndexReader()),
    	        indexSearcher, docId, hitWords);
    	  }

    	  if (query instanceof BoostQuery)
    	    saveHitWordInList(((BoostQuery) query).getQuery(), indexSearcher, docId,
    	        hitWords);
    }
/*
    private static int runQuery(IndexSearcher searcher, Query query) throws IOException {
        return runQuery(searcher, query, false, null);
    }
*/
    private static int runQuery(IndexSearcher searcher, Query query, boolean explain, DirectoryReader dirReader) throws IOException {
		int h=0;
		System.out.println("Requested query: "+query.toString());
		TopDocs hits = searcher.search(query, 10);
		if (hits.totalHits.value > 0)
		{
			if (hits.scoreDocs.length<hits.totalHits.value)
				System.out.println(hits.scoreDocs.length + " top hits of "+hits.totalHits.value);
			else
				System.out.println("Total hits returned: "+hits.totalHits.value);
			for (int i = 0; i < hits.scoreDocs.length; i++) {
				ScoreDoc scoreDoc = hits.scoreDocs[i];
				Document doc = searcher.doc(scoreDoc.doc);

				System.out.println("doc"+scoreDoc.doc + ":"+ doc.get(fieldNome) + " (" + scoreDoc.score +")");
				h++;

				if (query.toString().contains(fieldContenuto+":"))
				{
//	            	handleHit(scoreDoc, query, dirReader, searcher);


					HashSet<String> hitWords = new HashSet<String>();

					saveHitWordInList(query, searcher, scoreDoc.doc, hitWords);

					//                System.out.println(doc.get("contenuto"));

					if (!hitWords.isEmpty())
						System.out.println(hitWords);
				}
				if (explain) {
					Explanation explanation = searcher.explain(query, scoreDoc.doc);
					System.out.println(explanation);
				}
			}
		}
		else
			System.out.println("No hits!");
		return h;
    }

    public static void main(String[] args) throws Exception {
		// First argument Index directory
		if (args.length > 0) {
			try {
				pathIdx = Paths.get(args[0]);
			} catch (Exception e) {
				System.err.println("Exception parsing first argument " + args[0]);
				System.exit(1);
			}
		}
		System.out.println("Index directory: "+ pathIdx.toString());

		System.out.println("Enter query:\n");
		String queryString = new Scanner(System.in).nextLine();

		long start = System.currentTimeMillis();
		homework2_SearchQP(queryString);
		long end = System.currentTimeMillis();

		int ms = Math.round(end - start);
		System.out.println("Elapsed time: "+ ms + " ms");

	}
}
