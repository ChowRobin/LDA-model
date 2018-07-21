package com.project.main;

import com.project.conf.ConstantConfig;
import com.project.conf.PathConfig;
import com.project.utils.FileUtil;
import jxl.read.biff.BiffException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Lda {

    private static final Logger logger = LoggerFactory.getLogger(Lda.class);

    public static class modelparameters {
        float alpha = 0.5f; // usual value if 50 / K
        float beta = 0.1f; // usual value is 0.1
        int topicNum = 100;
        int iteration = 100;
        int saveStep = 10;
        int beginSaveIters = 50;
    }

    public enum parameters {
        alpha, beta, topicNum, iteration, saveStep, beginSaveIters;
    }

    private static void getParametersFromFile(modelparameters ldaparameters,
                                              String parameterFile) {
        ArrayList<String> paramLines = new ArrayList<String>();
        FileUtil.readLines(parameterFile, paramLines);
        for (String line : paramLines) {
            String[] lineParts = line.split("\t");
            switch (parameters.valueOf(lineParts[0])) {
                case alpha:
                    ldaparameters.alpha = Float.valueOf(lineParts[1]);
                    break;
                case beta:
                    ldaparameters.beta= Float.valueOf(lineParts[1]);
                    break;
                case topicNum:
                    ldaparameters.topicNum= Integer.valueOf(lineParts[1]);
                    break;
                case iteration:
                    ldaparameters.iteration= Integer.valueOf(lineParts[1]);
                    break;
                case saveStep:
                    ldaparameters.saveStep= Integer.valueOf(lineParts[1]);
                    break;
                case beginSaveIters:
                    ldaparameters.beginSaveIters= Integer.valueOf(lineParts[1]);
                    break;
            }
        }
    }

    public static void main(String[] args) throws IOException, BiffException {
        String originalDocsPath = PathConfig.LdaDocsPath;
        String resultPath = PathConfig.LdaResultsPath;
        String parameterFile = ConstantConfig.LDAPARAMETERFILE;

//        Documents documents = new Documents();
//        ArrayList<String> cols = new ArrayList<String>();
//        cols.add("sovereignty");
//        documents.transXls(originalXlsPath, originalDocsPath, cols);

        modelparameters ldaparameters = new modelparameters();
        getParametersFromFile(ldaparameters, parameterFile);
        Documents docSet = new Documents();
        docSet.readDocs(originalDocsPath);
        docSet.filter(500);
        System.out.println("wordMap size is " + docSet.wordIndexMap.size());
        FileUtil.mkdir(new File(resultPath));
        Model model = new Model(ldaparameters);
        System.out.println("1 Initialize the model ...");
        model.initializeModel(docSet);
        System.out.println("2 Learning and Saving the model ...");
        model.inferenceModel(docSet);
        System.out.println("3 Output the final model ...");
        model.saveIteratedModel(ldaparameters.iteration, docSet);
        System.out.println("Done!");

        int maxCountNum = 0;
        String word = null;
        for (String w : docSet.wordCountMap.keySet()) {
            int count = docSet.wordCountMap.get(w);
            if (count > maxCountNum) {
                maxCountNum = count;
                word = w;
            }
        }
        System.out.println(word + "=>" + maxCountNum);
    }
}
