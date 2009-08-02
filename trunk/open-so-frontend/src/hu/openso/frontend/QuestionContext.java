/*
 * Classname            : hu.openso.frontend.QuestionContext
 * Version information  : 1.0
 * Date                 : 2009.08.02.
 * Copyright notice     : GE Consumer & Industrial
 */

package hu.openso.frontend;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.swing.ImageIcon;

/**
 * Question context object to avoid passing 6+ parameters around.
 * @author karnokd, 2009.08.02.
 * @version $Revision 1.0$
 */
public class QuestionContext {
	public Map<String, ImageIcon> avatars;
	public Map<String, ImageIcon> avatarsLoading; 
	public Map<String, ImageIcon> siteIcons;
	public ExecutorService exec; 
	public Map<String, String> globalIgnores; 
	public IgnoreListGUI globalIgnoreListGUI;
	public Map<String, Boolean> knownWikis;
	/**
	 * @param avatars the avatars to set
	 */
	public QuestionContext setAvatars(Map<String, ImageIcon> avatars) {
		this.avatars = avatars;
		return this;
	}
	/**
	 * @param avatarsLoading the avatarsLoading to set
	 */
	public QuestionContext setAvatarsLoading(Map<String, ImageIcon> avatarsLoading) {
		this.avatarsLoading = avatarsLoading;
		return this;
	}
	/**
	 * @param siteIcons the siteIcons to set
	 */
	public QuestionContext setSiteIcons(Map<String, ImageIcon> siteIcons) {
		this.siteIcons = siteIcons;
		return this;
	}
	/**
	 * @param exec the exec to set
	 */
	public QuestionContext setExec(ExecutorService exec) {
		this.exec = exec;
		return this;
	}
	/**
	 * @param globalIgnores the globalIgnores to set
	 */
	public QuestionContext setGlobalIgnores(Map<String, String> globalIgnores) {
		this.globalIgnores = globalIgnores;
		return this;
	}
	/**
	 * @param globalIgnoreListGUI the globalIgnoreListGUI to set
	 */
	public QuestionContext setGlobalIgnoreListGUI(IgnoreListGUI globalIgnoreListGUI) {
		this.globalIgnoreListGUI = globalIgnoreListGUI;
		return this;
	}
	/**
	 * @param knownWikis the knownWikis to set
	 */
	public QuestionContext setKnownWikis(Map<String, Boolean> knownWikis) {
		this.knownWikis = knownWikis;
		return this;
	}
}
