package deslopper3;

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

public class Deslopper3 implements DeslopperInterface {
	Tomb tomb;

	public Map<Integer, Vector<Pair<Double, Func>>> wordsToFuncs;
	
	public Ident input;
	
	public Type desiredType;

	public int iterationCount = 0;

	public String toString() {
		return "Genetic";
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
	
    public int getIterations() {
    	return iterationCount;
    }

	public MyNode<NodeData> buildTree(int[] bondTypes, double[] order,
			int[] indexes) {

		iterationCount++;

		Vector<MyNode<NodeData>> nodes = new Vector<MyNode<NodeData>>();
		Vector<Pair<Double, Pair<Integer, Integer>>> bonds = new Vector<Pair<Double, Pair<Integer, Integer>>>();
		int start = 0;
		int end = 1;
		for (int i = 0; i < bondTypes.length; i++) {
			int bondType = bondTypes[i];
			double bondOrder = order[i];
			if (bondType == 0) {
			} else {
				nodes.add(new MyNode<NodeData>(new NodeData(start, end,
						bondType == 1)));
				bonds
						.add(new Pair<Double, Pair<Integer, Integer>>(
								bondOrder, new Pair<Integer, Integer>(nodes
										.size() - 1, indexes[i])));
				start = end;
			}
			end++;
		}
		nodes.add(new MyNode<NodeData>(new NodeData(start, end, false)));

		Collections.sort(bonds);
		for (Pair<Double, Pair<Integer, Integer>> bond : bonds) {
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

	public String deslop(String slop, sloptester.Type desiredType, Tomb tomb,
			int localStartIndex, double[] bestScore) throws Exception {
		iterationCount = 0;

		this.tomb = tomb;
		
		this.desiredType = desiredType;

		input = new Ident(slop);
		calcWordsToFuncs(input.words, tomb.funcs);

		int numTokens = input.words.size();
		if (numTokens == 0)
			return "__";

        MyGenome g = (MyGenome)(new MyGenome().makeGenome());
        
        MyGenome best = null;
        for (int t = 0; t < 1; t++) {
            GA ga = new GA(g, 100, 1, 3.0);
            ga.populationSize = 10;
            for (int i = 0; i < 100; i++) {
                ga.iterate();
            }
            MyGenome cur = (MyGenome)ga.best();
            if (best == null || cur.score > best.score) {
                best = cur;
            }
        }

		MyNode<Func> tree = getFuncTree(best.tree, desiredType);
		String output = generateCanonicalName(tree);
		bestScore[0] = best.tree.data.score;
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
        
    public class MyGenome implements Genome {
        double[] genes;
    	public MyNode<NodeData> tree;
    	public double score = 0.0;
        
        public MyGenome() {
            genes = new double[(input.words.size() - 1) * 3];
        }
        
        public Genome makeGenome() {
            MyGenome g = new MyGenome();
            for (int i = 0; i < genes.length; i++) {
                g.genes[i] = u.r.nextDouble();
            }
            return g;
        }
        
        public MyGenome clone() {
            MyGenome g = new MyGenome();
            g.genes = genes.clone();
            return g;
        }
        
        public Genome makeGenome(Genome m, Genome d) {
            MyGenome mom = (MyGenome)m;
            MyGenome dad = (MyGenome)d;
            MyGenome g = new MyGenome();
            
            int crossOverPoint = u.r.nextInt(g.genes.length / 3 + 1) * 3;
            for (int i = 0; i < g.genes.length; i++) {
                if (u.r.nextDouble() < 0.2) {
                    g.genes[i] = u.r.nextDouble();
                } else if (i < crossOverPoint) {
                    g.genes[i] = mom.genes[i];
                } else {
                    g.genes[i] = dad.genes[i];
                }
            }
            
            return g;
        }
        
        public double fitness() {
        	int length = genes.length / 3;
        	int[] bondTypes = new int[length];
        	double[] order = new double[length];
        	int[] indexes = new int[length];
        	
        	for (int i = 0; i < length; i++) {
        		bondTypes[i] = (int)(genes[i * 3 + 0] * 3);
        		order[i] = genes[i * 3 + 1];
        		indexes[i] = (int)(genes[i * 3 + 2] * (genes.length - 1));
        	}
        	
        	tree = buildTree(bondTypes, order, indexes);
        	updateScore(tree, desiredType);
        	score = tree.data.score;
        	return score;
        }
    }
    
    interface Genome {
        public Genome makeGenome();
        public Genome makeGenome(Genome mom, Genome dad);
        public double fitness();
    }
    
    public class GA {
        public Genome genome;
        public Vector<Pair<Double, Genome>> genomes;
        public int populationSize;
        public int keepTopThisMany;
        public double standardDeviation;
        
        public GA(Genome genome, int populationSize, int keepTopThisMany, double standardDeviation) {
            this.genome = genome;
            genomes = new Vector<Pair<Double, Genome>>();
            this.populationSize = populationSize;
            this.keepTopThisMany = keepTopThisMany;
            this.standardDeviation = standardDeviation;
            for (int i = 0; i < populationSize; i++) {
                Genome g = genome.makeGenome();
                genomes.add(new Pair<Double, Genome>(g.fitness(), g));
            }
            Collections.sort(genomes, Collections.reverseOrder());
        }
        
        public Genome pickParent() {
            return genomes.get(
                (int)Math.abs(u.r.nextGaussian() * standardDeviation) % genomes.size()).right;
        }
        
        public Genome best() {
            return genomes.get(0).right;
        }
        
        public void iterate() {
            Vector<Pair<Double, Genome>> newGenomes = new Vector<Pair<Double, Genome>>();
            for (int i = 0; i < keepTopThisMany; i++) {
                newGenomes.add(genomes.get(i));
            }
            for (int i = keepTopThisMany; i < populationSize; i++) {
                Genome g = genome.makeGenome(pickParent(), pickParent());
                newGenomes.add(new Pair<Double, Genome>(g.fitness(), g));
            }
            Collections.sort(newGenomes, Collections.reverseOrder());
            genomes = newGenomes;
        }
    }
	
}
