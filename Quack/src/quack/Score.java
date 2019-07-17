package quack;

import java.util.Arrays;

import MyUtil.UU;

public class Score implements Comparable {
    public Double finalScore;
    
    public double sideScore;
    public double[] tokenScores;
    public Func func;
    
    public Score(double score, Score reference) {
        this(score, reference.tokenScores.length);
    }
    
    public Score(double score, int length) {
        sideScore = score;
        tokenScores = new double[length];
        updateFinalScore();
    }
    
    public Score(Ident slop, Func func) {
        tokenScores = new double[slop.words.size()];
        sideScore = 0;
        this.func = func;
    }
    
    public void paste(Score that) {
        finalScore = that.finalScore;
        sideScore = that.sideScore;
        System.arraycopy(that.tokenScores, 0, tokenScores, 0, tokenScores.length);
        func = that.func;
    }
    
    public void zeroOut() {
        finalScore = 0.0;
        sideScore = 0;
        Arrays.fill(tokenScores, 0);
        func = null;
    }
    
    public Score clone() {
        Score clone = new Score(0, tokenScores.length);
        clone.paste(this);
        return clone;
    }
    
    public void updateFinalScore() {
        finalScore = sideScore + UU.sum(tokenScores);
    }
    
    public int compareTo(Object that) {
        return finalScore.compareTo(((Score)that).finalScore);
    }
    
    public void add(Score that) {
        add_dontUpdate(that);
        updateFinalScore();
    }
    
    public void add_dontUpdate(Score that) {
        sideScore += that.sideScore;
        for (int i = 0; i < tokenScores.length; i++) {
            tokenScores[i] = Math.min(1.0, tokenScores[i] + that.tokenScores[i]);
        }
    }
    
    public void maxWith_dontUpdate(Score that) {
        sideScore = Math.max(sideScore, that.sideScore);
        for (int i = 0; i < tokenScores.length; i++) {
            tokenScores[i] = Math.max(tokenScores[i], that.tokenScores[i]);
        }
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
