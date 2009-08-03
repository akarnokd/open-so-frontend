package hu.openso.frontend;

/**
 * Reputation entry on the reputation history tab.
 * @author karnokd
 */
public class ReputationEntry {
	enum RepEntryType {
		NORMAL,
		BOUNTY,
		ACCEPT
	}
	/** The approximate entry timestamp. */
	public long time;
	public int amount;
	public RepEntryType type;
	public int reputation;
	public String title;
	public String id;
	public String postId;
	/** The full URL to the question and post. */
	public String url;
}
