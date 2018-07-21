package com.project.main;

import com.project.conf.PathConfig;
import com.project.utils.FileUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Model {

    int [][] doc; //word index array
    int M, K, V; //M->document number, K->topic number, V->word number
    int [][] z; // topic label array
    float alpha; //doc-topic dirichlet prior parameter
    float beta; //topic-word dirichlet prior parameter
    int [][] nmk; //given doc m, count times of topic k. M*K
    int [][] nkt; //given topic k, count times of words. K*V
    int [] nmkSum; // sum of each row in nmk, M, words times of each doc
    int [] nktSum; // sum of each row in nkt, K, words times of each topic
    double [][] phi; // Parameters for topic-word distribution K*V
    double [][] theta; // Parameters for doc-topic distribution M*K
    int iterations; // Times of iterations
    int saveStep; // The number of iterations between two saving
    int beginSaveIters; // Begin save model at this iteration

    public Model(Lda.modelparameters modelparameters) {
        alpha = modelparameters.alpha;
        beta = modelparameters.beta;
        iterations = modelparameters.iteration;
        K = modelparameters.topicNum;
        saveStep = modelparameters.saveStep;
        beginSaveIters = modelparameters.beginSaveIters;
    };

    public void initializeModel(Documents docSet) {
        M = docSet.docs.size();
        V = docSet.wordIndexMap.size();
        nmk = new int[M][K];
        nkt = new int[K][V];
        nmkSum = new int[M];
        nktSum = new int[K];
        phi = new double[K][V];
        theta = new double[M][K];

        //initialize doc index array
        doc = new int[M][];
        for (int m = 0; m < M; ++m) {
            int N = docSet.docs.get(m).docWords.length;
            doc[m] = new int[N];
            for (int n = 0; n < N; ++n) {
                doc[m][n] = docSet.docs.get(m).docWords[n];
            }
        }

        //initialize topic lable z for each word
        z = new int[M][];
        for (int m = 0; m < M; ++m) {
            int N = docSet.docs.get(m).docWords.length;
            z[m] = new int[N];
            for (int n = 0; n < N; ++n) {
                int initTopic = (int)(Math.random() * K); // From 0 to K-1
                z[m][n] = initTopic;
                if (initTopic >= K) {
                    System.err.println("init Topic out of bounds!");
                }
                //number of words in doc n assigned to topic initTopic add 1
                nmk[m][initTopic]++;
                //number of terms doc[m][n] assigned to topic initTopic add 1
                nkt[initTopic][doc[m][n]]++;
                // total num of words assigned to topic initTopic add 1
                nktSum[initTopic]++;
            }
            // total num of words in doc m is N
            nmkSum[m] = N;
        }
    }

    public void inferenceModel(Documents docSets) throws IOException {
        if (iterations < saveStep + beginSaveIters) {
            System.err.println("Error: the number of iterations should be larger than " + (saveStep + beginSaveIters));
            System.exit(0);
        }
        for (int i = 0; i < iterations; ++i) {
            System.out.println("Iteration " + i);
            if ((i >= beginSaveIters) && (((i - beginSaveIters) % saveStep) == 0)) {
                //Saving the model
                System.out.println("Saving model at iteration " + i + "... ");
                //First update parameters
                updateEstimatedParameters();
                //secondly print model variables
                saveIteratedModel(i, docSets);
            }

            //use Gibbs Sampling to update z[][]
            for (int m = 0; m < M; ++m) {
                int N = docSets.docs.get(m).docWords.length;
                for (int n = 0; n < N; ++n) {
                    // Sample from p(z_i|z_-i, w)
                    int newTopic = sampleTopicZ(m, n);
                    z[m][n] = newTopic;
                }
            }
        }
    }

    private void updateEstimatedParameters() {
        for (int k = 0; k < K; ++k) {
            for (int t = 0; t < V; ++t) {
                phi[k][t] = (nkt[k][t] + beta) / (nktSum[k] + V * beta);
            }
        }

        for (int m = 0; m < M; ++m) {
            for (int k = 0; k < K; ++k) {
                theta[m][k] = (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha);
            }
        }
    }

    private int sampleTopicZ(int m, int n) {
        // Sample from p(z_i|z_-i, w) using Gibbs upde rule
        // Remove topic label for w_{m, n}
        int oldTopic = z[m][n];
        if (oldTopic >= 10) {
            System.err.println("old Topic out of bounds!");
        }
        nmk[m][oldTopic]--;
        nkt[oldTopic][doc[m][n]]--;
        nmkSum[m]--;
        nktSum[oldTopic]--;

        // Compute p(z_i = k|z_-i, w)
        double[] p = new double[K];
        for (int k = 0; k < K; k++) {
            p[k] = (nkt[k][doc[m][n]] + beta) / (nktSum[k] + V * beta) * (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha);
        }

        // Sample a new topic label for w_{m, n} like roulette
        for (int k = 1; k < K; k++) {
            p[k] += p[k - 1];
        }
        double u = Math.random() * p[K - 1]; // p[] is unnormalised
        int newTopic;
        for (newTopic = 0; newTopic < K; newTopic++) {
            if (u < p[newTopic]) {
                break;
            }
        }
        if (newTopic >= K) {
            System.err.println("new Topic out of bounds!");
        }
        // Add new Topic label for w_{m, n}
        nmk[m][newTopic]++;
        nkt[newTopic][doc[m][n]]++;
        nmkSum[m]++;
        nktSum[newTopic]++;
        return newTopic;
    }

    public void saveIteratedModel(int iters, Documents docSet) throws IOException {
        //lda.params lda.phi lda.theta lda.tassign lda.twords
        String resPath = PathConfig.LdaResultsPath;
        String modelName = "lda_" + iters;
        ArrayList<String> lines = new ArrayList<String>();
        lines.add("alpha = " + alpha);
        lines.add("beta = " + beta);
        lines.add("topicNum = " + K);
        lines.add("docNum = " + M);
        lines.add("termNum = " + V);
        lines.add("iterations = " + iterations);
        lines.add("saveStep = " + saveStep);
        lines.add("beginSaveIters = " + beginSaveIters);
        FileUtil.writeLines(resPath + modelName + ".params", lines);

        //lda.phi K*V
        BufferedWriter writer = new BufferedWriter(new FileWriter(resPath + modelName + ".phi"));
        for (int i = 0; i < K; i++){
            for (int j = 0; j < V; j++){
                writer.write(phi[i][j] + "\t");
            }
            writer.write("\n");
        }
        writer.close();

        //lda.theta M*K
        writer = new BufferedWriter(new FileWriter(resPath + modelName + ".theta"));
        for(int i = 0; i < M; i++){
            for(int j = 0; j < K; j++){
                writer.write(theta[i][j] + "\t");
            }
            writer.write("\n");
        }
        writer.close();

        //lda.tassign
        writer = new BufferedWriter(new FileWriter(resPath + modelName + ".tassign"));
        for(int m = 0; m < M; m++){
            for(int n = 0; n < doc[m].length; n++){
                writer.write(doc[m][n] + ":" + z[m][n] + "\t");
            }
            writer.write("\n");
        }
        writer.close();

        //lda.twords phi[][] K*V
        writer = new BufferedWriter(new FileWriter(resPath + modelName + ".twords"));
        int topNum = 20; //Find the top 20 topic words in each topic
        for(int i = 0; i < K; i++){
            List<Integer> tWordsIndexArray = new ArrayList<Integer>();
            for(int j = 0; j < V; j++){
                tWordsIndexArray.add(new Integer(j));
            }
            Collections.sort(tWordsIndexArray, new Model.TwordsComparable(phi[i]));
            writer.write("topic " + i + "\t:\t");
            for(int t = 0; t < topNum; t++){
                writer.write(docSet.wordList.get(tWordsIndexArray.get(t)) + " id=" + docSet.wordIndexMap.get(docSet.wordList.get(tWordsIndexArray.get(t)))+ " " + phi[i][tWordsIndexArray.get(t)] + "\t");
            }
            writer.write("\n");
        }
        writer.close();
    };

    public class TwordsComparable implements Comparator<Integer> {

        public double[] sortProb; // Store probability of each word in topic k

        public TwordsComparable(double[] sortProb) {
            this.sortProb = sortProb;
        }

        @Override
        public int compare(Integer o1, Integer o2) {
            if (sortProb[o1] > sortProb[o2]) return -1;
            else if (sortProb[o1] < sortProb[o2]) return 1;
            return 0;
        }
    }
}
