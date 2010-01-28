package hu.openso.frontend;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
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
import org.apache.commons.httpclient.methods.PostMethod;
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
	static final Charset UTF_8;
	static {
		UTF_8 = Charset.forName("UTF-8");
	}
	/**
	 * Processes the main page data and returns a list of summary entry objects.
	 * @param data the raw data to parse and process
	 * @return the list of summary entry objects, not null
	 * @throws ParserException if a HTML parsing problem occurs
	 */
	public static List<SummaryEntry> processMainPage(byte[] data) throws ParserException {
		List<SummaryEntry> list = new ArrayList<SummaryEntry>();
		String htmlStr = new String(data, UTF_8);
		// check for offline indicator
		if (htmlStr.contains("<title>Offline - ")) {
			return list; 
		}
		Parser html = new Parser(htmlStr);
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
	static byte[] getQuestionsData(String site, String tags, String sort
			, int page, int ps) throws IOException, HttpException,
			FileNotFoundException {
		HttpClient client = new HttpClient();
		client.getParams().setParameter(HttpMethodParams.USER_AGENT,
	     "Mozilla/5.0 (Windows; U; Windows NT 6.1; hu; rv:1.9.1.1) Gecko/20090715 Firefox/3.5.1");		
		
		// TODO if it is superuser, lie us in
		if (site.contains("superuser.com")) {
			PostMethod post = new PostMethod(site + "/beta-access");
			post.setParameter("password", "ewok.adventure");
			client.executeMethod(post);
		}		
		HttpMethod method = null;
		if (tags != null) {
			method = new GetMethod(site + "/questions/tagged?tagnames=" + URLEncoder.encode(tags, "UTF-8") + "&page=" + page + "&sort=" + sort + "&pagesize=" + ps);
		} else {
			method = new GetMethod(site + "/questions?page=" + page + "&sort=" + sort + "&pagesize=" + ps);
		}
		client.executeMethod(method);
		
		byte[] data = method.getResponseBody();
		method.releaseConnection();
		return data;
	}
	public static byte[] getUnansweredData(String site, String tags, String sort
			, int page, int ps) throws IOException, HttpException,
			FileNotFoundException {
		HttpClient client = new HttpClient();
		client.getParams().setParameter(HttpMethodParams.USER_AGENT,
	     "Mozilla/5.0 (Windows; U; Windows NT 6.1; hu; rv:1.9.1.1) Gecko/20090715 Firefox/3.5.1");		
		
		// TODO if it is superuser, lie us in
		if (site.contains("superuser.com")) {
			PostMethod post = new PostMethod(site + "/beta-access");
			post.setParameter("password", "ewok.adventure");
			client.executeMethod(post);
		}		
		HttpMethod method = null;
		if (tags != null) {
			method = new GetMethod(site + "/unanswered/tagged?tagnames=" + URLEncoder.encode(tags, "UTF-8") + "&page=" + page + "&tab=" + sort + "&pagesize=" + ps);
		} else {
			method = new GetMethod(site + "/unanswered?page=" + page + "&tab=" + sort + "&pagesize=" + ps);
		}
		client.executeMethod(method);
		
		byte[] data = method.getResponseBody();
		method.releaseConnection();
		return data;
	}

	public static byte[] getAQuestionData(String site, String id) throws IOException, HttpException,
	FileNotFoundException {
		HttpClient client = new HttpClient();
		client.getParams().setParameter(HttpMethodParams.USER_AGENT,
	     "Mozilla/5.0 (Windows; U; Windows NT 6.1; hu; rv:1.9.1.1) Gecko/20090715 Firefox/3.5.1");		
		// TODO if it is superuser, lie us in
		if (site.contains("superuser.com")) {
			PostMethod post = new PostMethod(site + "/beta-access");
			post.setParameter("password", "ewok.adventure");
			client.executeMethod(post);
		}		
		HttpMethod method = new GetMethod(site + "/questions/" + id);
		client.executeMethod(method);
		byte[] data = method.getResponseBody();
		method.releaseConnection();
		return data;
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
						se.userId = a.substring(7, idx);
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
		return title != null && (title.endsWith("Z UTC") || title.endsWith("Z"));
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
	static boolean tagWithAttrEnds(Tag t, String tagname, String attr, String starts) {
		if (tagname == null || t.getTagName().equalsIgnoreCase(tagname)) {
			String r = t.getAttribute(attr);
			return  r != null && r.endsWith(starts);
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
	/**
	 * Processes the raw question page data and returns a QuestionEntry object for it.
	 * If there was some trouble in the parsing (e.g. wrong page), the
	 * returned QuestionEntry objects id field is null.
	 * @param data the data to process
	 * @return the question entry object.
	 * @throws ParserException
	 */
	public static QuestionEntry processQuestionPage(byte[] data) throws ParserException {
		final QuestionEntry qe = new QuestionEntry();
		String htmlStr = new String(data, UTF_8);
		// check for offline indicator
		if (htmlStr.contains("<title>Offline - ")) {
			return qe; 
		}
		Parser html = new Parser(htmlStr);
		// filter question summaries
		NodeList lst = html.parse(getTagAcceptor("body"));
		if (lst.size() >= 1) {
			final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			visitor((Tag)lst.elementAt(0), new Associator() {
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
		return qe;
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
	static String replaceEntities(String s) {
		return s.replaceAll("&ldquo;", "\u201C")
		.replaceAll("&rdquo;", "\u201D")
		.replaceAll("&lsquo;", "\u2018")
		.replaceAll("&rsquo;", "\u2019")
		.replaceAll("&lt;", "<")
		.replaceAll("&gt;", ">")
		.replaceAll("&amp;", "&")
		.replaceAll("&quot;", "\"")
		.replaceAll("&apos;", "'")
		.replaceAll("&hellip;", "\u2026")
		.replaceAll("&mdash;", "\u2014")
		;
	}
	/**
	 * Determine the current on-line version of the frontend
	 * @return the online version string or empty if there was an error
	 */
	public static String getOnlineVersion() {
		String ver = "";
		try {
			Parser html = new Parser("http://code.google.com/p/open-so-frontend/");
			NodeList nl = html.parse(new NodeFilter() {
				private static final long serialVersionUID = 4628298108041398258L;

				@Override
				public boolean accept(Node node) {
					if (node instanceof Tag) {
						Tag t = (Tag)node;
						if (t.getTagName().equalsIgnoreCase("h1")) {
							if (getTextOf(t).contains("Current version:")) {
								return true;
							}
						}
					}
					return false;
				}
			});
			if (nl.size() >= 1) {
				String s = getTextOf((Tag)nl.elementAt(0));
				ver = s.substring(s.lastIndexOf(' ') + 1).trim();
			}
		} catch (ParserException ex) {
			ex.printStackTrace();
		}
		return ver;
	}
	public static List<ReputationEntry> parseHistoryTable(byte[] data) throws ParserException {
		final List<ReputationEntry> result = new ArrayList<ReputationEntry>();
		String htmlStr = new String(data, UTF_8);
		// check for offline indicator
		if (htmlStr.contains("<title>Offline - ")) {
			return result; 
		}
		Parser html = new Parser(htmlStr);
		// filter question summaries
		NodeList lst = html.parse(new NodeFilter() {
			private static final long serialVersionUID = -4798449277408336566L;
			@Override
			public boolean accept(Node node) {
				if (node instanceof Tag) {
					Tag t = (Tag)node;
					if (tagWithAttrIs(t, "table", "class", "history-table")) {
						return true;
					}
				}
				return false;
			}
		});
		if (lst.size() > 0) {
			visitor((Tag)lst.elementAt(0), new Associator() {
				/** The current reputation entry. */
				ReputationEntry re;
				@Override
				public void associate(Tag t) {
					if (t.getTagName().equalsIgnoreCase("tr")) {
						re = new ReputationEntry();
						result.add(re);
					}
					if (re == null) {
						return;
					}
					if (tagWithAttrIs(t, "div", "class", "date")) {
						String d = getTextOf(t);
						String v = d.substring(0, d.length() - 1);
						if (d.endsWith("h")) {
							re.time = System.currentTimeMillis() - Long.parseLong(v) * 60 * 60 * 1000;
						} else
						if (d.endsWith("d")) {
							re.time = System.currentTimeMillis() - Long.parseLong(v) * 60 * 60 * 1000 * 24;
						} else
						if (d.endsWith("m")) {
							re.time = System.currentTimeMillis() - Long.parseLong(v) * 60 * 1000;
						}
						if (d.endsWith("s")) {
							re.time = System.currentTimeMillis() - Long.parseLong(v) * 1000;
						}
//					} else
//					if (tagWithAttrIs(t, "div", "class", "date_brick")) {
//						String d = getTextOf(t);
					}
				}
			});
		}
		
		return result;
	}
	/**
	 * Returns the HTML page for the user stats page for the given site.
	 * @param site the site to query
	 * @param id the user identifier at that site
	 * @return the byte array containing the HTML data
	 * @throws IOException
	 * @throws HttpException
	 * @throws FileNotFoundException
	 */
	public static byte[] getAUserData(String site, String id) throws IOException, HttpException,
	FileNotFoundException {
		HttpClient client = new HttpClient();
		client.getParams().setParameter(HttpMethodParams.USER_AGENT,
	     "Mozilla/5.0 (Windows; U; Windows NT 6.1; hu; rv:1.9.1.1) Gecko/20090715 Firefox/3.5.1");		
		client.getParams().setParameter("Connection", "close");
		client.getParams().setParameter("Cache-Control", "no-cache");
		client.getParams().setParameter("Pragma", "no-cache");
		// TODO if it is superuser, lie us in
		if (site.contains("superuser.com")) {
			PostMethod post = new PostMethod(site + "/beta-access");
			post.setParameter("password", "ewok.adventure");
			client.executeMethod(post);
		}		
		HttpMethod method = new GetMethod(site + "/users/" + id + "?tab=stats");
		int ret = client.executeMethod(method);
		if (ret != 200) {
			System.err.println(ret);
		}
		byte[] data = method.getResponseBody();
		method.releaseConnection();
		return data;
	}
	/**
	 * Parses the stats page for the public user profile.
	 * @param data the data which contains
	 * @return the user profile, returns null if the site is offline or UserProfile.id is null
	 */
	public static UserProfile parseUserProfileStats(byte[] data) throws ParserException {
		final UserProfile up = new UserProfile();
		String htmlStr = new String(data, UTF_8);
		// check for offline indicator
		if (htmlStr.contains("<title>Offline - ")) {
			return null; 
		}
		Parser html = new Parser(htmlStr);
		// filter question summaries
		NodeList lst = html.parse(getTagAcceptor("html"));
		if (lst.size() > 0) {
			final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			visitor((Tag)lst.elementAt(0), new Associator() {
				String currentTag;
				BadgeEntry currentBadge;
				boolean repFound = false;
				boolean badgeMode = false;
				boolean lastSeen = false;
				/** The current reputation entry. */
				@Override
				public void associate(Tag t) {
					if (t.getTagName().equalsIgnoreCase("img") && ((Tag)t.getParent()).getTagName().equalsIgnoreCase("td")) {
						up.avatarUrl = replaceEntities(t.getAttribute("src"));
					} else
					if (tagWithAttrContains(t, "div", "class", "summarycount") && !repFound) {
						String s = getNumberFrom(t).trim();
						up.reputation = Integer.parseInt(s);
						repFound = true;
					} else
					if (tagWithAttrContains(t, "td", "class", "summaryinfo")) {
						String s = getTextOf(t);
						if (s.endsWith("views") || s.endsWith("view")) {
							s = getNumberFrom(s.substring(0, s.indexOf(' ')).trim());
							up.views = Integer.parseInt(s);
						}
					} else
					if (tagWithAttrEnds(t, "span", "title", "Z UTC")) {
						String s = t.getAttribute("title");
						s = s.substring(0, s.lastIndexOf("Z UTC"));
						if (!lastSeen) {
							try {
								up.lastSeen = sdf.parse(s).getTime();
							} catch (ParseException e) {
								e.printStackTrace();
							}
							lastSeen = true;
						}
					} else
					if (t.getTagName().equalsIgnoreCase("title")) {
						String s = getTextOf(t).trim();
						int uend = s.lastIndexOf('-');
						if (s.startsWith("User ")) {
							up.name = replaceEntities(s.substring(5, uend)).trim();
						}
					} else
					if (tagWithAttrContains(t, "div", "class", "user-about-me")) {
						up.description = t.getChildren().toHtml();
					} else
					if (tagWithAttrContains(t, "a", "class", "question-hyperlink")) {
						String s = t.getAttribute("href");
						up.questions.add(s.substring(11, s.indexOf('/', 12)));
					} else
					if (tagWithAttrContains(t, "a", "class", "answer-hyperlink")) {
						String s = t.getAttribute("href");
						String qid = s.substring(11, s.indexOf('/', 12));
						String pid = s.substring(s.lastIndexOf("#") + 1);
						up.answers.add(qid + "/" + pid);
					} else
					if (tagWithAttrContains(t, "span", "class", "vote-count-post") && tagWithAttrContains(t, "span", "title", "total number of")) {
						String vt = t.getAttribute("title");
						String v = getNumberFrom(t).trim();
						if (vt.contains("up votes")) {
							up.upvotes = Integer.parseInt(v);
						} else
						if (vt.contains("down votes")) {
							up.downvotes = Integer.parseInt(v);
						}
					} else
					if (tagWithAttrStarts(t, "a", "href", "/questions/tagged/")) {
						// if no multiplier, post it with count 1
						if (currentTag != null) {
							up.tagActivity.put(currentTag, 1);
							currentTag = null;
						}
						currentTag = replaceEntities(getTextOf(t));
					} else
					if (tagWithAttrContains(t, "span", "class", "item-multiplier") && (currentTag != null || currentBadge != null)) {
						String v = getTextOf(t);
						int j = v.length() - 1;
						while (j >= 0 && (Character.isDigit(v.charAt(j)) || v.charAt(j) == '.' || v.charAt(j) == ',')) {
							j--;
						}
						String v2 = v.substring(j + 1);
						if (currentTag != null && !badgeMode) {
							up.tagActivity.put(currentTag, Integer.valueOf(v2));
							currentTag = null;
						} else
						if (currentBadge != null && badgeMode) {
							currentBadge.count = Integer.parseInt(v2);
							currentBadge = null;
						}
					} else
					if (tagWithAttrContains(t, "a", "href", "/badges/") && tagWithAttrContains(t, "a", "class", "badge")) {
						String v = t.getAttribute("href");
						badgeMode = true;
						// if no counter, just post it with X 1
						currentBadge = new BadgeEntry();
						currentBadge.id = v.substring(8, v.indexOf('/', 9));
						currentBadge.count = 1;
						up.badgeActivity.put(currentBadge.id, currentBadge);
						currentBadge.title = t.getAttribute("title");
						String s = getTextOf(t);
						currentBadge.name = s.substring(s.lastIndexOf("&nbsp;") + 6);
						if (currentBadge.title.startsWith("bronze badge:")) {
							currentBadge.level = BadgeLevel.BRONZE;
						} else
						if (currentBadge.title.startsWith("silver badge:")) {
							currentBadge.level = BadgeLevel.SILVER;
						} else
						if (currentBadge.title.startsWith("gold badge:")) {
							currentBadge.level = BadgeLevel.GOLD;
						} else {
							System.err.println("Badge type mismatch: " + currentBadge.title);
						}
					}
				}
			});
		}
		return up;
	}
	public static void main(String[] args) throws Exception {
		byte[] data = getAUserData("http://stackoverflow.com", "61158");
		UserProfile up = parseUserProfileStats(data);
		System.out.println(up);
	}
	/**
	 * Returns the data from the users listing page.
	 * @param site the target site
	 * @param page the user page
	 * @return the byte data
	 * @throws IOException
	 * @throws HttpException
	 */
	public static byte[] getUsers(String site, int page) throws IOException, HttpException {
		HttpClient client = new HttpClient();
		client.getParams().setParameter(HttpMethodParams.USER_AGENT,
	     "Mozilla/5.0 (Windows; U; Windows NT 6.1; hu; rv:1.9.1.1) Gecko/20090715 Firefox/3.5.1");		
		client.getParams().setParameter("Connection", "close");
		client.getParams().setParameter("Cache-Control", "no-cache");
		client.getParams().setParameter("Pragma", "no-cache");
		// TODO if it is superuser, lie us in
		if (site.contains("superuser.com")) {
			PostMethod post = new PostMethod(site + "/beta-access");
			post.setParameter("password", "ewok.adventure");
			client.executeMethod(post);
		}		
		HttpMethod method = new GetMethod(site + "/users?page=" + page);
		int ret = client.executeMethod(method);
		if (ret != 200) {
			System.err.println(ret);
		}
		byte[] data = method.getResponseBody();
		method.releaseConnection();
		return data;
	}
	/**
	 * Parses the stats page for the public user profile.
	 * @param data the data which contains
	 * @return the user profile, returns null if the site is offline or UserProfile.id is null
	 */
	public static List<BasicUserInfo> parseUsers(byte[] data) throws ParserException {
		final List<BasicUserInfo> ups = new ArrayList<BasicUserInfo>();
		String htmlStr = new String(data, UTF_8);
		// check for offline indicator
		if (htmlStr.contains("<title>Offline - ")) {
			return ups; 
		}
		Parser html = new Parser(htmlStr);
		// filter question summaries
		NodeList lst = html.parse(new NodeFilter() {
			private static final long serialVersionUID = -6162280697937343870L;

			@Override
			public boolean accept(Node node) {
				if (node instanceof Tag) {
					Tag t = (Tag)node;
					if (tagWithAttrContains(t, "div", "class", "user-info" )) {
						return true;
					}
				}
				return false;
			}
		});
		for (int i = 0; i < lst.size(); i++) {
			final BasicUserInfo bui = new BasicUserInfo();
			ups.add(bui);
			visitor((Tag)lst.elementAt(i), new Associator() {
				@Override
				public void associate(Tag t) {
					if (tagWithAttrStarts(t, "a", "href", "/users/")) {
						String v = t.getAttribute("href");
						bui.id = v.substring(7, v.indexOf('/', 8));
						bui.name = getTextOf(t);
					} else
					if (tagWithAttrStarts(t, "img", "src", "http")) {
						bui.avatarUrl = replaceEntities(t.getAttribute("src"));
					} else
					if (tagWithAttrContains(t, "span", "class", "reputation-score")) {
						bui.reputation = Integer.parseInt(getNumberFrom(t).trim());
					}
				}
			});
		}
		return ups;
	}
	/**
	 * Returns a new Tag acceptor.
	 * @param tagName the tag to check
	 * @return the node filter
	 */
	public static NodeFilter getTagAcceptor(final String tagName) {
		return new NodeFilter() {
			private static final long serialVersionUID = -3930964184187161561L;

			@Override
			public boolean accept(Node node) {
				if (node instanceof Tag) {
					Tag t = (Tag)node;
					if (t.getTagName().equalsIgnoreCase(tagName)) {
						return true;
					}
				}
				return false;
			}
		};
	}
}
