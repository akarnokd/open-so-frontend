/*
 * Classname            : hu.openso.frontend.PanelManager
 * Version information  : 1.0
 * Date                 : 2009.08.03.
 * Copyright notice     : GE Consumer & Industrial
 */

package hu.openso.frontend;

/**
 * Interface for cross-tab functionality.
 * @author karnokd, 2009.08.03.
 * @version $Revision 1.0$
 */
public interface PanelManager {
	/**
	 * Opens the question listings for the given site and tag.
	 * @param site the site id without http://
	 * @param tag the tag to use for search
	 */
	public void openListingFor(String site, String tag);
	/**
	 * Open a concrete question panel for the given site and id.
	 * @param site the site id without http://
	 * @param qid the question id
	 * @param aid the answer id or null for the question
	 */
	public void openQuestion(String site, String qid, String aid);
	/**
	 * Open a user settings
	 * @param site the site name without http://
	 * @param id the user id
	 * @param name the optional display name
	 */
	public void openUser(String site, String id, String name);
}
