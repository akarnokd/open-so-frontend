package hu.openso.frontend;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class QuestionsGUI extends JFrame {
	private static final long serialVersionUID = 5676803531378664660L;
	static final String version = "0.61";
	
	Map<String, ImageIcon> avatars = new ConcurrentHashMap<String, ImageIcon>();
	Map<String, ImageIcon> avatarsLoading = new ConcurrentHashMap<String, ImageIcon>();
	Map<String, ImageIcon> siteIcons = new HashMap<String, ImageIcon>();
	// set of known wikis
	Map<String, String> knownWikis = new ConcurrentHashMap<String, String>();
	ExecutorService exec = Executors.newFixedThreadPool(5);
	/** The global ignore table for site/id. */
	Map<String, String> globalIgnores = new LinkedHashMap<String, String>();
	JTabbedPane tabs;
	/** The main views of the application. */
	JTabbedPane views;
	/** Main tab for individual questions/answers. */
	JTabbedPane answers;
	/** Main tab for users. */
	JTabbedPane users;
	/** A completely empty panel. */
	JPanel EMPTY_PANEL_Q = new JPanel();
	JPanel EMPTY_PANEL_A = new JPanel();
	JPanel EMPTY_PANEL_U = new JPanel();
	boolean disableTabChange;
	IgnoreListGUI globalIgnoreListGUI;
	protected boolean disableAnswersChange;
	protected boolean disableUsersChange;
	public QuestionsGUI() {
		super("Open Stack Overflow Frontend v" + version + " - Questions");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				doExit();
			}
		});
		try {
			setIconImage(ImageIO.read(getClass().getResource("res/logo.png")));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		globalIgnoreListGUI = new IgnoreListGUI(globalIgnores);
		globalIgnoreListGUI.setTitle("Global Ignore List");
		
		views = new JTabbedPane();
		
		{
			tabs = new JTabbedPane();
			QuestionContext qc = createQuestionContext();
			QuestionPanel p = new QuestionPanel(qc);
			tabs.insertTab("Questions " + (tabs.getTabCount() + 1), null, p, null, tabs.getTabCount());
			TitleWithClose component = new TitleWithClose("Questions " + (tabs.getTabCount()), tabs, p);
			p.setTabTitle(component);
			
			tabs.setTabComponentAt(0, component);
			
			tabs.addTab("+", null, EMPTY_PANEL_Q, "Open new tab");
			tabs.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (!disableTabChange) {
						doTabClicked();
					}
				}
			});
		}
		{
			answers = new JTabbedPane();
			JPanel apanel = new JPanel(); // TODO replace with concrete panel
			answers.insertTab("Q&A " + (answers.getTabCount() + 1), null, apanel, null, answers.getTabCount());
			TitleWithClose atitle = new TitleWithClose("Q&A " + (answers.getTabCount()), answers, apanel);
			
			answers.setTabComponentAt(0, atitle);
			
			answers.addTab("+", null, EMPTY_PANEL_A, "Open new tab");
			answers.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (!disableAnswersChange) {
						doAnswersClicked();
					}
				}
			});
		}
		
		users = new JTabbedPane();
		{
			users = new JTabbedPane();
			JPanel upanel = new JPanel(); // TODO replace with concrete panel
			users.insertTab("User " + (users.getTabCount() + 1), null, upanel, null, users.getTabCount());
			TitleWithClose atitle = new TitleWithClose("User " + (users.getTabCount()), users, upanel);
			
			users.setTabComponentAt(0, atitle);
			
			users.addTab("+", null, EMPTY_PANEL_U, "Open new tab");
			users.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (!disableUsersChange) {
						doUsersClicked();
					}
				}
			});
		}
		
		views.addTab("Question Listings", tabs);
		views.addTab("Q&As", answers);
		views.addTab("Users", users);
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(views, BorderLayout.CENTER);
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				pack();
				setLocationRelativeTo(null);
				initConfig();
				setVisible(true);
			}
		});
	}
	/**
	 * 
	 */
	protected void doUsersClicked() {
		if (users.getSelectedComponent() == EMPTY_PANEL_U) {
			disableUsersChange = true;
			JPanel component = new JPanel();
			users.insertTab("", null, component, null, users.getTabCount() - 1);
			TitleWithClose tabComponent = new TitleWithClose("Users " + (users.getTabCount() - 1), users, component);
			
			users.setTabComponentAt(users.getTabCount() - 2, tabComponent);
			disableUsersChange = false;
			users.setSelectedIndex(users.getTabCount() - 2);
		}
	}
	/**
	 * 
	 */
	protected void doAnswersClicked() {
		if (answers.getSelectedComponent() == EMPTY_PANEL_A) {
			disableAnswersChange = true;
			JPanel component = new JPanel();
			answers.insertTab("", null, component, null, answers.getTabCount() - 1);
			TitleWithClose tabComponent = new TitleWithClose("Q&A " + (answers.getTabCount() - 1), answers, component);

			answers.setTabComponentAt(answers.getTabCount() - 2, tabComponent);
			disableAnswersChange = false;
			answers.setSelectedIndex(answers.getTabCount() - 2);
		}
	}
	protected void doTabClicked() {
		if (tabs.getSelectedComponent() == EMPTY_PANEL_Q) {
			disableTabChange = true;
			QuestionContext qc = createQuestionContext();

			QuestionPanel component = new QuestionPanel(qc);
			tabs.insertTab("", null, component, null, tabs.getTabCount() - 1);
			TitleWithClose tabComponent = new TitleWithClose("Questions " + (tabs.getTabCount() - 1), tabs, component);
			component.setTabTitle(tabComponent);
			tabs.setTabComponentAt(tabs.getTabCount() - 2, tabComponent);
			disableTabChange = false;
			tabs.setSelectedIndex(tabs.getTabCount() - 2);
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new QuestionsGUI().setVisible(true);
			}
		});
	}

	/** Initialize the window based on the configuration file. */
	private void initConfig() {
		try {
			FileInputStream in = new FileInputStream("config.xml");
			try {
				Properties p = new Properties();
				p.loadFromXML(in);
				// set window statuses
				String winstat = p.getProperty("WindowStatus");
				setExtendedState(Integer.parseInt(winstat));
				if (getExtendedState() == JFrame.NORMAL) {
					Rectangle rect = new Rectangle();
					rect.x = Integer.parseInt(p.getProperty("X"));
					rect.y = Integer.parseInt(p.getProperty("Y"));
					rect.width = Integer.parseInt(p.getProperty("Width"));
					rect.height = Integer.parseInt(p.getProperty("Height"));
					setBounds(rect);
				}
				String pc = p.getProperty("PanelCount");
				if (pc != null) {
					int panels = Integer.parseInt(pc);
					if (panels > 0) {
						disableTabChange = true;
						tabs.removeTabAt(0); // remove default tab
						for (int i = 0; i < panels; i++) {
							QuestionContext qc = createQuestionContext();

							QuestionPanel component = new QuestionPanel(qc);
							tabs.insertTab("", null, component, null, i);
							String title = p.getProperty("P" + i + "-Title");
							if (title == null) {
								title = "Questions " + (i + 1);
							}
							TitleWithClose tabTitle = new TitleWithClose(title, tabs, component);
							component.setTabTitle(tabTitle);
							tabs.setTabComponentAt(i, tabTitle);
							component.initPanel(i, p);
						}
						disableTabChange = false;
						tabs.setSelectedIndex(0);
					}
				}
				String apc = p.getProperty("AnswerPanelCount");
				if (apc != null) {
					int panels = Integer.parseInt(apc);
					if (panels > 0) {
						disableAnswersChange = true;
						answers.removeTabAt(0); // remove default tab
						for (int i = 0; i < panels; i++) {
							JPanel component = new JPanel();
							answers.insertTab("", null, component, null, i);
							String title = p.getProperty("A" + i + "-Title");
							if (title == null) {
								title = "Q&A " + (i + 1);
							}
							TitleWithClose tabTitle = new TitleWithClose(title, answers, component);
							answers.setTabComponentAt(i, tabTitle);
						}
						disableAnswersChange = false;
						answers.setSelectedIndex(0);
					}
				}
				String upc = p.getProperty("UserPanelCount");
				if (upc != null) {
					int panels = Integer.parseInt(upc);
					if (panels > 0) {
						disableUsersChange = true;
						users.removeTabAt(0); // remove default tab
						for (int i = 0; i < panels; i++) {
							JPanel component = new JPanel();
							users.insertTab("", null, component, null, i);
							String title = p.getProperty("U" + i + "-Title");
							if (title == null) {
								title = "User " + (i + 1);
							}
							TitleWithClose tabTitle = new TitleWithClose(title, users, component);
							users.setTabComponentAt(i, tabTitle);
						}
						disableUsersChange = false;
						users.setSelectedIndex(0);
					}
				}
				String ignoreCnt = p.getProperty("IgnoreCount");
				if (ignoreCnt != null) {
					int ic = Integer.parseInt(ignoreCnt);
					for (int i = 0; i < ic; i++) {
						globalIgnores.put(p.getProperty("Ignore" + i), p.getProperty("IgnoreTitle" + i));
					}
				}
				String wikiCnt = p.getProperty("KnownWikisCount");
				if (wikiCnt != null) {
					int ic = Integer.parseInt(wikiCnt);
					for (int i = 0; i < ic; i++) {
						globalIgnores.put(p.getProperty("KnownWikis" + i), "");
					}
				}
			} finally {
				in.close();
			}
		} catch (IOException ex) {
			// ignore
		}
	}
	/**
	 * @return
	 */
	private QuestionContext createQuestionContext() {
		QuestionContext qc = new QuestionContext()
		.setAvatars(avatars)
		.setAvatarsLoading(avatarsLoading)
		.setSiteIcons(siteIcons)
		.setExec(exec)
		.setGlobalIgnores(globalIgnores)
		.setGlobalIgnoreListGUI(globalIgnoreListGUI)
		.setKnownWikis(knownWikis);
		;
		return qc;
	}
	/** Save the window state to configuration file. */
	private void doneConfig() {
		try {
			Properties p = new Properties();
			p.setProperty("WindowStatus", Integer.toString(getExtendedState()));
			Rectangle rect = getBounds();
			p.setProperty("X", Integer.toString(rect.x));
			p.setProperty("Y", Integer.toString(rect.y));
			p.setProperty("Width", Integer.toString(rect.width));
			p.setProperty("Height", Integer.toString(rect.height));
			p.setProperty("PanelCount", Integer.toString(tabs.getTabCount() - 1));
			for (int i = 0; i < tabs.getTabCount(); i++) {
				Component c = tabs.getComponentAt(i);
				if (c instanceof QuestionPanel) {
					QuestionPanel component = (QuestionPanel)c;
					TitleWithClose tc = (TitleWithClose)tabs.getTabComponentAt(i);
					p.setProperty("P" + i + "-Title", tc.getTitle());
					component.donePanel(i, p);
				}
			}
			p.setProperty("AnswerPanelCount", Integer.toString(answers.getTabCount() - 1));
			for (int i = 0; i < answers.getTabCount(); i++) {
				Component c = answers.getComponentAt(i);
				if (c != EMPTY_PANEL_A) {
					TitleWithClose tc = (TitleWithClose)answers.getTabComponentAt(i);
					p.setProperty("A" + i + "-Title", tc.getTitle());
				}
			}
			p.setProperty("UserPanelCount", Integer.toString(users.getTabCount() - 1));
			for (int i = 0; i < users.getTabCount(); i++) {
				Component c = users.getComponentAt(i);
				if (c != EMPTY_PANEL_U) {
					TitleWithClose tc = (TitleWithClose)users.getTabComponentAt(i);
					p.setProperty("U" + i + "-Title", tc.getTitle());
				}
			}
			p.setProperty("IgnoreCount", Integer.toString(globalIgnores.size()));
			int i = 0;
			for (Map.Entry<String, String> e: globalIgnores.entrySet()) {
				p.setProperty("Ignore" + i, e.getKey());
				p.setProperty("IgnoreTitle" + i, e.getValue());
				i++;
			}
			p.setProperty("KnownWikisCount", Integer.toString(knownWikis.size()));
			for (Map.Entry<String, String> e: knownWikis.entrySet()) {
				p.setProperty("KnownWikis" + i, e.getKey());
				i++;
			}
			
			FileOutputStream out = new FileOutputStream("config.xml");
			try {
				p.storeToXML(out, "");
			} finally {
				out.close();
			}
		} catch (IOException ex) {
			
		}
	}
	/**
	 * 
	 */
	private void doExit() {
		exec.shutdown();
		globalIgnoreListGUI.dispose();
		globalIgnoreListGUI = null;
		doneConfig();
	}
}
