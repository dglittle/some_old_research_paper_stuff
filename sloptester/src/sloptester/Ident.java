package sloptester;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import MyUtil.Bag;
import MyUtil.Pair;
import MyUtil.u;

public class Ident {
    public String canonical;
    public List<String> words;
    public Set<String> wordSet;
    public Bag<String> counts;
        
    public void debugPrint() {
        System.out.println("canonical = " + canonical);
        System.out.println("word = " + words);
        System.out.println("wordSet = " + wordSet);
    }
    
    public Ident() {
    }
    
    public Ident(String s) {
        this.canonical = s;
        
        words = u.keywords(s);
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
    
    public double match(Ident that) {
        int intersectionSize = u.intersectionSize(wordSet, that.wordSet);
        if (intersectionSize > 0) {
            int badWords = wordSet.size() + that.wordSet.size() - (2 * intersectionSize);            
            return intersectionSize - (badWords * 0.01);
        }
        return -Double.MAX_VALUE;
    }
    
    public double match(List<String> that) {
    	Set<String> thatWordSet = getWordSet(that);    	
        int intersectionSize = u.intersectionSize(wordSet, thatWordSet);
        if (intersectionSize > 0) {
            int badWords = wordSet.size() + thatWordSet.size() - (2 * intersectionSize);            
            return intersectionSize - (badWords * 0.01);
        }
        return -Double.MAX_VALUE;
    }
}
