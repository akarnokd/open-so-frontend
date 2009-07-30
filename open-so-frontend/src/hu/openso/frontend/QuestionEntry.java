package hu.openso.frontend;

import java.util.ArrayList;
import java.util.List;

public class QuestionEntry {
	public String id;
	public String title;
	public int votes;
	public int views;
	public int favorite;
	public String post;
	public final List<String> tags = new ArrayList<String>();
	public final BasicUserInfo creator = new BasicUserInfo();
	public BasicUserInfo editor;
	public long created;
	public long edited;
	public boolean wiki;
	public final List<PostComment> comments = new ArrayList<PostComment>();
	public final List<AnswerEntry> answers = new ArrayList<AnswerEntry>();
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(512);
		b
		.append("<question>\r\n")
		.append("\t<id>").append(id).append("</id>\r\n")
		.append("\t<title>").append(title).append("</title>\r\n")
		.append("\t<votes>").append(votes).append("</votes>\r\n")
		.append("\t<views>").append(views).append("</views>\r\n")
		.append("\t<favorite>").append(favorite).append("</favorite>\r\n")
		.append("\t<post>").append(post).append("</post>\r\n")
		.append("\t<tags>\r\n").append(getTagsXML()).append("\t</tags>\r\n")
		.append("\t<creator>\r\n").append(creator).append("\t</creator>\r\n")
		.append("\t<editor>\r\n").append(editor != null ? editor : "").append("\t</editor>\r\n")
		.append("\t<created>").append(created).append("</created>\r\n")
		.append("\t<edited>").append(edited).append("</edited>\r\n")
		.append("\t<wiki>").append(wiki).append("</wiki>\r\n")
		.append("\t<comments>\r\n").append(getCommentsXML()).append("\t</comments>\r\n")
		.append("\t<answers>\r\n").append(getAnswersXML()).append("\t</answers>\r\n")
		.append("</question>\r\n")
		;
		return b.toString();
	}
	private StringBuilder getTagsXML() {
		StringBuilder b = new StringBuilder();
		for (String s : tags) {
			b.append("\t\t<tag>").append(s).append("</tag>\r\n");
		}
		return b;
	}
	private StringBuilder getCommentsXML() {
		StringBuilder b = new StringBuilder();
		for (PostComment s : comments) {
			b.append("\t\t<comment>\r\n\t\t\t").append(s).append("\t\t</comment>\r\n");
		}
		return b;
	}
	private StringBuilder getAnswersXML() {
		StringBuilder b = new StringBuilder();
		for (AnswerEntry s : answers) {
			b.append(s);
		}
		return b;
	}
}
