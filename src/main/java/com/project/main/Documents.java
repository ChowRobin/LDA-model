package com.project.main;

import com.project.utils.FileUtil;
import jxl.read.biff.BiffException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Documents {


    ArrayList<Document> docs;
    Map<String, Integer> wordIndexMap;
    ArrayList<String> wordList;
    Map<String, Integer> wordCountMap;

    private static final Logger logger = LoggerFactory.getLogger(Document.class);

    public Documents() {
        docs = new ArrayList<Document>();
        wordIndexMap = new HashMap<String, Integer>();
        wordList = new ArrayList<String>();
        wordCountMap = new HashMap<String, Integer>();
    }

    public void readDocs(String docsPath) {
        for (File docFile: new File(docsPath).listFiles()) {
            String docName = docFile.getAbsolutePath();
            ArrayList<String> docLines = new ArrayList<String>();
            FileUtil.readLines(docName, docLines);
            Document doc = new Document(docName, docLines, wordIndexMap, wordList, wordCountMap);
            docs.add(doc);
        }
    }

    public void filter(int upper) {
        // list of words should delete
        List<String> deletedList = new ArrayList<String>();
        // add word to deletelist
        for (String word : wordList) {
            if (wordCountMap.get(word) > upper) {
                deletedList.add(word);
            }
        }
        // new map of word index
        Map<String, Integer> newWordIndexMap = new HashMap<String, Integer>();
        List<String> wordListCopy = new ArrayList<String>();
        for (String word : wordList) {
            wordListCopy.add(word);
        }
        for (String word : deletedList) {
            wordCountMap.remove(word);
            wordList.remove(word);
        }
        for (int i = 0; i < wordList.size(); ++i) {
            newWordIndexMap.put(wordList.get(i), i);
        }
        for (Document doc : docs) {
            for (int i = 0; i < doc.docWords.length; ++i) {
                Integer newIndex = newWordIndexMap.get(wordListCopy.get(doc.docWords[i]));
                if (newIndex != null) {
                    doc.docWords[i] = newIndex;
                }
            }
        }
        wordIndexMap = newWordIndexMap;
    }

    public void transXls(String xlsPath, String targetDir, ArrayList<String> cols) throws IOException, BiffException {
        for (File xlsFile : new File(xlsPath).listFiles()) {
            String xlsName = xlsFile.getAbsolutePath();
            ArrayList<String> docLines = new ArrayList<String>();
            FileUtil.readXls(xlsName, cols, docLines);
            ArrayList<String> words = new ArrayList<String>();
            trans(docLines, words, targetDir + xlsFile.getName().split("\\.")[0]);
        }
    }

    public void transDocs(String docsPath, String targetDir) {
        for (File docFile : new File(docsPath).listFiles()) {
            String docName = docFile.getAbsolutePath();
            ArrayList<String> docLines = new ArrayList<String>();
            FileUtil.readLines(docName, docLines);
            ArrayList<String> words = new ArrayList<String>();
            trans(docLines, words, targetDir + docFile.getName().split("\\.")[0]);
        }
    }

    private void trans(ArrayList<String> docLines, ArrayList<String> words, String target) {
        for (String line: docLines) {
            FileUtil.tokenizeByWord(line, words);
        }
        FileUtil.writeLines(target, words);
    }

    public static class Document {
        private String docName;
        int[] docWords;

        public Document(String docName, ArrayList<String> docLines, Map<String, Integer> wordIndexMap, ArrayList<String> wordList, Map<String, Integer> wordCountMap) {
            this.docName = docName;
            //Read file and initialize word index array
            ArrayList<String> words = new ArrayList<String>();
            words = docLines;
//            for(String line : docLines) {
//                FileUtil.tokenize(line, words);
//            }
            //Remove stop words and noise words
//            for (int i = 0; i < words.size(); ++i) {
//                if (StopWords.isStopWord(words.get(i)) || isNoiseWord(words.get(i))) {
//                    words.remove(i);
//                    --i;
//                }
//            }
            //Transfer word to index
            this.docWords = new int[words.size()];
            for (int i = 0; i < words.size(); ++i) {
                String word = words.get(i);
                //judge word in map or not
                if (!wordIndexMap.containsKey(word)) {
                    int newIndex = wordIndexMap.size();
                    wordIndexMap.put(word, newIndex);
                    wordList.add(word);
                    wordCountMap.put(word, 1);
                    docWords[i] = newIndex;
                } else {
                    docWords[i] = wordIndexMap.get(word);
                    wordCountMap.put(word, wordCountMap.get(word) + 1);
                }
            }
//            logger.info("父亲" + wordCountMap.get("父亲"));
            words.clear();
        }

        public boolean isNoiseWord(String string) {
            string = string.toLowerCase().trim();
            Pattern MY_PATTERN = Pattern.compile(".*[a-zA-Z]+.*");
            Matcher m = MY_PATTERN.matcher(string);
            // filter @xxx and URL
            if(string.matches(".*www\\..*") || string.matches(".*\\.com.*") ||
                    string.matches(".*http:.*") )
                return true;
            if (!m.matches()) {
                return true;
            } else
                return false;
        }

    }

    public void print() {
        for (String word : wordIndexMap.keySet()) {
            System.out.println(word);
        }
    }

}
