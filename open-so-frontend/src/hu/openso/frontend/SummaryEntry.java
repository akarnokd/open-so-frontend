/**
 * 
 */
package hu.openso.frontend;

import java.util.ArrayList;
import java.util.List;

class SummaryEntry {
	public String site;
	public String id;
	public int votes;
	public int answers;
	public boolean accepted;
	public int views;
	public String title;
	public String excerpt;
	public String userId;
	public String userName;
	public long time;
	public int userRep;
	public int goldBadges;
	public int silverBadges;
	public int bronzeBadges;
	public String avatarUrl;
	public final List<String> tags = new ArrayList<String>();
	// ----------------------------------------------
	public Boolean wiki;
	public int bounty;
	public boolean userClicked;
	@Override
	public String toString() {
		String result = "<entry>\r\n\t<id>" + id + "</id>\r\n\t<votes>" + votes + 
		"</votes>\r\n\t<answers>" + answers + "</answers>\r\n\t<accepted>" + 
		accepted + "</answered>\r\n\t<views>"
		+ views + "</views>\r\n\t<title>" + HTML.toHTML(title) + "</title>\r\n\t<excerpt>" + 
		HTML.toHTML(excerpt) + "</excerpt>\r\n\t<user-id>"
		+ userId + "</user-id>\r\n\t<user-name>"
		+ userName + "</user-name>\r\n\t" +
		"<time>" + time + "</time>\r\n"
		+ "\t<reputation>" + userRep + "</reputation>\r\n"
		+ "\t<gold-badges>" + goldBadges + "</gold-badges>\r\n"
		+ "\t<silver-badges>" + silverBadges + "</silver-badges>\r\n"
		+ "\t<bronze-badges>" + bronzeBadges + "</bronze-badges>\r\n"
		+ "\t<avatar-url>" + avatarUrl + "</avatar-url>\r\n"
		+ "\t<tags>\r\n";
		for (String s : tags) {
			result += "\t\t<tag>" + s + "</tag>\r\n";
		}
		result += "\t</tags>\r\n";
		result += "</entry>\r\n";
		return result;
	}
}