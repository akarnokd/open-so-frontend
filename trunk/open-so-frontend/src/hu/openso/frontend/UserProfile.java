package hu.openso.frontend;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	/** The user's display name: Optional. */
	public String name;
	/** User is member since this date. */
	public long memberSince;
	/** When was this user last seen? .*/
	public long lastSeen;
	/** The user's website. Optional. */
	public String website;
	/** The user's location. Optional. */
	public String location;
	/** The user's age. Optional. */
	public Integer age;
	/** The URL to the user's avatar. */
	public String avatarUrl;
	/** The current exact reputation score. */
	public int reputation;
	/** The current amount of profile views. */
	public int views;
	/** The user's self description as HTML text. */
	public String description;
	/** The list of question ids this user asked. */
	public final List<String> questions = new ArrayList<String>(); // TODO use a more appropriate object type
	/** The list of question id/answer id this user posted. */
	public final List<String> answers = new ArrayList<String>(); // TODO use a more appropriate object type
	/** Number of upvotes this user did. */
	public int upvotes;
	/** Number of downvotes this user did. */
	public int downvotes;
	/** The tag name to activity number map. */
	public final Map<String, Integer> tagActivity = new HashMap<String, Integer>();
	/** The badges received by this user. */
	public final Map<String, BadgeEntry> badgeActivity = new HashMap<String, BadgeEntry>();
	/** The list of user activity. */
	public final List<Object> activity = new ArrayList<Object>();
	/**
	 * Returns the number of badges for the given level.
	 * @param level the badge level enum
	 * @return the number of badges
	 */
	public int getBadgeCount(BadgeLevel level) {
		int sum = 0;
		for (BadgeEntry be : badgeActivity.values()) {
			if (be.level == level) {
				sum += be.count;
			}
		}
		return sum;
	}
	/**
	 * Returns the tag names in descending order, regarding the number of items.
	 * @return the list of tags
	 */
	public List<String> getTagsByCountDesc() {
		List<String> result = new ArrayList<String>(tagActivity.keySet());
		Collections.sort(result, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return tagActivity.get(o2).compareTo(tagActivity.get(o1));
			}
		});
		return result;
	}
	/**
	 * {@inheritDoc}
	 */
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
