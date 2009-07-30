package hu.openso.frontend;

/**
 * Record for basic user information displayed on the pages.
 * @author karnokd
 */
public class BasicUserInfo {
	public String id;
	public String name;
	public String avatarUrl;
	public int reputation;
	public int goldBadges;
	public int silverBadges;
	public int bronzeBadges;
	public void assign(BasicUserInfo that) {
		id = that.id;
		name = that.name;
		avatarUrl = that.avatarUrl;
		reputation = that.reputation;
		goldBadges = that.goldBadges;
		silverBadges = that.silverBadges;
		bronzeBadges = that.bronzeBadges;
	}
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("\t\t<user-info>\r\n")
		.append("\t\t\t<id>").append(id).append("</id>\r\n")
		.append("\t\t\t<name>").append(name).append("</name>\r\n")
		.append("\t\t\t<avatar-url>").append(avatarUrl).append("</avatar-url>\r\n")
		.append("\t\t\t<reputation>").append(reputation).append("</reputation>\r\n")
		.append("\t\t\t<gold-badges>").append(goldBadges).append("</gold-badges>\r\n")
		.append("\t\t\t<silver-badges>").append(silverBadges).append("</silver-badges>\r\n")
		.append("\t\t\t<bronze-badges>").append(bronzeBadges).append("</bronze-badges>\r\n")
		.append("\t\t</user-info>\r\n")
		;
		return b.toString();
	}
	public String toString2() {
		StringBuilder b = new StringBuilder();
		b.append("\t\t\t\t<user-info>\r\n")
		.append("\t\t\t\t\t<id>").append(id).append("</id>\r\n")
		.append("\t\t\t\t\t<name>").append(name).append("</name>\r\n")
		.append("\t\t\t\t\t<avatar-url>").append(avatarUrl).append("</avatar-url>\r\n")
		.append("\t\t\t\t\t<reputation>").append(reputation).append("</reputation>\r\n")
		.append("\t\t\t\t\t<gold-badges>").append(goldBadges).append("</gold-badges>\r\n")
		.append("\t\t\t\t\t<silver-badges>").append(silverBadges).append("</silver-badges>\r\n")
		.append("\t\t\t\t\t<bronze-badges>").append(bronzeBadges).append("</bronze-badges>\r\n")
		.append("\t\t\t\t</user-info>\r\n")
		;
		return b.toString();
	}
}
