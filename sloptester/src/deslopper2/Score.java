package deslopper2;

import java.util.Set;

import sloptester.Func;
import sloptester.Ident;
import MyUtil.u;

public class Score implements Comparable {
    public Double finalScore;
    
    public double sideScore;
    public double[] tokenScores;
    public Func func;
    
    private Score() {
    }
    
    public Score(double score) {
        sideScore = score;
        updateFinalScore();
    }
    
    public Score(Ident slop, Ident other, Func func) {
        int size = slop.words.size();
        tokenScores = new double[slop.words.size()];
        sideScore = 0;
        for (int i = 0; i < slop.words.size(); i++) {
            String word = slop.words.get(i);
            double slopCount = slop.counts.get(word);
            double otherCount = other.counts.get(word);
            tokenScores[i] = otherCount / slopCount;
        }
        
        int intersectionSize = u.intersectionSize(slop.wordSet, other.wordSet);
        int badWords = slop.wordSet.size() + other.wordSet.size() - 
        	(2 * intersectionSize);            
        sideScore -= (badWords * 0.01);
        
        updateFinalScore();
        
        this.func = func;
    }
    
    public Score clone() {
        Score clone = new Score();
        clone.finalScore = finalScore;
        clone.sideScore = sideScore;
        if (tokenScores != null) {
            clone.tokenScores = tokenScores.clone();
        }
        clone.func = func;
        return clone;
    }
    
    public void updateFinalScore() {
        finalScore = sideScore + (tokenScores != null ? u.sum(tokenScores) : 0);
    }
    
    public int compareTo(Object that) {
        return finalScore.compareTo(((Score)that).finalScore);
    }
    
    public void add(Score that) {
        sideScore += that.sideScore;
        if (that.tokenScores != null) {
            if (tokenScores == null) {
                tokenScores = that.tokenScores.clone();
            } else {
                for (int i = 0; i < tokenScores.length; i++) {
                    tokenScores[i] = Math.min(1.0, tokenScores[i] + that.tokenScores[i]);
                }
            }
        }
        updateFinalScore();
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Score: ");
        sb.append(finalScore);
        sb.append("; ");
        sb.append(sideScore);
        sb.append(", [");
        if (tokenScores != null) {
            for (int i = 0; i < tokenScores.length; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(tokenScores[i]);
            }
        }
        sb.append("] : ");
        sb.append(func);
        return sb.toString();
    }
}
