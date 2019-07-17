package deslopper2;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import org.eclipse.jdt.core.dom.IMethodBinding;

import sloptester.DeslopperInterface;
import sloptester.Func;
import sloptester.Ident;
import sloptester.Tomb;
import sloptester.Type;
import MyUtil.MyNode;
import MyUtil.Pair;
import MyUtil.u;

public class Deslopper2 implements DeslopperInterface {

	public String toString() {
		return "Iterative";
	}

	final int numIterations = 4;

	final int keepBestThisMany = 3;

	final int keepBestThisMany_forObject = 5;

	Tomb tomb;

	public static final double funcCallPenalty = 0.05;
	
    public int getIterations() {
    	return 0;
    }
    
	public MyNode<Pair<Score, Func>> getBestFunc(int maxI, Type returnType,
			Score scoreSoFar, HashSet<Func> usedFuncs) {

		MyNode<Pair<Score, Func>> best = null;
		double bestScore = -Double.MAX_VALUE;
		HashSet<Func> bestUsedFuncs = new HashSet<Func>();

		Collection<Type> types = tomb.keyToType.values();
		for (Type t : types) {
			if (returnType != null) {
				t = returnType;
			}
			for (int i = 0; i <= maxI; i++) {
				for (Score s : t.score[i]) {
					Func func = s.func;
					Score funcScore = scoreSoFar.clone();
					HashSet<Func> myUsedFuncs = (HashSet<Func>) usedFuncs
							.clone();
					if (func != null) {
						funcScore.add(func.score);
						funcScore.sideScore -= funcCallPenalty;
						if (usedFuncs.contains(func)) {
							funcScore.sideScore -= 10;
						}
						funcScore.updateFinalScore();
						myUsedFuncs.add(func);
					} else {
						funcScore.add(s);
					}
					MyNode<Pair<Score, Func>> tree = new MyNode<Pair<Score, Func>>(
							new Pair<Score, Func>(funcScore, func));
					if (func != null) {
						Vector<Pair<Type, Integer>> vp = new Vector<Pair<Type, Integer>>();
						int ii = 0;
						for (Type tt : func.params) {
							vp.add(new Pair<Type, Integer>(tt, ii));
							ii++;
						}
						Collections.sort(vp);
						Object[] a = new Object[vp.size()];
						for (Pair<Type, Integer> p : vp) {
							MyNode<Pair<Score, Func>> child = getBestFunc(
									i - 1, p.left, funcScore, myUsedFuncs);
							funcScore.add(child.data.left);
							a[p.right] = child;
						}
						for (Object o : a) {
							tree.addChild((MyNode<Pair<Score, Func>>) o);
						}
					}
					if (funcScore.finalScore > bestScore) {
						bestScore = funcScore.finalScore;
						best = tree;
						bestUsedFuncs = myUsedFuncs;
					}
				}
			}
			if (returnType != null) {
				break;
			}
		}

		usedFuncs.addAll(bestUsedFuncs);
		return best;
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
	
    public double getScoreFor(MyNode<Func> node) {
    	double score = node.data.score.finalScore - funcCallPenalty;
    	for (MyNode<Func> child : node.children) {
    		score += getScoreFor(child);
    	}
    	return score;
    }

	public String deslop(String slop, Type desiredType, Tomb tomb,
			int localStartIndex, double[] bestScore) throws Exception {
		this.tomb = tomb;
		Collection<Type> types = tomb.keyToType.values();
		Collection<Func> funcs = tomb.funcs;
		Ident slopIdent = new Ident(slop);

		u.profile("score funcs");
		// score each func
		Map<String, Score> stringToScore = new HashMap<String, Score>();
		{
			int j = 0;
			for (Func f : tomb.funcs) {
				String name = f.name;
				Score score = stringToScore.get(name);
				if (score == null) {
					Ident otherIdent = f.ident;
					score = new Score(slopIdent, otherIdent, f);
					stringToScore.put(name, score);
				} else {
					score = score.clone();
					score.func = f;
				}

				if (f.bonus > 0) {
					score.sideScore += f.bonus;
					score.updateFinalScore();
				}

				f.score = score;
				j++;
			}
		}
		u.profile("score funcs");

		// give each type a default score
		for (Type t : types) {
			int keep = keepBestThisMany;
			if (t.isJavaLangObject) {
				keep = keepBestThisMany_forObject;
			}
			t.score = new Score[numIterations][keep];
			Score hole = new Score(-1000000);
			for (int i = 0; i < numIterations; i++) {
				for (int j = 0; j < keep; j++) {
					t.score[i][j] = hole;
				}
			}
			t.nextScore = new Vector<Score>();
		}

		u.profile("parse");
		for (int i = 0; i < numIterations; i++) {
			for (Type t : types) {
				for (Type sub : t.subTypes) {
					for (Score score : sub.nextScore) {
						addIfGoodEnough_dontAddDuplicates(t.score[i], score);
					}
				}
			}
			for (Type t : types) {
				t.nextScore = new Vector<Score>();
				for (int j = 0; j < t.score[i].length; j++) {
					t.nextScore.add(t.score[i][j]);
				}
			}
			if (i < numIterations - 1) {
				for (Func f : funcs) {
					Score score = f.score.clone();
					for (Type p : f.params) {
						Score best = null;
						for (int newI = i; newI >= 0; newI--) {
							for (int j = 0; j < p.score[newI].length; j++) {
								Score newScore = score.clone();
								newScore.add(p.score[newI][j]);
								if (best == null
										|| newScore.finalScore > best.finalScore) {
									best = newScore;
								}
							}
						}
						score = best;
					}

					// disadvantage having lots of function calls
					score.sideScore -= funcCallPenalty;
					score.updateFinalScore();

					f.returnType.nextScore.add(score);
				}
			}
		}
		u.profile("parse");

		MyNode<Pair<Score, Func>> best = getBestFunc(numIterations - 1,
				desiredType, new Score(0), new HashSet<Func>());

		String guess = generateCanonicalName(best);
		bestScore[0] = best.data.left.finalScore;
		return guess;
	}

	public void debugPrintToFiles() throws Exception {
		Collection<Type> types = tomb.keyToType.values();
		Collection<Func> funcs = tomb.funcs;
		{
			PrintWriter out = new PrintWriter(new File("C:/Home/logTypes.txt"));
			for (Type t : types) {
				out.println("T: " + t);
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
			PrintWriter out = new PrintWriter(new File("C:/Home/logFuncs.txt"));
			for (Func f : funcs) {
				out.println(f.score + ", BONUS:" + f.score.sideScore);
			}
			out.close();
		}
	}

	public String generateCanonicalName(MyNode<Pair<Score, Func>> tree) {
		StringBuffer buf = new StringBuffer();
		generateCanonicalName(tree, buf);
		return buf.toString();
	}

	public void generateCanonicalName(MyNode<Pair<Score, Func>> tree,
			StringBuffer buf) {
		Func f = tree.data.right;

		if (f == null) {
			buf.append("__");
			return;
		}

		int i = 0;
		if (f.hasThisParam) {
			generateCanonicalName(tree.children.get(i), buf);
			buf.append(".");
			i++;
		}
		buf.append(f.name);
		if (f.binding instanceof IMethodBinding) {
			buf.append("(");
		}
		for (; i < f.params.size(); i++) {
			generateCanonicalName(tree.children.get(i), buf);
			if ((i + 1) < f.params.size()) {
				buf.append(",");
			}
		}
		if (f.binding instanceof IMethodBinding) {
			buf.append(")");
		}
	}
}
