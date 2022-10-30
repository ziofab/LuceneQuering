import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LuceneQuering {
	static Path pathIdx = Paths.get("D:/Univ/ID/HW2/LuceneIndex");
	static String fieldNome = "nome";
	static String fieldContenuto = "contenuto";
	public static void homework2_SearchQP(String queryString) throws Exception {
		// I need to use WhitespaceAnalyzer() on fieldNome,
		// but StandardAnalyzer() on fieldContenuto...
		// so use two QueryParser for the two analyzer,
		// and then "merge" the two parsed querystring into queryStringTrue.
		Analyzer analyzerWA = new WhitespaceAnalyzer();
		Analyzer analyzerSA = new StandardAnalyzer();
		QueryParser queryParserWA = new QueryParser(fieldContenuto, analyzerWA);
		QueryParser queryParserSA = new QueryParser(fieldContenuto, analyzerSA);
        // fieldContenuto is the field that QueryParser will search if you don't
        // prefix it with a field.

		StringBuilder queryStringTrue = new StringBuilder();

		queryParserWA.setAllowLeadingWildcard(true); // Allow expensive wildcard...
		queryParserSA.setAllowLeadingWildcard(true); // Allow expensive wildcard...

        // https://www.lucenetutorial.com/lucene-query-syntax.html
		Query queryWA = queryParserWA.parse(queryString);
		Query querySA = queryParserSA.parse(queryString);
		String sWA = queryWA.toString();
		String sSA = querySA.toString();

		if (sWA.compareTo(sSA)!=0)
		{
			String[] splitWA = sWA.split(" ");
			String[] splitSA = sSA.split(" ");

			assert(splitWA.length == splitSA.length);

			boolean isNome = false;
			for (int i=0; i<splitWA.length; i++) {
				if (splitSA[i].startsWith(fieldNome+":") | splitSA[i].startsWith(fieldNome+":", 1) )
					isNome = true;
				if (splitSA[i].startsWith(fieldContenuto+":") | splitSA[i].startsWith(fieldContenuto+":", 1) )
					isNome = false;
				if (i>0)
					queryStringTrue.append(" ");
				if (isNome)
					queryStringTrue.append(splitWA[i]);
				else
					queryStringTrue.append(splitSA[i]);
			}
		}
		else
			queryStringTrue = new StringBuilder(sWA);
		//System.out.println("queryStringTrue: "+queryStringTrue);

		queryWA = queryParserWA.parse(queryStringTrue.toString());

        try (Directory directory = FSDirectory.open(pathIdx)) {
            // pathIdx = pathIdx.resolve(pathIdx);
            try {
            	try {
            		//System.out.println("Try to open directory: "+directory);
	            	IndexReader reader;
	            	try {
	            		reader = DirectoryReader.open(directory);
		            	try {
			            	IndexSearcher searcher = new IndexSearcher(reader);
							int nResults = runQuery(searcher, queryWA);
							System.out.println();
			                if (nResults == 0)
			                	System.out.print("No");
							else
								System.out.print(nResults);
							System.out.println(" results found for query "+queryString);
			
			                Pattern pattern = Pattern.compile("lucene.version=(.*?),");
			
			                Matcher matcher = pattern.matcher(reader.toString());
			                if (matcher.find()) {
			                    System.out.println("Current lucene version: " + matcher.group(1));
			                }
		                } catch (Exception e){
		                	e.printStackTrace();
		                }
	                } catch (Exception e){
	                	e.printStackTrace();
	                }
                } catch (Exception e){
                	e.printStackTrace();
                }
            } finally {
                directory.close();
            }
        } catch (Exception e){
        	System.out.println("Exception: probably Lucene index at "+pathIdx.toString()+" doesn't exists...");
        }
    }

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

    private static int runQuery(IndexSearcher searcher, Query query) throws IOException {
        return runQuery(searcher, query, false);
    }

    private static int runQuery(IndexSearcher searcher, Query query, boolean explain) throws IOException {
    	int h=0;
    	System.out.println("Requested query: "+query);
    	TopDocs hits = searcher.search(query, 10);
        for (int i = 0; i < hits.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = hits.scoreDocs[i];
            Document doc = searcher.doc(scoreDoc.doc);
            
            System.out.println("doc"+scoreDoc.doc + ":"+ doc.get(fieldNome) + " (" + scoreDoc.score +")");
            h++;

            if (query.toString().contains(fieldContenuto+":"))
            {
            	HashSet<String> hitWords = new HashSet<>();
            	
            	saveHitWordInList(query, searcher, scoreDoc.doc, hitWords);

				if (!hitWords.isEmpty())
					System.out.println(hitWords);
            }
            if (explain) {
                Explanation explanation = searcher.explain(query, scoreDoc.doc);
                System.out.println(explanation);
            }
        }
        return h;
    }

    public static void main(String[] args) throws Exception {
		System.out.println("Enter query:\n");
		String queryString = new Scanner(System.in).nextLine();

		long start = System.currentTimeMillis();
		homework2_SearchQP(queryString);
		long end = System.currentTimeMillis();

		float sec = (end - start) / 1000F;
		System.out.println(sec + " seconds");

	}
}
