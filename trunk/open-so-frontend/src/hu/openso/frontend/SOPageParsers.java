package hu.openso.frontend;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;

public class SOPageParsers {
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
//		getJavaQuestions();
		NodeList lst = parseMainPage();
		SimpleNodeIterator nit = lst.elements();
		List<SummaryEntry> list = new ArrayList<SummaryEntry>();
		while (nit.hasMoreNodes()) {
			Node n = nit.nextNode();
			if (n instanceof Tag) {
				Tag t = (Tag)n;
				if ("div".equalsIgnoreCase(t.getTagName())) {
					String id = t.getAttribute("id");
					if (id != null && id.startsWith("question-summary-")) {
						String questionId = id.substring("question-summary-".length());
						SummaryEntry se = new SummaryEntry();
						se.id = questionId;
						processListingsPage(se, t);
						list.add(se);
					}
				}
			}
		}

//		System.out.println(list);
		
		lst = parseQuestionPage();
		QuestionEntry qe = new QuestionEntry();
		processQuestionPage(qe, (Tag)lst.elementAt(0));
		System.out.println(qe);
	}
	static final Charset UTF_8;
	static {
		UTF_8 = Charset.forName("UTF-8");
	}
	static List<SummaryEntry> processMainPage(byte[] data) throws ParserException {
		Parser html = new Parser(new String(data, UTF_8));
		// filter question summaries
		NodeList lst = html.parse(new NodeFilter() {
			private static final long serialVersionUID = -4798449277408336566L;
			@Override
			public boolean accept(Node node) {
				if (node instanceof Tag) {
					Tag t = (Tag)node;
					if ("div".equalsIgnoreCase(t.getTagName())) {
						String id = t.getAttribute("id");
						if (id != null && id.startsWith("question-summary-")) {
							return true;
						}
					}
				}
				return false;
			}
		});
		SimpleNodeIterator nit = lst.elements();
		List<SummaryEntry> list = new ArrayList<SummaryEntry>();
		while (nit.hasMoreNodes()) {
			Node n = nit.nextNode();
			if (n instanceof Tag) {
				Tag t = (Tag)n;
				if ("div".equalsIgnoreCase(t.getTagName())) {
					String id = t.getAttribute("id");
					if (id != null && id.startsWith("question-summary-")) {
						String questionId = id.substring("question-summary-".length());
						SummaryEntry se = new SummaryEntry();
						se.id = questionId;
						processListingsPage(se, t);
						list.add(se);
					}
				}
			}
		}
		return list;
	}
	static NodeList parseMainPage() throws ParserException {
		Parser html = new Parser("so.html");
		// filter question summaries
		NodeList lst = html.parse(new NodeFilter() {
			private static final long serialVersionUID = -4798449277408336566L;
			@Override
			public boolean accept(Node node) {
				if (node instanceof Tag) {
					Tag t = (Tag)node;
					if ("div".equalsIgnoreCase(t.getTagName())) {
						String id = t.getAttribute("id");
						if (id != null && id.startsWith("question-summary-")) {
							return true;
						}
					}
				}
				return false;
			}
		});
		return lst;
	}
	static NodeList parseQuestionPage() throws ParserException {
		Parser html = new Parser("so-question.html");
		// filter question summaries
		NodeList lst = html.parse(new NodeFilter() {
			private static final long serialVersionUID = -4798449277408336566L;
			@Override
			public boolean accept(Node node) {
				if (node instanceof Tag) {
					Tag t = (Tag)node;
					return "body".equalsIgnoreCase(t.getTagName());
				}
				return false;
			}
		});
		return lst;
	}
	static void getQuestions(String site, String tags) throws IOException, HttpException,
			FileNotFoundException {
		HttpClient client = new HttpClient();
		HttpMethod method = new GetMethod(site + "/questions/tagged?tagnames=" + tags + "&page=1&sort=active&pagesize=50");
		client.executeMethod(method);
		FileOutputStream fout = new FileOutputStream("so.html");
		fout.write(method.getResponseBody());
		fout.close();
		method.releaseConnection();
	}
	static byte[] getQuestionsData(String site, String tags, String sort
			, int page) throws IOException, HttpException,
			FileNotFoundException {
		HttpClient client = new HttpClient();
		client.getParams().setParameter(HttpMethodParams.USER_AGENT,
	     "Mozilla/5.0 (Windows; U; Windows NT 6.1; hu; rv:1.9.1.1) Gecko/20090715 Firefox/3.5.1");		
		
		HttpMethod method = null;
		if (tags != null) {
			method = new GetMethod(site + "/questions/tagged?tagnames=" + tags + "&page=" + page + "&sort=" + sort + "&pagesize=50");
		} else {
			method = new GetMethod(site + "/questions?page=" + page + "&sort=" + sort + "&pagesize=50");
		}
//		long t = System.currentTimeMillis();
//		int code = 
			client.executeMethod(method);
//		System.out.printf("%d (%s ms)%n", code, System.currentTimeMillis() - t);
		
		byte[] data = method.getResponseBody();
		method.releaseConnection();
		return data;
	}

	static void getAQuestion(String site, String id) throws IOException, HttpException,
	FileNotFoundException {
		HttpClient client = new HttpClient();
		HttpMethod method = new GetMethod(site + "/questions/" + id);
		client.executeMethod(method);
		FileOutputStream fout = new FileOutputStream("so-question-" + id + ".html");
		fout.write(method.getResponseBody());
		fout.close();
		method.releaseConnection();
	}

	protected static boolean testUser(Tag t) {
		Tag t2 = (Tag)t.getParent();
		if (t2 != null) {
			String a = t2.getAttribute("class");
			if (a != null && a.startsWith("user-details")) {
				a = t.getAttribute("href");
				return a != null && a.startsWith("/users/");
			}
		}
		return false;
	}
	protected static boolean testAvatar(Tag t) {
		if (t.getTagName().equalsIgnoreCase("img")) {
			Tag t2 = (Tag)t.getParent();
			if (t2 != null && t2.getTagName().equalsIgnoreCase("a")) {
				String a = t2.getAttribute("href");
				if (a != null && a.startsWith("/users/")) {
					a = t.getAttribute("src");
					return a != null;
				}
			}
		}
		return false;
	}
	protected static boolean testBadge(Tag t, String badgeType) {
		if (t.getTagName().equalsIgnoreCase("span")) {
			String a = t.getAttribute("title");
			return a != null && a.contains(badgeType);
		}
		return false;
	}
	static String replaceEntities(String s) {
		return s.replaceAll("&ldquo;", "\u201C")
		.replaceAll("&rdquo;", "\u201D")
		.replaceAll("&lsquo;", "\u2018")
		.replaceAll("&rsquo;", "\u2019")
		.replaceAll("&gt;", ">")
		.replaceAll("&amp;", "&")
		.replaceAll("&quot;", "\"")
		.replaceAll("&apos;", "'")
		.replaceAll("&hellip;", "\u2026")
		;
	}
	private static void processListingsPage(final SummaryEntry se, Tag t) {
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		visitor(t, new Associator() {
			@Override
			public void associate(Tag t) {
				String clazzAttr = t.getAttribute("class");
				if (clazzAttr != null) {
					if (clazzAttr.equalsIgnoreCase("vote-count-post")) {
						se.votes = Integer.parseInt(getTextOf(t).trim());
					} else
					if (clazzAttr.startsWith("status")) {
						String s = getTextOf(t);
						se.answers = Integer.parseInt(s.substring(0, s.indexOf("a")).trim());
						se.accepted = clazzAttr.contains("answered-accepted");
					} else
					if (tagWithAttrStarts(t, "div", "class", "views")) {
						String s = getTextOf(t);
						se.views = Integer.parseInt(getNumberFrom(s.substring(0, s.indexOf("view")).trim()));
					} else
					if (clazzAttr.contains("question-hyperlink")) {
						se.title = replaceEntities(getTextOf(t));
					} else
					if (clazzAttr.contains("excerpt")) {
						Node n = t.getFirstChild();
						if (n instanceof Text) {
							se.excerpt = replaceEntities(((Text)n).getText());
						}
					} else
					if (testTimestamp(t)) {
						String a = t.getAttribute("title");
						try {
							se.time = sdf.parse(a.substring(0, 19)).getTime();
						} catch (ParseException e) {
						}
					} else
					if (clazzAttr.contains("reputation-score")) {
						String s = getNumberFrom(t);
						se.userRep = Integer.parseInt(s);
					}
				} else {
					if (testUser(t)) {
						String a = t.getAttribute("href");
						int idx = a.indexOf("/", 8);
						se.userId = a.substring(8, idx);
						Node n = t.getFirstChild();
						if (n instanceof Text) {
							se.userName = ((Text)n).getText();
						}
					} else
					if (testAvatar(t)) {
						se.avatarUrl = replaceEntities(t.getAttribute("src"));
					} else
					if (testBadge(t, "gold badge")) {
						String s = t.getAttribute("title");
						s = s.substring(0, s.indexOf(' '));
						se.goldBadges = Integer.parseInt(getNumberFrom(s));
					} else
					if (testBadge(t, "silver badge")) {
						String s = t.getAttribute("title");
						s = s.substring(0, s.indexOf(' '));
						se.silverBadges = Integer.parseInt(getNumberFrom(s));
					} else
					if (testBadge(t, "bronze badge")) {
						String s = t.getAttribute("title");
						s = s.substring(0, s.indexOf(' '));
						se.bronzeBadges = Integer.parseInt(getNumberFrom(s));
					}
				}
				if (tagWithAttrContains(t, "a", "title", "questions tagged")) {
					se.tags.add(getTextOf(t));
				} else
				if (tagWithAttrContains(t, "div", "class", "bounty-indicator")) {
					String s = getTextOf(t).trim();
					if (s.startsWith("+")) {
						s = s.substring(1);
					}
					se.bounty = Integer.parseInt(s);
				}
			}
		});
	}
	protected static boolean testTimestamp(Tag t) {
		String title = t.getAttribute("title");
		return title != null && title.endsWith("Z UTC");
	}
	protected static String getTextOf(Tag t) {
		StringBuilder b = new StringBuilder();
		getTextOf(b, t);
		return b.toString();
	}
	protected static void getTextOf(StringBuilder b, Tag t) {
		NodeList nl = t.getChildren();
		if (nl != null) {
			SimpleNodeIterator nit = nl.elements();
			while (nit.hasMoreNodes()) {
				Node n = nit.nextNode();
				if (n instanceof Text) {
					b.append(((Text)n).getText());
				} else
				if (n instanceof Tag) {
					getTextOf(b, (Tag)n);
				}
			}
		}
	}
	protected static void visitor(Tag t, Associator assoc) {
		assoc.associate(t);
		NodeList nl = t.getChildren();
		if (nl != null) {
			SimpleNodeIterator nit = nl.elements();
			while (nit.hasMoreNodes()) {
				Node n = nit.nextNode();
				if (n instanceof Tag) {
					visitor((Tag)n, assoc);
				}
			}
		}
	}
	protected static String getNumberFrom(Tag t) {
		String s = getTextOf(t).trim();
		if (s.endsWith("k")) {
			s = s.substring(0, s.length() - 1);
			if (s.contains(".")) {
				s += "00";
			} else {
				s += "000";
			}
			s = s.replace(".", "");
		}
		s = s.replace(",", "");
		return s;
	}
	protected static String getNumberFrom(String t) {
		String s = t.trim();
		if (s.endsWith("k")) {
			s = s.substring(0, s.length() - 1);
			if (s.contains(".")) {
				s += "00";
			} else {
				s += "000";
			}
			s = s.replace(".", "");
		}
		s = s.replace(",", "");
		return s;
	}
	static boolean tagWithAttrStarts(Tag t, String tagname, String attr, String starts) {
		if (tagname == null || t.getTagName().equalsIgnoreCase(tagname)) {
			String r = t.getAttribute(attr);
			return  r != null && r.startsWith(starts);
		}
		return false;
	}
	static boolean tagWithAttrContains(Tag t, String tagname, String attr, String starts) {
		if (tagname == null || t.getTagName().equalsIgnoreCase(tagname)) {
			String r = t.getAttribute(attr);
			return  r != null && r.contains(starts);
		}
		return false;
	}
	static boolean tagWithAttrIs(Tag t, String tagname, String attr, String is) {
		if (tagname == null || t.getTagName().equalsIgnoreCase(tagname)) {
			String r = t.getAttribute(attr);
			return  r != null && r.equals(is);
		}
		return false;
	}
	static boolean tagWithAttrRegex(Tag t, String tagname, String attr, String regex) {
		if (tagname == null || t.getTagName().equalsIgnoreCase(tagname)) {
			String r = t.getAttribute(attr);
			return  r != null && r.matches(regex);
		}
		return false;
	}
	protected static void analyzeUserInfo(Tag t, BasicUserInfo editor) {
		if (testUser(t)) {
			String a = t.getAttribute("href");
			int idx = a.indexOf("/", 8);
			editor.id = a.substring(8, idx);
			Node n = t.getFirstChild();
			if (n instanceof Text) {
				editor.name = ((Text)n).getText();
			}
		} else
			if (tagWithAttrContains(t, null, "class", "reputation-score")) {
				String s = getNumberFrom(t).trim();
				editor.reputation = Integer.parseInt(s);
			}
		if (testAvatar(t)) {
			editor.avatarUrl = t.getAttribute("src");
		} else
		if (testBadge(t, "gold badge")) {
			String s = t.getAttribute("title");
			s = s.substring(0, s.indexOf(' '));
			editor.goldBadges = Integer.parseInt(getNumberFrom(s));
		} else
		if (testBadge(t, "silver badge")) {
			String s = t.getAttribute("title");
			s = s.substring(0, s.indexOf(' '));
			editor.silverBadges = Integer.parseInt(getNumberFrom(s));
		} else
		if (testBadge(t, "bronze badge")) {
			String s = t.getAttribute("title");
			s = s.substring(0, s.indexOf(' '));
			editor.bronzeBadges = Integer.parseInt(getNumberFrom(s));
		}
	}
	static void analyzeEditor(final QuestionEntry qe, Tag t, final SimpleDateFormat sdf) {
		final BasicUserInfo editor = new BasicUserInfo();
		visitor(t, new Associator() {
			@Override
			public void associate(Tag t) {
				analyzeUserInfo(t, editor);
				if (testTimestamp(t)) {
					String a = t.getAttribute("title");
					try {
						qe.edited = sdf.parse(a.substring(0, 19)).getTime();
						if (editor.id != null) {
							qe.editor = editor;
						} else {
							qe.editor = qe.creator;
						}
					} catch (ParseException e) {
					}
				}
			}
		});
	}
	static void analyzeCreator(final QuestionEntry qe, Tag t, final SimpleDateFormat sdf) {
		visitor(t, new Associator() {
			@Override
			public void associate(Tag t) {
				analyzeUserInfo(t, qe.creator);
				if (testTimestamp(t)) {
					String a = t.getAttribute("title");
					try {
						qe.created = sdf.parse(a.substring(0, 19)).getTime();
					} catch (ParseException e) {
					}
				}
			}
		});
	}
	static void analyzeEditor(final AnswerEntry qe, Tag t, final SimpleDateFormat sdf, final int count) {
		final BasicUserInfo editor = new BasicUserInfo();
		visitor(t, new Associator() {
			@Override
			public void associate(Tag t) {
				analyzeUserInfo(t, editor);
				if (testTimestamp(t)) {
					String a = t.getAttribute("title");
					try {
						qe.created = sdf.parse(a.substring(0, 19)).getTime();
					} catch (ParseException e) {
					}
				}
			}
		});
		if (count == 1) {
			qe.editor = new BasicUserInfo();
			qe.editor.assign(qe.creator);
			qe.edited = qe.created;
		}
		qe.creator.assign(editor);
	}
	enum QuestionMode {
		QUESTION,
		ANSWER
	}
	static void processQuestionPage(final QuestionEntry qe, Tag t) {
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		visitor(t, new Associator() {
			QuestionMode mode = QuestionMode.QUESTION;
			@Override
			public void associate(Tag t) {
				if (tagWithAttrContains(t, "div", "id", "answers")) {
					mode = QuestionMode.ANSWER;
				}
				if (mode == QuestionMode.QUESTION) {
					if (tagWithAttrRegex(t, "a", "href", "\\/questions\\/\\d+\\/.*")) {
						String r = t.getAttribute("href");
						qe.id = r.substring(11, r.indexOf('/', 12));
						qe.title = getTextOf(t);
					} else
					if (tagWithAttrContains(t, "span", "class", "vote-count-post")) {
						qe.votes = Integer.parseInt(getNumberFrom(t).trim());
					} else
					if (tagWithAttrContains(t, "div", "class", "favoritecount")) {
						String s = getTextOf(t).trim();
						qe.favorite = s.length() > 0 ? Integer.parseInt(getNumberFrom(s)) : 0;
					} else
					if (tagWithAttrStarts(t, "div", "class", "post-text")) {
						qe.post = t.getChildren().toHtml();
					} else
					if (tagWithAttrIs(t, "td", "class", "post-signature")) {
						analyzeEditor(qe, t, sdf);
					} else
					if (tagWithAttrContains(t, "td", "class", "post-signature owner")) {
						analyzeCreator(qe, t, sdf);
					} else
					if (tagWithAttrContains(t, "a", "title", "questions tagged")) {
						qe.tags.add(getTextOf(t));
					} else
					if (tagWithAttrContains(t, "span", "class", "community-wiki")) {
						qe.wiki = true;
					}
				} else
				if (mode == QuestionMode.ANSWER) {
					if (tagWithAttrStarts(t, "div", "id", "answer-")) {
						processQuestionAnswer(qe, t, sdf);
					}
				}
				if (tagWithAttrContains(t, "p", "class", "label-value")) {
					String s = getTextOf(t);
					if (s.matches("\\d+\\s+time.*")) {
						qe.views = Integer.parseInt(s.substring(0, s.indexOf(' ')));
					}
				}
			}
		});
	}
	protected static void processQuestionAnswer(final QuestionEntry qe, Tag t,
			final SimpleDateFormat sdf) {
		final AnswerEntry ae = new AnswerEntry();
		qe.answers.add(ae);
		String id = t.getAttribute("id");
		ae.id = id.substring(id.indexOf('-') + 1);
		visitor(t, new Associator() {
			int editors = 0;
			@Override
			public void associate(Tag t) {
				if (tagWithAttrContains(t, "span", "class", "vote-count-post")) {
					ae.votes = Integer.parseInt(getNumberFrom(t).trim());
				} else
				if (tagWithAttrContains(t, "div", "class", "post-text")) {
					ae.post = t.getChildren().toHtml();
				} else
				if (tagWithAttrContains(t, "td", "class", "post-signature")) {
					analyzeEditor(ae, t, sdf, editors++);
				} else
				if (tagWithAttrContains(t, "a", "title", "permalink to")) {
					ae.permalink = t.getAttribute("href");
				} else
				if (tagWithAttrContains(t, "span", "class", "community-wiki")) {
					ae.wiki = true;
				}
			}
		});
	}
}
