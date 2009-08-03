package hu.openso.frontend;

import java.util.ArrayList;
import java.util.List;

public class VoteHistoryEntry {
	/** The history entry. */
	public long time;
	/** Current absolute value. */
	public int value;
	/** Delta since last seen. */
	public int delta;
	/**
	 * The location Ids in a form of site / questionid [/answerid].
	 */
	public final List<String> locationIds = new ArrayList<String>();
}
