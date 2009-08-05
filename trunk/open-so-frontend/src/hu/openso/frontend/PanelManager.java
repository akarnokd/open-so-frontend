/*
 * Classname            : hu.openso.frontend.PanelManager
 * Version information  : 1.0
 * Date                 : 2009.08.03.
 * Copyright notice     : GE Consumer & Industrial
 */

package hu.openso.frontend;

import java.util.List;

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
	void openListingFor(String site, String tag);
	/**
	 * Open a concrete question panel for the given site and id.
	 * @param site the site id without http://
	 * @param qid the question id
	 * @param aid the answer id or null for the question
	 */
	void openQuestion(String site, String qid, String aid);
	/**
	 * Open a user settings
	 * @param sites the site names without http://
	 * @param ids the user id on the sites
	 * @param name the optional display name
	 */
	void openUser(String[] site, String[] id, String name);
	/**
	 * Register the given reputation float window to auto-save it.
	 * @param rf the reputation float.
	 */
	void registerRepFloat(ReputationFloat rf);
	/**
	 * Unregister the given reputation float window so it is no more autosaved.
	 * @param rf the reputation float
	 */
	void unregisterRepFloat(ReputationFloat rf);
	/**
	 * @return the list of currently active and floating reputation watchers.
	 */
	List<ReputationFloat> getRegisteredReputationFloats();
}
