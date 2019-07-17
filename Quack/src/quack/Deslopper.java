package quack;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Vector;

import MyUtil.MyNode;
import MyUtil.UU;

public class Deslopper {

    final int numIterations = 4;

    final int keepBestThisMany = 5;

    final int keepBestThisMany_forObject = 7;

    final int keepBestThisMany_forVoid = 10;

    Model model;

    public static final double inferencePenalty = 1.0 / 10;

    public static final double levelTwoPenalty = 1.0 / 10000;

    public int getIterations() {
        return 0;
    }

    public static class SearchElement implements Comparable<SearchElement> {
        public Score anticipatedScore;

        public Score runningScore;

        public MyNode<Func> tree;

        public PriorityQueue<ExpansionElement> expands = new PriorityQueue();

        public void debug_print() {
            System.out.println("anticipated score: " + anticipatedScore);
            System.out.println("running score: " + runningScore);
            System.out.println("" + tree);
            for (ExpansionElement e : expands) {
                System.out.println("\t---------");
                e.debug_print();
            }
        }

        public int compareTo(SearchElement that) {
            return -anticipatedScore.compareTo(that.anticipatedScore);
        }

        public SearchElement clone(Map<MyNode<Func>, MyNode<Func>> map) {
            SearchElement c = new SearchElement();
            c.anticipatedScore = anticipatedScore;
            c.runningScore = runningScore.clone();
            c.tree = tree.clone(map);
            for (ExpansionElement e : expands) {
                c.expands.add(e.clone(map));
            }
            return c;
        }

        public void calcAnticipatedScore() {
            anticipatedScore = runningScore.clone();
            for (ExpansionElement e : expands) {
                anticipatedScore.add_dontUpdate(e.anticipatedScore);
            }
            anticipatedScore.updateFinalScore();
        }

        public void expand(PriorityQueue<SearchElement> q) {
            ExpansionElement e = expands.poll();
            for (int i = 0; i <= e.maxI; i++) {
                for (Score score : e.type.score[i]) {
                    Map<MyNode<Func>, MyNode<Func>> map = new HashMap();
                    SearchElement s = clone(map);

                    Func func = score.func;
                    MyNode<Func> node = new MyNode<Func>(func, func.params
                            .size());
                    map.get(e.parent).setChild(e.insertIndex, node);
                    double oldScore = s.runningScore.finalScore;
                    s.runningScore.add(func.score);

                    // detect case where we reuse a "good" function too many
                    // times,
                    // like more times than we have keywords for,
                    // and apply the standard inferencePenalty
                    if (s.runningScore.finalScore - oldScore < 0.7) {
                        if (func.score.finalScore > 0.7) {
                            s.runningScore.sideScore -= inferencePenalty;
                            s.runningScore.updateFinalScore();
                        }
                    }

                    int insert = 0;
                    for (Type t : func.params) {
                        s.expands.add(new ExpansionElement(i - 1, t, node,
                                insert++));
                    }

                    s.calcAnticipatedScore();

                    // NOTE: we only keep possibilities that can explain all
                    // keywords,
                    // and infer at most 3 functions
                    if (s.anticipatedScore.finalScore > (double) score.tokenScores.length
                            - (inferencePenalty * 3)) {
                        q.add(s);
                    }
                }
            }
        }
    }

    public static class ExpansionElement implements
            Comparable<ExpansionElement> {
        public Score anticipatedScore;

        public int maxI;

        public Type type;

        public MyNode<Func> parent;

        public int insertIndex;

        public void debug_print() {
            System.out.println("\tanticipated score: " + anticipatedScore);
            System.out.println("\tmaxI = " + maxI);
            System.out.println("\ttype = " + type);
            System.out.println("\tparent = (" + parent.hashCode() + ") "
                    + parent.data);
            System.out.println("\tinsert index = " + insertIndex);
        }

        public ExpansionElement(int maxI, Type type, MyNode<Func> parent,
                int insertIndex) {
            this.maxI = maxI;
            this.type = type;
            this.parent = parent;
            this.insertIndex = insertIndex;
            calcAnticipatedScore();
        }

        public void calcAnticipatedScore() {
            anticipatedScore = new Score(0, type.score[0][0]);
            for (int i = 0; i <= maxI; i++) {
                for (Score s : type.score[i]) {
                    anticipatedScore.maxWith_dontUpdate(s);
                }
            }
            anticipatedScore.updateFinalScore();
        }

        public int compareTo(ExpansionElement that) {
            return -anticipatedScore.compareTo(that.anticipatedScore);
        }

        public ExpansionElement clone(Map<MyNode<Func>, MyNode<Func>> map) {
            return new ExpansionElement(maxI, type, map.get(parent),
                    insertIndex);
        }
    }

    public void debug_print(PriorityQueue<SearchElement> q, int max) {
        Vector<SearchElement> v = new Vector(q);
        Collections.sort(v);
        int i = 0;
        for (SearchElement s : q) {
            System.out.println("-------------");
            s.debug_print();
            i++;
            if (i >= max)
                break;
        }
    }

    public void debug_print(PriorityQueue<SearchElement> q, int max,
            String rootName) {
        Vector<SearchElement> v = new Vector(q);
        Collections.sort(v);
        int i = 0;
        for (SearchElement s : q) {
            if (!((MyNode<Func>) s.tree.children[0]).data.name.equals(rootName))
                continue;
            System.out.println("-------------");
            s.debug_print();
            i++;
            if (i >= max)
                break;
        }
    }

    public int findIndexOfWorst(Score[] scores, Score score) {
        int worstIndex = 0;
        double worstScore = Double.MAX_VALUE;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] == null) {
                return i;
            }
            double s = scores[i].finalScore;
            if (s < worstScore) {
                worstScore = s;
                worstIndex = i;
            }
        }
        return worstIndex;
    }

    public void addIfGoodEnough_dontAddDuplicates(Score[] scores, Score score) {
        // see if this is a duplicate
        // - if it is, but we have a higher score now, update it
        for (int i = 0; i < scores.length; i++) {
            Score s = scores[i];
            if ((s != null) && (s.func == score.func)) {
                if (score.finalScore > s.finalScore) {
                    scores[i] = score;
                }
                return;
            }
        }

        int indexOfWorst = findIndexOfWorst(scores, score);
        if (scores[indexOfWorst] == null
                || (score.finalScore > scores[indexOfWorst].finalScore)) {
            scores[indexOfWorst] = score;
        }
    }

    public List<String> deslop(Ident quack, Type desiredType, Model model)
            throws Exception {

        this.model = model;
        Ident slopIdent = quack;

        // ----------------------------------------------------
        // score each func
        UU.profile("score funcs");
        int numFuncs = 0;
        Map<String, Score> stringToScore = new HashMap<String, Score>();
        {
            int j = 0;
            for (Vector<Func> funcs : model.funcGroups.values()) {
                numFuncs += funcs.size();
                for (Func f : funcs) {
                    String name = f.name;
                    Score score = stringToScore.get(name);
                    if (score == null) {
                        score = new Score(slopIdent, f);
                        Ident funcIdent = f.ident;
                        boolean inferred = true;
                        if (funcIdent.regex == null) {
                            double unusedWords = funcIdent.words.size();
                            for (int i = 0; i < quack.words.size(); i++) {
                                String word = quack.words.get(i);
                                double slopCount = quack.counts.get(word);
                                double funcCount = funcIdent.counts.get(word);
                                score.tokenScores[i] = Math.min(funcCount
                                        / slopCount, 1.0);
                                unusedWords -= score.tokenScores[i];
                            }
                            score.sideScore -= levelTwoPenalty * unusedWords;
                            if (unusedWords + 0.1 < funcIdent.words.size())
                                inferred = false;
                        } else {
                            int count = 0;
                            for (int i = 0; i < quack.words.size(); i++) {
                                String word = quack.words.get(i);
                                boolean match = word.matches(funcIdent.regex);
                                if (match) {
                                    score.tokenScores[i] = 1.0;
                                    count++;
                                }
                            }
                            for (int i = 0; i < quack.words.size(); i++) {
                                score.tokenScores[i] /= count;
                            }
                            if (count > 0)
                                inferred = false;
                        }
                        if (inferred) {
                            score.sideScore -= inferencePenalty;
                        }
                        stringToScore.put(name, score);
                    }
                    score = score.clone();
                    score.func = f;

                    // give bonus to local variables
                    if (f.contextDist > 0) {
                        score.sideScore += levelTwoPenalty
                                * (100 - f.contextDist);
                    }

                    // give bonus to commonly called functions
                    score.sideScore += levelTwoPenalty
                            * Math
                                    .min(100, model.functionCallCounts
                                            .get(f.key));

                    score.updateFinalScore();

                    f.score = score;
                    j++;
                }
            }
        }
        UU.profile("score funcs");

        // ----------------------------------------------------
        // give each type a default score
        UU.profile("setup table");
        int quackSize = quack.words.size();
        for (Type t : model.types) {
            int keep = keepBestThisMany;
            if (t == model.objectType) {
                keep = keepBestThisMany_forObject;
            } else if (t == model.voidType) {
                keep = keepBestThisMany_forVoid;
            }
            t.score = new Score[numIterations][keep];
            Score hole = new Score(-1000000, quackSize);
            Func holeFunc = new Func(null, "hole", t);
            holeFunc.score = hole;
            hole.func = holeFunc;
            for (int i = 0; i < numIterations; i++) {
                for (int j = 0; j < keep; j++) {
                    t.score[i][j] = hole;
                }
            }
        }
        UU.profile("setup table");

        // ----------------------------------------------------
        UU.profile("parse");
        for (int i = 0; i < numIterations - 1; i++) {
            for (Vector<Func> funcs : model.funcGroups.values()) {
                for (Func f : funcs) {
                    Score score = f.score.clone();
                    for (Type p : f.params) {
                        Score best = null;
                        for (int newI = i; newI >= 0; newI--) {
                            for (Score s : p.score[newI]) {
                                Score newScore = score.clone();
                                newScore.add(s);

                                // detect case where we reuse a "good" function
                                // too many
                                // times,
                                // like more times than we have keywords for,
                                // and apply the standard inferencePenalty
                                if (newScore.finalScore - score.finalScore < 0.7) {
                                    if (s.finalScore > 0.7) {
                                        newScore.sideScore -= inferencePenalty;
                                        newScore.updateFinalScore();
                                    }
                                }

                                if (best == null
                                        || newScore.finalScore > best.finalScore) {
                                    best = newScore;
                                }
                            }
                        }
                        score = best;
                    }
                    addIfGoodEnough_dontAddDuplicates(
                            f.returnType.score[i + 1], score);
                }
            }
            for (Type t : model.types) {
                for (Type sup : t.superTypes) {
                    for (Score score : t.score[i + 1]) {
                        addIfGoodEnough_dontAddDuplicates(sup.score[i + 1],
                                score);
                    }
                }
            }
        }
        UU.profile("parse");

        // ----------------------------------------------------
        UU.profile("extract");
        Vector<String> literals = new Vector<String>(quack.words);
        Vector<String> guesses = new Vector();

        Type returnType = desiredType != null ? desiredType : model.voidType;
        int maxI = numIterations - 1;
        int keepThisMany = 3;

        SearchElement s = new SearchElement();
        s.runningScore = new Score(0, returnType.score[0][0]);
        s.tree = new MyNode<Func>(new Func(null, "root", returnType), 1);
        s.expands.add(new ExpansionElement(maxI, returnType, s.tree, 0));
        s.calcAnticipatedScore();

        PriorityQueue<SearchElement> q = new PriorityQueue();
        q.add(s);

        int biggest = 0;
        int length = 0;
        while (q.size() > 0 && guesses.size() < keepThisMany) {
            biggest = Math.max(biggest, q.size());
            length++;

            // arbitrary limit to how long we run A*
            if (length > 1000)
                break;

            s = q.poll();
            if (s.expands.size() == 0) {
                MyNode<Func> tree = s.tree.children[0];
                String guess = generateCanonicalName(tree, literals);
                if (!guesses.contains(guess)) {
                    guesses.add(guess);
                }
            } else {
                s.expand(q);
            }
        }
        UU.profile("extract");

        Main.getMain().log(
                "stats: w:" + biggest + " l:" + length + " f:" + numFuncs
                        + " t" + model.types.size());

        // work here
        debugPrintToFiles();

        UU.profile("free parse mem");
        for (Type t : model.types) {
            t.score = null;
        }
        for (Vector<Func> funcs : model.funcGroups.values()) {
            for (Func f : funcs) {
                f.score = null;
            }
        }
        UU.profile("free parse mem");

        return guesses;
    }

    public void debugPrintToFiles() throws Exception {
        if (false) {
            UU.profile("debug print");
            {
                PrintWriter out = new PrintWriter(new File(
                        "C:/Working/logTypes.txt"));
                for (Type t : model.types) {
                    out.println("T: " + t);
                    for (Type tt : t.superTypes) {
                        out.println("\t\tsuper: " + tt);
                    }
                    for (int i = 0; i < numIterations; i++) {
                        out.println("\t" + i);
                        for (int j = 0; j < t.score[i].length; j++) {
                            out.println("\t\t: " + t.score[i][j]);
                        }
                    }
                }
                out.close();
            }
            {
                PrintWriter out = new PrintWriter(new File(
                        "C:/Working/logFuncs.txt"));
                for (Vector<Func> funcs : model.funcGroups.values()) {
                    for (Func f : funcs) {
                        out.println(f.score);
                    }
                }
                out.close();
            }
            UU.profile("debug print");
        }
    }

    public String generateCanonicalName(MyNode<Func> tree,
            Vector<String> literals) {
        StringBuffer buf = new StringBuffer();
        generateCanonicalName(tree, buf, (Vector<String>) literals.clone());
        return buf.toString();
    }

    public void generateCanonicalName(MyNode<Func> tree, StringBuffer buf,
            Vector<String> literals) {
        Func f = tree.data;

        if (f == null) {
            buf.append("__");
            return;
        }

        boolean arrayAccessor = f.name.equals("#array accessor");

        int i = 0;
        if (f.hasThisParam) {
            generateCanonicalName(tree.children[i], buf, literals);
            if (!arrayAccessor) {
                buf.append(".");
            }
            i++;
        }

        if (arrayAccessor) {
        } else if (f.ident.regex == null) {
            buf.append(f.name);
        } else {
            int j = 0;
            while (j < literals.size()) {
                String literal = literals.get(j);
                if (literal.matches(f.ident.regex)) {
                    buf.append(literals.remove(j));
                    break;
                }
                j++;
            }
        }

        if (f.method) {
            buf.append("(");
        } else if (arrayAccessor) {
            buf.append("[");
        }
        for (; i < f.params.size(); i++) {
            generateCanonicalName(tree.children[i], buf, literals);
            if ((i + 1) < f.params.size()) {
                buf.append(", ");
            }
        }
        if (f.method) {
            buf.append(")");
        } else if (arrayAccessor) {
            buf.append("]");
        }
    }
}
