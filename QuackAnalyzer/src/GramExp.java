import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import MyUtil.Pair;
import MyUtil.U;

public class GramExp {
	public static void main(String[] args) throws Exception {

		// GramExp g = new GramExp(
		// "assignment = (??type)\\s*=\\s*(??exp)",
		// "exp = (??literal)",
		// "type = (??id)",
		// "literal = (\\d+)",
		// "id = \\w+"
		// );
		GramExp g = new GramExp("S = (??B)(??C)*", "A = a+", "B = b+",
				"C = (??A)(??B)");

		g.print();

		// System.out.println(g.findAll("assignment",
		// U.slurp("src/GramExp.java")));
		System.out.println(g.findAll("S", "bbbbaaaaabaaabb"));

	}

	Map<String, RootNode> ids;

	String topId;

	// work here
	public static int maxPos = 0;

	public GramExp(String... rules) {
		ids = new HashMap<String, RootNode>();
		int i = 0;
		for (String rule : rules) {
			String id;
			String regexp;
			Matcher m = Pattern.compile("(?s)(\\w+)\\s*=\\s*(.*)")
					.matcher(rule);
			if (m.find()) {
				id = m.group(1);
				regexp = m.group(2);
			} else {
				id = "<untitled:" + i + ">";
				regexp = m.group(0);
			}
			if (i == 0) {
				topId = id;
			}
			ids.put(id, new RootNode(id, regexp));
			i++;
		}
		for (RootNode n : ids.values()) {
			n.init(ids);
			n.compact();
		}
	}

	public void print() {
		for (String key : ids.keySet()) {
			RootNode r = ids.get(key);
			r.print(0);
		}
	}

	public Map<String, Object> match(CharSequence seq) {
		return match(topId, seq);
	}

	public Map<String, Object> match(String id, CharSequence seq) {
		return match(id, seq, 0);
	}

	public Map<String, Object> match(CharSequence seq, int pos) {
		return match(topId, seq, pos);
	}

	public Map<String, Object> match(String id, CharSequence seq, int pos) {
		RootNode r = ids.get(id);
		MatchThing mt = new MatchThing(seq);
		int after = r.match(pos, mt);
		if (after != -1) {
			return mt.matches();
		} else {
			return null;
		}
	}

	public Vector<Map<String, Object>> findAll(CharSequence seq) {
		return findAll(topId, seq);
	}

	public Vector<Map<String, Object>> findAll(String id, CharSequence seq) {
		RootNode r = ids.get(id);
		Vector<Map<String, Object>> finds = new Vector<Map<String, Object>>();
		int pos = 0;
		while (pos < seq.length()) {
			MatchThing mt = new MatchThing(seq);
			int after = r.match(pos, mt);
			if (after != -1) {
				finds.add(mt.matches());
				if (after > pos) {
					pos = after;
				} else {
					pos++;
				}
			} else {
				pos++;
			}
		}
		return finds;
	}

	public static class Match {
		public CharSequence seq;

		public int start;

		public int end;

		public Match(CharSequence seq, int start, int end) {
			this.seq = seq.subSequence(start, end);
			this.start = start;
			this.end = end;
		}

		public String toString() {
			return seq.toString();
		}
	}

	public static Document createXml(Object o, String root) throws Exception {
		Document doc = U.createDocument();
		createXml_helper(o, doc, doc.getDocumentElement(), root);
		return doc;
	}

	public static void createXml_helper(Object o, Document doc, Element parent,
			String name) throws Exception {
		if (o instanceof Map) {
			Map<String, Object> m = (Map<String, Object>) o;
			boolean hasChildren = false;
			int maxGroup = 0;
			for (String key : m.keySet()) {
				if (key.matches("\\d+")) {
					int groupNum = Integer.parseInt(key);
					if (groupNum > maxGroup) {
						maxGroup = groupNum;
					}
				} else {
					hasChildren = true;
				}
			}
			if (!hasChildren && (maxGroup <= 1)) {
				createXml_helper(m.get("" + maxGroup), doc, parent, name);
			} else {
				Element e = doc.createElement(name);
				parent.appendChild(e);

				// do this here, rather than in the for loop below,
				// since we want the capture groups in order,
				// and they may not be in order in the hashmap
				for (int i = 1; i <= maxGroup; i++) {
					createXml_helper(m.get("" + i), doc, e, "group");
				}

				for (String key : m.keySet()) {
					if (!key.matches("\\d+")) {
						createXml_helper(m.get(key), doc, e, key);
					}
				}
			}
		} else if (o instanceof Vector) {
			for (Object oo : (Vector) o) {
				createXml_helper(oo, doc, parent, name);
			}
		} else {
			Element e = doc.createElement(name);
			parent.appendChild(e);
			e.appendChild(doc.createTextNode(o.toString()));
		}
	}

	public static class MatchThing {
		CharSequence seq;

		Vector<Pair<String, Object>> matches = new Vector<Pair<String, Object>>();

		public Set<Pair<String, Integer>> currentSearches;

		public MatchThing(MatchThing mt) {
			this(mt.seq, mt.currentSearches);
		}

		public MatchThing(CharSequence seq) {
			this(seq, new HashSet<Pair<String, Integer>>());
		}

		public MatchThing(CharSequence seq,
				Set<Pair<String, Integer>> currentSearches) {
			this.seq = seq;
			this.currentSearches = currentSearches;
		}

		public int mark() {
			return matches.size();
		}

		public void reset(int markValue) {
			matches.setSize(markValue);
		}

		public void add(String i, Object o) {
			matches.add(new Pair<String, Object>(i, o));
		}

		public void add(String i, int start, int end) {
			add(i, new Match(seq, start, end));
		}

		public Map<String, Object> matches() {
			if (matches.size() > 0) {
				Map<String, Object> map = new HashMap<String, Object>();
				for (Pair<String, Object> match : matches) {
					Object value = match.right;
					if (value instanceof MatchThing) {
						value = ((MatchThing) match.right).matches();
					}

					Vector<Object> vector = (Vector<Object>) map
							.get(match.left);
					if (vector != null) {
						vector.add(value);
					} else {
						map.put(match.left, value);
					}
				}
				return map;
			} else {
				return null;
			}
		}
	}

	public static class TokenThing {
		StringBuffer buf;

		CharSequence restSeq;

		Map<String, RootNode> ids;

		String currentRoot;

		int groupBase = 0;

		Vector<String> flagStack = new Vector<String>();

		Map<String, Vector<RecNode>> recNodesAtThisLevel = new HashMap<String, Vector<RecNode>>();

		public TokenThing(String s, Map<String, RootNode> ids,
				String currentRoot) {
			this.buf = new StringBuffer(s);
			this.restSeq = buf.subSequence(0, buf.length());
			this.ids = ids;
			this.currentRoot = currentRoot;
		}

		public boolean nextTokenIs(char c) {
			if (c == (char) 0) {
				return restSeq.length() == 0;
			} else {
				if (restSeq.length() == 0) {
					return false;
				} else {
					return restSeq.charAt(0) == c;
				}
			}
		}

		public Vector<RecNode> getRecNodesAtThisLevel(String name) {
			Vector<RecNode> vector = recNodesAtThisLevel.get(name);
			if (vector == null) {
				vector = new Vector<RecNode>();
				recNodesAtThisLevel.put(name, vector);
			}
			return vector;
		}

		public int getGroupBase() {
			return groupBase;
		}

		public void incGroupBase() {
			groupBase++;
		}

		public Vector<String> getFlags() {
			return flagStack;
		}

		public void addFlags(String flags) {
			flagStack.add(flags);
		}

		public int markFlags() {
			return flagStack.size();
		}

		public void resetFlags(int mark) {
			flagStack.setSize(mark);
		}

		public void consume(char c) {
			if (nextTokenIs(c)) {
				restSeq = restSeq.subSequence(1, restSeq.length());
			} else {
				throw new IllegalArgumentException("bad parse: expected " + c);
			}
		}

		public Matcher consume(String regexp) {
			Matcher m = Pattern.compile(regexp).matcher(restSeq);
			if (m.lookingAt()) {
				restSeq = restSeq.subSequence(m.end(0), restSeq.length());
				return m;
			} else {
				throw new IllegalArgumentException("bad parse: expected "
						+ regexp);
			}
		}

		public RootNode lookupId(String id) {
			RootNode n = ids.get(id);
			if (n == null) {
				if (id.equals("self") || id.equals("this") || id.equals("rec")) {
					return ids.get(currentRoot);
				}
				throw new IllegalArgumentException("unbound id: " + id);
			} else {
				return n;
			}
		}

		public boolean isBound(String id) {
			RootNode n = ids.get(id);
			if (n == null) {
				return id.equals("self") || id.equals("this")
						|| id.equals("rec");
			} else {
				return true;
			}
		}

		CharSequence startCapture() {
			return restSeq;
		}

		String endCapture(CharSequence startSeq) {
			return startSeq
					.subSequence(0, startSeq.length() - restSeq.length())
					.toString();
		}
	}

	public static abstract class Node {
		public String regexp;

		public int groupBase;

		public Vector<String> flagBase = new Vector<String>();

		public Node(TokenThing tt) {
			if (tt != null) {
				CharSequence saveRegexp = tt.startCapture();
				groupBase = tt.getGroupBase();
				flagBase.addAll(tt.getFlags());

				parse(tt);

				regexp = tt.endCapture(saveRegexp);
			}
		}

		public abstract void parse(TokenThing tt);

		// work here
		public int match(int pos, MatchThing mt) {
			int after = match_imp(pos, mt);
			if (after > maxPos)
				maxPos = after;
			return after;
		}

		public abstract int match_imp(int pos, MatchThing mt);

		public abstract Node compact();

		public void print(int indent) {
			U.indent(indent);
			System.out.println("##" + getClass() + ":" + regexp);
			U.indent(indent);
			System.out.println("groupBase = " + groupBase);
			U.indent(indent);
			System.out.println("flagBase = " + flagBase);
		}
	}

	public static class RootNode extends Node {
		public int groupCount;

		public Node child;

		public String id;

		public RootNode(String id, String regexp) {
			super(null);
			this.id = id;
			this.regexp = regexp;
		}

		public void parse(TokenThing tt) {
			child = new OrNode(tt);
		}

		public void init(Map<String, RootNode> ids) {
			TokenThing tt = new TokenThing(regexp, ids, id);
			parse(tt);
			groupCount = tt.getGroupBase() - 1;
		}

		public int match_imp(int pos, MatchThing mt) {
			// if we are already searching for this in this sequence, then fail
			if (!mt.currentSearches.add(new Pair<String, Integer>(id, pos))) {
				return -1;
			}

			int afterMatch = child.match(pos, mt);

			// we're done searching for this...
			if (!mt.currentSearches.remove(new Pair<String, Integer>(id, pos))) {
				throw new IllegalArgumentException("this shouldn't happen");
			}

			if (afterMatch >= 0) {
				mt.add("" + 0, pos, afterMatch);
				return afterMatch;
			} else {
				return -1;
			}
		}

		public Node compact() {
			child = child.compact();
			return this;
		}

		public void print(int indent) {
			super.print(indent);
			child.print(indent + 1);
		}
	}

	public static class RecNode extends Node {
		public RootNode child;

		public Vector<RecNode> recNodesAtThisLevel;

		public String key;

		public RecNode(String regexp, String name, TokenThing tt) {
			super(tt);
			this.regexp = regexp;
			child = tt.lookupId(name);

			recNodesAtThisLevel = tt.getRecNodesAtThisLevel(name);
			recNodesAtThisLevel.add(this);
		}

		public String getKey() {
			if (key == null) {
				key = child.id;
				if (recNodesAtThisLevel.size() > 1) {
					key += "_" + (recNodesAtThisLevel.indexOf(this) + 1);
				}
			}
			return key;
		}

		public void parse(TokenThing tt) {
		}

		public int match_imp(int pos, MatchThing mt) {
			MatchThing childMT = new MatchThing(mt);
			int after = child.match(pos, childMT);
			if (after != -1) {
				mt.add(getKey(), childMT);
			}
			return after;
		}

		public Node compact() {
			return this;
		}

		public void print(int indent) {
			super.print(indent);
			U.indent(indent);
			System.out.println("recurs to: " + child.id);
		}
	}

	public static class OrNode extends Node {
		public Vector<Node> nodes;

		public OrNode(TokenThing tt) {
			super(tt);
		}

		public void parse(TokenThing tt) {
			nodes = new Vector<Node>();
			nodes.add(new ConcatNode(tt));
			while (tt.nextTokenIs('|')) {
				tt.consume('|');
				nodes.add(new ConcatNode(tt));
			}
		}

		public int match_imp(int pos, MatchThing mt) {
			for (Node node : nodes) {
				int after = node.match(pos, mt);
				if (after != -1) {
					return after;
				}
			}
			return -1;
		}

		public Node compact() {
			Vector<Node> newNodes = new Vector<Node>();
			boolean reduceMe = true;
			for (Node n : nodes) {
				Node newNode = n.compact();
				newNodes.add(newNode);
				if (!(newNode instanceof RegexpNode)) {
					reduceMe = false;
				}
			}
			if (reduceMe) {
				return new RegexpNode(regexp, groupBase, flagBase);
			} else {
				nodes = newNodes;
				if (nodes.size() == 1) {
					return nodes.get(0);
				} else {
					return this;
				}
			}
		}

		public void print(int indent) {
			super.print(indent);
			for (Node n : nodes) {
				n.print(indent + 1);
			}
		}
	}

	public static class GroupNode extends Node {
		public Node child;

		int min;

		int max;

		boolean capturing;

		public GroupNode(TokenThing tt) {
			super(tt);
		}

		public void parse(TokenThing tt) {
			int flagMark = tt.markFlags();
			tt.consume('(');
			String recName = null;
			Matcher m = tt
					.consume("(?:\\?(?:\\:|([-a-zA-Z]+)\\:|([-a-zA-Z]+)|(\\?)|=|!|<=|<!|>))?");
			String flags = null;
			if (m.group(1) != null) {
				flags = m.group(1);
			} else if (m.group(2) != null) {
				flags = m.group(2);
			} else if (m.group(3) != null) {
				recName = tt.consume("\\w+").group(0);
			}
			if (flags != null) {
				tt.addFlags(flags);
			}
			String quantifierRegexp = "(?:(?:([\\?\\+\\*])|\\{(\\d+)(,(\\d+)?)?\\})([\\?\\+])?)?";
			if (m.group(0).length() == 0) {
				capturing = true;
				tt.incGroupBase();
			} else {
				capturing = false;
			}
			if (m.group(2) != null && recName == null) {
				tt.consume(')');
				// this is JUST a modifier, so let it's modification outlive
				// it's parenthesis...
				flagMark = tt.markFlags();
				// ... it shouldn't have a quantifier (parse error if it does)
				m = tt.consume(quantifierRegexp);
				if (m.group(0).length() > 0) {
					throw new IllegalArgumentException("unwanted: "
							+ m.group(0));
				}
			} else {
				if (recName != null) {
					child = new RecNode(m.group(0), recName, tt);
				} else {
					child = new OrNode(tt);
				}
				tt.consume(')');
				m = tt.consume(quantifierRegexp);
				if (m.group(1) != null) {
					if (m.group(1).equals("*")) {
						min = 0;
						max = Integer.MAX_VALUE;
					} else if (m.group(1).equals("?")) {
						min = 0;
						max = 1;
					} else if (m.group(1).equals("+")) {
						min = 1;
						max = Integer.MAX_VALUE;
					}
				} else if (m.group(2) != null) {
					min = Integer.parseInt(m.group(2));
					if (m.group(3) != null) {
						if (m.group(4) != null) {
							max = Integer.parseInt(m.group(4));
						} else {
							max = Integer.MAX_VALUE;
						}
					} else {
						max = min;
					}
				} else {
					min = 1;
					max = 1;
				}
			}
			tt.resetFlags(flagMark);
		}

		public int match_imp(int pos, MatchThing mt) {
			int mark = mt.mark();

			if (child instanceof RecNode) {
				if (max > 1) {
					mt.add(((RecNode) child).getKey(), new Vector<Object>());
				}
			}

			String captureKey = null;
			if (capturing) {
				captureKey = "" + (groupBase + 1);
				if (max > 1) {
					mt.add(captureKey, new Vector<Object>());
				}
			}

			int count = 0;
			while (count < max) {
				int after = child.match(pos, mt);
				if (after != -1) {
					if (capturing) {
						mt.add(captureKey, pos, after);
					}
					pos = after;
					count++;
				} else {
					break;
				}
			}
			if (count >= min && count <= max) {
				return pos;
			} else {
				mt.reset(mark);
				return -1;
			}
		}

		public Node compact() {
			if (child != null) {
				child = child.compact();
			}
			if (child == null || child instanceof RegexpNode) {
				return new RegexpNode(regexp, groupBase, flagBase);
			} else {
				return this;
			}
		}

		public void print(int indent) {
			super.print(indent);
			U.indent(indent);
			System.out.println("Group: min = " + min + ", max = " + max);
			if (child != null) {
				child.print(indent + 1);
			}
		}
	}

	public static class ConcatNode extends OrNode {
		public ConcatNode(TokenThing tt) {
			super(tt);
		}

		public void parse(TokenThing tt) {
			nodes = new Vector<Node>();
			while (true) {
				if (tt.nextTokenIs('(')) {
					nodes.add(new GroupNode(tt));
				} else if (tt.nextTokenIs('|') || tt.nextTokenIs(')')
						|| tt.nextTokenIs((char) 0)) {
					break;
				} else {
					nodes.add(new RegexpNode(tt));
				}
			}
		}

		public Node compact() {
			Vector<Node> newNodes = new Vector<Node>();
			int regexpGroupBase = 0;
			Vector<String> regexpFlagBase = null;
			String regexpRun = "";
			for (Node n : nodes) {
				Node newNode = n.compact();
				if (newNode instanceof RegexpNode) {
					if (regexpRun.length() == 0) {
						regexpGroupBase = newNode.groupBase;
						regexpFlagBase = newNode.flagBase;
					}
					regexpRun += newNode.regexp;
				} else {
					if (regexpRun.length() > 0) {
						newNodes.add(new RegexpNode(regexpRun, regexpGroupBase,
								regexpFlagBase));
						regexpRun = "";
					}
					newNodes.add(newNode);
				}
			}
			if (regexpRun.length() > 0) {
				newNodes.add(new RegexpNode(regexpRun, regexpGroupBase,
						regexpFlagBase));
			}
			nodes = newNodes;

			if (nodes.size() == 1) {
				return nodes.get(0);
			} else {
				return this;
			}
		}

		public int match(int pos, MatchThing mt) {
			int mark = mt.mark();
			for (Node node : nodes) {
				pos = node.match(pos, mt);
				if (pos == -1) {
					mt.reset(mark);
					return -1;
				}
			}
			return pos;
		}
	}

	public static class RegexpNode extends Node {
		Matcher m;

		String flagPrefix;

		public RegexpNode(TokenThing tt) {
			super(tt);
		}

		public void parse(TokenThing tt) {
			m = tt
					.consume("(?s)([^\\|\\(\\)\\\\]|\\\\\\\\|\\\\0\\d{1,3}|\\\\x\\p{XDigit}{2}|\\\\u\\p{XDigit}{4}|\\\\c.|\\\\.)*");
			m = Pattern.compile(m.group(0)).matcher("");
		}

		public RegexpNode(String regexp, int groupBase, Vector<String> flagBase) {
			super(null);
			this.groupBase = groupBase;
			this.flagBase = flagBase;
			this.regexp = regexp;
			this.flagPrefix = flagBase.size() > 0 ? "(?"
					+ U.concat(flagBase, ")(?") + ")" : "";
			m = Pattern.compile(flagPrefix + regexp).matcher("");
		}

		public int match_imp(int pos, MatchThing mt) {
			m.reset(mt.seq.subSequence(pos, mt.seq.length()));
			if (m.lookingAt()) {
				for (int i = 1; i <= m.groupCount(); i++) {
					if (m.group(i) != null) {
						mt.add("" + (groupBase + i), new Match(mt.seq, pos
								+ m.start(i), pos + m.end(i)));
					}
				}
				return pos + m.end(0);
			}
			return -1;
		}

		public Node compact() {
			return this;
		}

		public void print(int indent) {
			super.print(indent);
			U.indent(indent);
			System.out.println("flag prefix: " + flagPrefix);
		}
	}
}
