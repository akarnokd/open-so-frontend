/*
 * Classname            : hu.openso.frontend.QuestionContext
 * Version information  : 1.0
 * Date                 : 2009.08.02.
 * Copyright notice     : GE Consumer & Industrial
 */

package hu.openso.frontend;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.ImageIcon;

/**
 * Frontend context object to avoid passing 6+ parameters around.
 * @author karnokd, 2009.08.02.
 * @version $Revision 1.0$
 */
public class FrontendContext {
	public final Map<String, ImageIcon> avatars = new ConcurrentHashMap<String, ImageIcon>();
	public final Map<String, String> avatarsLoading = new ConcurrentHashMap<String, String>();
	public final Map<String, ImageIcon> siteIcons = new HashMap<String, ImageIcon>();
	/** Set of known wikis. */
	public final Map<String, Boolean> knownWikis = new ConcurrentHashMap<String, Boolean>();
	/** The global ignore table for site/id. */
	public final Map<String, String> globalIgnores = new LinkedHashMap<String, String>();

	public ExecutorService exec = Executors.newFixedThreadPool(5);
	public PanelManager panelManager;
	public ImageIcon rolling;
	public ImageIcon okay;
	public ImageIcon unknown;
	public ImageIcon error;
	public ImageIcon wiki;
	public IgnoreListGUI globalIgnoreListGUI;
	public ImageIcon go;
	public ImageIcon more;
	public ImageIcon clear;
	public ImageIcon warning;
	public ImageIcon downvote;
	/** Icon for new window. */
	public ImageIcon newwin;
	public FrontendContext() {
		rolling = new ImageIcon(getClass().getResource("res/loading.gif"));
		okay = new ImageIcon(getClass().getResource("res/ok.png"));
		unknown = new ImageIcon(getClass().getResource("res/unknown.png"));
		error = new ImageIcon(getClass().getResource("res/error.png"));
		warning = new ImageIcon(getClass().getResource("res/warning.png"));
		wiki = new ImageIcon(getClass().getResource("res/wiki.png"));
		go = new ImageIcon(getClass().getResource("res/go.png"));
		more = new ImageIcon(getClass().getResource("res/more.png"));
		newwin = new ImageIcon(getClass().getResource("res/newwin.gif"));
		clear = new ImageIcon(getClass().getResource("res/clear.png"));
		downvote = new ImageIcon(getClass().getResource("res/vote-arrow-down-on.png"));
		
		siteIcons.put("stackoverflow.com", new ImageIcon(getClass().getResource("res/so.png")));
		siteIcons.put("serverfault.com", new ImageIcon(getClass().getResource("res/sf.png")));
		siteIcons.put("superuser.com", new ImageIcon(getClass().getResource("res/su.png")));
		siteIcons.put("meta.stackoverflow.com", new ImageIcon(getClass().getResource("res/meta.png")));
		globalIgnoreListGUI = new IgnoreListGUI(globalIgnores);
		globalIgnoreListGUI.setTitle("Global Ignore List");
	}
	/**
	 * @param panelManager the panelManager to set
	 */
	public FrontendContext setPanelManager(PanelManager panelManager) {
		this.panelManager = panelManager;
		return this;
	}
}
