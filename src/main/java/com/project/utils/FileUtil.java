package com.project.utils;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.apdplat.word.WordSegmenter;
import org.apdplat.word.segmentation.Word;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class FileUtil {
    /**
     *
     * @param file
     * @param lines
     */
    public static void readLines(String file, ArrayList<String> lines) {
        BufferedReader reader = null;
        try {
            String line = null;
            reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *
     * @param file
     * @param counts
     */
    public static void writeLines(String file, ArrayList<?> counts) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(new File(file)));
            for (int i = 0; i < counts.size(); ++i) {
                writer.write(counts.get(i) + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *
     * @param line
     * @param tokens
     */
    public static void tokenize(String line, ArrayList<String> tokens) {
        StringTokenizer strTok = new StringTokenizer(line);
        while (strTok.hasMoreTokens()) {
            String token = strTok.nextToken();
            tokens.add(clean(token, " ,.\"'()[]{}?<>;:="));
        }
    }

    public static void tokenizeByAnsj(String line, ArrayList<String> tokens) {
        Iterable<Term> terms = ToAnalysis.parse(line);
        for (Term term : terms) {
            String dirtyChars = ",.\"(){}[]?!;'=，。/？！；：（）【】「」、";
            String cleanWord = clean(term.getName(), dirtyChars);
            if (dirtyChars.contains(cleanWord)) continue;
            else {
                tokens.add(cleanWord);
            }
        }
    }

    public static void tokenizeByWord(String line, ArrayList<String> tokens) {
        List<Word> words = WordSegmenter.seg(line);
        for (Word word : words) {
            tokens.add(word.getText());
        }
    }

    private static String clean(String word, String dirtyChars) {
        boolean beginFlag = false, endFlag = false;
        String str = word;
        if (word.length() <= 1) {
            return word;
        }
        for (int i = 0; i < dirtyChars.length(); ++i) {
//            System.out.println(.charAt(i));
            if (word.charAt(0) == dirtyChars.charAt(i)) {
                beginFlag = true;
            }
            if (word.charAt(word.length() - 1) == dirtyChars.charAt(i)) {
                endFlag = true;
            }
        }
        if (beginFlag && endFlag) {
            str = clean(word.substring(1, word.length() - 1), dirtyChars);
        } else if (beginFlag) {
            str = clean(word.substring(1, word.length()), dirtyChars);
        } else if (endFlag) {
            str = clean(word.substring(0, word.length() - 1), dirtyChars);
        }
        return str;
    }

    public static void mkdir(File dirFile) {
        boolean bFile = dirFile.exists();
        if (bFile) {
            System.err.println("The folder exists.");
        } else {
            System.err.println("Now try create the folder.");
            bFile = dirFile.mkdir();
            if (bFile) {
                System.out.println("Create successfully");
            } else {
                System.err.println("Disable to make the folder.");
            }
        }
    }

    public static void mkdir(File file, boolean b) {
        if (b) {
            deleteDirectory(file);
            mkdir(file);
        } else {
            mkdir(file);
        }
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return (path.delete());
    }

    public static void readXls(String xlsName, ArrayList<String> cols, ArrayList<String> lines) throws IOException, BiffException {
        System.out.println(xlsName);
        Workbook workbook = Workbook.getWorkbook(new File(xlsName));
        Sheet sheet = workbook.getSheet(0);
        int rowNum = sheet.getRows();
        int colNum = sheet.getColumns();
        ArrayList<Integer> indexList = new ArrayList<Integer>();
        Cell[] cells = sheet.getRow(0);
        for (int i = 0; i < cells.length; ++i) {
            String title = cells[i].getContents();
            if (cols.contains(title)) {
                indexList.add(i);
            }
        }
        for (int i = 1; i < rowNum; ++i) {
            Cell[] row = sheet.getRow(i);
            for (int index : indexList) {
                lines.add(row[index].getContents());
            }
        }
    }
}
