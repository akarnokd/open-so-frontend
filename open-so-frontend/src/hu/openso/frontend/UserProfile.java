package hu.openso.frontend;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfile implements Serializable {
	private static final long serialVersionUID = 8851306148584096762L;
	/** The user id. */
	public String id;
	/** The site. */
	public String site;
	/** Is this user registered? */
	public boolean isRegistered;
	public String name;
	public long memberSince;
	public long lastSeen;
	public String website;
	public String location;
	public Integer age;
	public String avatarUrl;
	public int reputation;
	public int views;
	/** The user's self description as HTML text. */
	public String description;
	public final List<String> questions = new ArrayList<String>(); // TODO use a more appropriate object type
	public final List<String> answers = new ArrayList<String>(); // TODO use a more appropriate object type
	public int upvotes;
	public int downvotes;
	/** The tag name to activity number map. */
	public final Map<String, Integer> tagActivity = new HashMap<String, Integer>();
	/** The badges received by this user. */
	public final Map<String, BadgeEntry> badgeActivity = new HashMap<String, BadgeEntry>();
	/** The list of user activity. */
	public final List<Object> activity = new ArrayList<Object>();
	@Override
	public String toString() {
		return "UserProfile [activity=" + activity 
		        + "\r\n\t, age=" + age
				+ "\r\n\t, answers=" + answers
				+ "\r\n\t, avatarUrl=" + avatarUrl
				+ "\r\n\t, badgeActivity=" + badgeActivity
				+ "\r\n\t, description=" + description
				+ "\r\n\t, downvotes=" + downvotes 
				+ "\r\n\t, id=" + id
				+ "\r\n\t, isRegistered=" + isRegistered
				+ "\r\n\t, lastSeen=" + lastSeen
				+ "\r\n\t, location=" + location
				+ "\r\n\t, memberSince=" + memberSince
				+ "\r\n\t, name=" + name
				+ "\r\n\t, questions=" + questions
				+ "\r\n\t, reputation=" + reputation
				+ "\r\n\t, site=" + site
				+ "\r\n\t, tagActivity=" + tagActivity
				+ "\r\n\t, upvotes=" + upvotes
				+ "\r\n\t, views=" + views
				+ "\r\n\t, website=" + website + "]";
	}
	
}
