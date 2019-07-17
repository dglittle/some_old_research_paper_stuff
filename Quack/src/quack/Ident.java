package quack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import MyUtil.Bag;

public class Ident {
    public String canonical;

    public List<String> words;

    public Set<String> wordSet;

    public Bag<String> counts;

    public String regex = null;

    public static String keywordRegex = "("
            + "([A-Z]+(?![a-z])|[A-Z][a-z]*|[a-z]+|(?<=\\w)\\d+)"
            + "|"
            + "\"(.*?(\\\\\\\\|\\\\\"))*?[^\\\\]*?\""
            + "|"
            + "'\\\\?.'"
            + "|"
            + "[-+]?(0[xX][0-9a-fA-F]+|(\\d+(\\.(\\d+)?)?|\\.\\d+)(?:e[-+]?\\d+)?)[fFdDlL]?"
             + ")\\s*";

    public void debugPrint() {
        System.out.println("canonical = " + canonical);
        System.out.println("word = " + words);
        System.out.println("wordSet = " + wordSet);
    }

    public Ident() {
    }

    public static Pattern p = null;

    public static Vector<String> keywords(String s) {
        Vector<String> v = new Vector<String>();
        if (p == null) {
            p = Pattern.compile(keywordRegex);
        }
        Matcher m = p.matcher(s);
        while (m.find()) {
            String g = m.group(1);
            if (m.group(2) != null) {
                g = g.toLowerCase();
            }
            v.add(g);
        }
        return v;
    }

    public Ident(String s) {
        this.canonical = s;

        words = keywords(s);
        counts = new Bag<String>();
        wordSet = getWordSet(words);
        for (String word : words) {
            counts.add(word);
        }
    }

    public static Set<String> getWordSet(List<String> words) {
        Set<String> wordSet = new HashSet<String>();
        for (String word : words) {
            while (wordSet.contains(word)) {
                word += "_";
            }
            wordSet.add(word);
        }
        return wordSet;
    }

    public boolean equals(String s) {
        return s.equals(canonical);
    }

    public String toString() {
        return canonical;
    }
}
