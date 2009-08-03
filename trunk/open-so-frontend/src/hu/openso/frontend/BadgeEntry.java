package hu.openso.frontend;

import java.io.Serializable;

/**
 * The badge record.
 * @author karnokd
 */
public class BadgeEntry implements Serializable {
	private static final long serialVersionUID = -2632445767616664982L;
	enum BadgeLevel {
		BRONZE,
		SILVER,
		GOLD
	}
	public String id;
	public String name;
	/** The short badge description. */
	public String title;
	public BadgeLevel level;
	public int count;
	@Override
	public String toString() {
		return "BadgeEntry [count=" + count + ", id=" + id + ", level=" + level
				+ ", name=" + name + ", title=" + title + "]";
	}
	
}
