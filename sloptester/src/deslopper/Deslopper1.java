package deslopper;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

class NodeData {
	int start;

	int end;

	Integer wordsKey;

	double score;

	boolean rightUp;

	Map<Type, Pair<Double, Func>> typeToScore;

	public NodeData(int start, int end, boolean rightUp) {
		this.start = start;
		this.end = end;
		this.wordsKey = (start << 16) + end;
		this.score = 0;
		this.rightUp = rightUp;
		this.typeToScore = null;
	}

	public String toString() {
		return "[" + start + "," + end + "]" + (rightUp ? "/" : "\\") + score
				+ ":" + typeToScore;
	}
}

public class Deslopper1 implements DeslopperInterface {
	Tomb tomb;

	public Map<Integer, Vector<Pair<Double, Func>>> wordsToFuncs;

	public String toString() {
		return "Token Tree";
	}

	public Vector<Pair<Double, Func>> getGoodFuncsForRangeInOrder(
			List<String> tokens, int s, int e, Vector<Func> funcs) {

		Vector<Pair<Double, Func>> v = new Vector<Pair<Double, Func>>();
		for (Func f : funcs) {
			double score = f.ident.match(tokens.subList(s, e));
			if (score > 0) {
				score += f.bonus;
				score += (e - s) - 1;
				v.add(new Pair<Double, Func>(score, f));
			}
		}
		Collections.sort(v, Collections.reverseOrder());
		return v;
	}

	public void calcWordsToFuncs(List<String> tokens, Vector<Func> funcs) {
		wordsToFuncs = new HashMap<Integer, Vector<Pair<Double, Func>>>();
		for (int s = 0; s < tokens.size(); s++) {
			for (int e = s + 1; e <= tokens.size(); e++) {
				wordsToFuncs.put((s << 16) + e, getGoodFuncsForRangeInOrder(
						tokens, s, e, funcs));
			}
		}
	}

	public void updateScore(MyNode<NodeData> n, Type desiredType) {
		if (n.data.typeToScore == null) {
			n.data.score = -Double.MAX_VALUE;
			n.data.typeToScore = new HashMap<Type, Pair<Double, Func>>();

			for (MyNode<NodeData> child : n.children) {
				updateScore(child, null);
			}

			for (Pair<Double, Func> pair : wordsToFuncs.get(n.data.wordsKey)) {
				double score = pair.left;
				Func func = pair.right;
				Type type = func.returnType;
				List<Type> params = func.params;

				if (desiredType != null) {
					if (!desiredType.subTypes.contains(type)) {
						continue;
					}
				}

				boolean valid = true;
				if (params.size() != n.children.size()) {
					valid = false;
				}
				for (int i = 0; i < n.children.size(); i++) {
					boolean good = false;
					if (i < params.size()) {
						Pair<Double, Func> best = null;
						for (Type t : params.get(i).subTypes) {
							Pair<Double, Func> p = n.children.get(i).data.typeToScore
									.get(t);
							if (p != null) {
								if (best == null || p.left > best.left) {
									best = p;
								}
							}
						}
						if (best != null) {
							score += best.left + 1;
							good = true;
						}
					}
					if (!good) {
						score += n.children.get(i).data.score;
						valid = false;
					}
				}
				if (valid) {
					Pair<Double, Func> p = n.data.typeToScore
							.get(func.returnType);
					if (p == null || score > p.left) {
						p = new Pair<Double, Func>(score, func);
						n.data.typeToScore.put(func.returnType, p);
					}
				}

				if (score > n.data.score) {
					n.data.score = score;
				}
			}
		}
	}

	public MyNode<NodeData> buildTree(int[] bondTypes, int[] order,
			int[] indexes) {

		iterationCount++;

		Vector<MyNode<NodeData>> nodes = new Vector<MyNode<NodeData>>();
		Vector<Pair<Integer, Pair<Integer, Integer>>> bonds = new Vector<Pair<Integer, Pair<Integer, Integer>>>();
		int start = 0;
		int end = 1;
		for (int i = 0; i < bondTypes.length; i++) {
			int bondType = bondTypes[i];
			int bondOrder = order[i];
			if (bondType == 0) {
			} else {
				nodes.add(new MyNode<NodeData>(new NodeData(start, end,
						bondType == 1)));
				bonds
						.add(new Pair<Integer, Pair<Integer, Integer>>(
								bondOrder, new Pair<Integer, Integer>(nodes
										.size() - 1, indexes[i])));
				start = end;
			}
			end++;
		}
		nodes.add(new MyNode<NodeData>(new NodeData(start, end, false)));

		Collections.sort(bonds);
		for (Pair<Integer, Pair<Integer, Integer>> bond : bonds) {
			MyNode<NodeData> a = nodes.get(bond.right.left);
			MyNode<NodeData> b = nodes.get(bond.right.left + 1);
			boolean rightUp = a.data.rightUp;
			a = a.getRoot();
			b = b.getRoot();

			int index = bond.right.right;
			if (rightUp) {
				b.addChildAt(a, index % (b.children.size() + 1));
			} else {
				a.addChildAt(b, index % (a.children.size() + 1));
			}
		}

		return nodes.get(0).getRoot();
	}

	public MyNode<Func> getFuncTree(MyNode<NodeData> n, Type desiredT) {
		Pair<Double, Func> best = null;
		if (desiredT != null) {
			for (Type t : desiredT.subTypes) {
				Pair<Double, Func> p = n.data.typeToScore.get(t);
				if (p != null) {
					if (best == null || p.compareTo(best) > 0) {
						best = p;
					}
				}
			}
		} else {
			for (Type t : n.data.typeToScore.keySet()) {
				Pair<Double, Func> p = n.data.typeToScore.get(t);
				if (p != null) {
					if (best == null || p.compareTo(best) > 0) {
						best = p;
					}
				}
			}
		}
		if (best != null) {
			Func f = best.right;
			MyNode<Func> node = new MyNode<Func>(f);
			for (int i = 0; i < f.params.size(); i++) {
				node.addChild(getFuncTree(n.children.get(i), f.params.get(i)));
			}
			return node;
		}
		return null;
	}

	public String getKey(MyNode<NodeData> node) {
		if (node == null)
			return "";
		StringBuffer buf = new StringBuffer();
		getKey(node, buf);
		return buf.toString();
	}

	public void getKey(MyNode<NodeData> node, StringBuffer buf) {
		buf.append("(");
		buf.append(node.data.start);
		buf.append(",");
		buf.append(node.data.end);
		for (MyNode<NodeData> child : node.children) {
			getKey(child, buf);
		}
		buf.append(")");
	}

	public MyNode<NodeData> pickBestNeighborOrSelf(String key, int[] bondTypes,
			int[] order, int[] indexes, Type desiredType) {
		MyNode<NodeData> a = buildTree(bondTypes, order, indexes);
		updateScore(a, desiredType);
		double bestScore = a.data.score;
		MyNode<NodeData> bestTree = a;
		int[] bestBondTypes = bondTypes.clone();
		int[] bestOrder1 = order.clone();
		int[] bestOrder2 = indexes.clone();
		for (int i = 0; i < bondTypes.length; i++) {
			for (int j = 0; j < 3; j++) {
				int save = bondTypes[i];
				if (save != j) {
					bondTypes[i] = j;
					{
						a = buildTree(bondTypes, order, indexes);
						if (!key.equals(getKey(a))) {
							updateScore(a, desiredType);
							if (a.data.score > bestScore) {
								bestScore = a.data.score;
								bestTree = a;
								bestBondTypes = bondTypes.clone();
								bestOrder1 = order.clone();
								bestOrder2 = indexes.clone();
							}
						}
					}
				}
				bondTypes[i] = save;
			}
		}
		for (int i = 0; i < order.length - 1; i++) {
			for (int j = i + 1; j < order.length; j++) {
				int temp = order[i];
				order[i] = order[j];
				order[j] = temp;
				{
					a = buildTree(bondTypes, order, indexes);
					if (!key.equals(getKey(a))) {
						updateScore(a, desiredType);
						if (a.data.score > bestScore) {
							bestScore = a.data.score;
							bestTree = a;
							bestBondTypes = bondTypes.clone();
							bestOrder1 = order.clone();
							bestOrder2 = indexes.clone();
						}
					}
				}
				temp = order[i];
				order[i] = order[j];
				order[j] = temp;
			}
		}
		for (int i = 0; i < indexes.length; i++) {
			for (int j = 0; j < indexes.length; j++) {
				int save = indexes[i];
				if (save != j) {
					indexes[i] = j;
					a = buildTree(bondTypes, order, indexes);
					if (!key.equals(getKey(a))) {
						updateScore(a, desiredType);
						if (a.data.score > bestScore) {
							bestScore = a.data.score;
							bestTree = a;
							bestBondTypes = bondTypes.clone();
							bestOrder1 = order.clone();
							bestOrder2 = indexes.clone();
						}
					}
				}
				indexes[i] = save;
			}
		}
		System.arraycopy(bestBondTypes, 0, bondTypes, 0, bondTypes.length);
		System.arraycopy(bestOrder1, 0, order, 0, order.length);
		System.arraycopy(bestOrder2, 0, indexes, 0, indexes.length);
		return bestTree;
	}

	public double getScoreFor(Func func) {
		return func.ident.words.size() + func.bonus
				+ (func.ident.words.size() - 1);
	}

	public double getScoreFor(MyNode<Func> node) {
		double score = getScoreFor(node.data);
		for (MyNode<Func> child : node.children) {
			score += 1 + getScoreFor(child);
		}
		return score;
	}

	public int iterationCount = 0;
    public int getIterations() {
    	return iterationCount;
    }

	public String deslop(String slop, sloptester.Type desiredType, Tomb tomb,
			int localStartIndex, double[] bestScore) throws Exception {
		iterationCount = 0;

		this.tomb = tomb;

		Ident slopIdent = new Ident(slop);
		calcWordsToFuncs(slopIdent.words, tomb.funcs);

		int numTokens = slopIdent.words.size();
		if (numTokens == 0)
			return "__";

		MyNode<NodeData> realBestTree = null;
		for (int t = 0; t < 10; t++) {
			int[] bondTypes = new int[numTokens - 1];
			int[] order = new int[numTokens - 1];
			int[] index = new int[numTokens - 1];

			Vector<Integer> order_ = new Vector<Integer>();
			for (int i = 0; i < numTokens - 1; i++) {
				order_.add(i);
			}
			Collections.shuffle(order_);

			for (int i = 0; i < numTokens - 1; i++) {
				bondTypes[i] = u.r.nextInt(3);
				order[i] = order_.get(i);
				index[i] = u.r.nextInt(numTokens - 1);
			}

			MyNode<NodeData> bestTree = null;
			// for (int tt = 0; tt < 10; tt++) {
			while (true) {
				MyNode<NodeData> a = pickBestNeighborOrSelf(getKey(bestTree),
						bondTypes, order, index, desiredType);
				if (bestTree == null || a.data.score > bestTree.data.score) {
					bestTree = a;
				} else {
					break;
				}
			}

			if (realBestTree == null
					|| bestTree.data.score > realBestTree.data.score) {
				realBestTree = bestTree;
			}
		}

		MyNode<Func> tree = getFuncTree(realBestTree, desiredType);
		String output = generateCanonicalName(tree);
		bestScore[0] = realBestTree.data.score;
		return output;
	}

	public String generateCanonicalName(MyNode<Func> tree) {
		StringBuffer buf = new StringBuffer();
		generateCanonicalName(tree, buf);
		return buf.toString();
	}

	public void generateCanonicalName(MyNode<Func> tree, StringBuffer buf) {
		if (tree == null) {
			buf.append("__");
			return;
		}

		Func f = tree.data;

		if (f == null) {
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
