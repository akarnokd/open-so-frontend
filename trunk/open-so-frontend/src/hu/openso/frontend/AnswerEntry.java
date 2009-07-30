package hu.openso.frontend;

import java.util.ArrayList;
import java.util.List;

public class AnswerEntry {
	public String id;
	public int votes;
	public String post;
	public String permalink;
	public final BasicUserInfo creator = new BasicUserInfo();
	public BasicUserInfo editor;
	public long created;
	public long edited;
	public final List<PostComment> comments = new ArrayList<PostComment>();
	public boolean wiki;
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(512);
		b
		.append("\t\t<answer>\r\n")
		.append("\t\t\t<id>").append(id).append("</id>\r\n")
		.append("\t\t\t<votes>").append(votes).append("</votes>\r\n")
		.append("\t\t\t<post>").append(post).append("</post>\r\n")
		.append("\t\t\t<permalink>").append(permalink).append("</permalink>\r\n")
		.append("\t\t\t<creator>\r\n").append(creator.toString2()).append("\t\t\t</creator>\r\n")
		.append("\t\t\t<editor>\r\n").append(editor != null ? editor.toString2() : "").append("\t\t\t</editor>\r\n")
		.append("\t\t\t<created>").append(created).append("</created>\r\n")
		.append("\t\t\t<edited>").append(edited).append("</edited>\r\n")
		.append("\t\t\t<wiki>").append(wiki).append("</wiki>\r\n")
		.append("\t\t\t<comments>\r\n").append(getCommentsXML()).append("\t\t\t</comments>\r\n")
		.append("\t\t</answer>\r\n")
		;
		return b.toString();
	}
	private StringBuilder getCommentsXML() {
		StringBuilder b = new StringBuilder();
		for (PostComment s : comments) {
			b.append("\t\t\t\t<comment>\r\n\t\t\t").append(s).append("\t\t\t\t</comment>\r\n");
		}
		return b;
	}
}
