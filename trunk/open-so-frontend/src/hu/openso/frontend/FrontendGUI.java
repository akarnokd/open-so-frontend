package hu.openso.frontend;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class FrontendGUI extends JFrame implements PanelManager {
	private static final long serialVersionUID = 5676803531378664660L;
	static final String version = "0.70a";
	
	JTabbedPane listings;
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
	protected boolean disableAnswersChange;
	protected boolean disableUsersChange;
	private FrontendContext mainQuestionContext;
	public FrontendGUI() {
		super("Open Stack Overflow Frontend v" + version);
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
		createQuestionContext();
		
		views = new JTabbedPane();
		
		{
			listings = new JTabbedPane();
			QuestionPanel p = new QuestionPanel(mainQuestionContext);
			listings.insertTab("Questions " + (listings.getTabCount() + 1), null, p, null, listings.getTabCount());
			TitleWithClose component = new TitleWithClose("Questions " + (listings.getTabCount()), listings, p);
			p.setTabTitle(component);
			
			listings.setTabComponentAt(0, component);
			
			listings.addTab("+", null, EMPTY_PANEL_Q, "Open new tab");
			listings.addChangeListener(new ChangeListener() {
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
			UserPanel upanel = new UserPanel(mainQuestionContext);
			users.insertTab("User " + (users.getTabCount() + 1), null, upanel, null, users.getTabCount());
			TitleWithClose utitle = new TitleWithClose("User " + (users.getTabCount()), users, upanel);
			upanel.setTabTitle(utitle);
			users.setTabComponentAt(0, utitle);
			
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
		
		views.addTab("Question Listings", listings);
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
				doVersionCheck();
			}
		});
	}
	/**
	 * 
	 */
	protected void doUsersClicked() {
		if (users.getSelectedComponent() == EMPTY_PANEL_U) {
			disableUsersChange = true;
			UserPanel component = new UserPanel(mainQuestionContext);
			users.insertTab("", null, component, null, users.getTabCount() - 1);
			TitleWithClose tabComponent = new TitleWithClose("Users " + (users.getTabCount() - 1), users, component);
			component.setTabTitle(tabComponent);
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
		if (listings.getSelectedComponent() == EMPTY_PANEL_Q) {
			disableTabChange = true;
			QuestionPanel component = new QuestionPanel(mainQuestionContext);
			listings.insertTab("", null, component, null, listings.getTabCount() - 1);
			TitleWithClose tabComponent = new TitleWithClose("Questions " + (listings.getTabCount() - 1), listings, component);
			component.setTabTitle(tabComponent);
			listings.setTabComponentAt(listings.getTabCount() - 2, tabComponent);
			disableTabChange = false;
			listings.setSelectedIndex(listings.getTabCount() - 2);
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new FrontendGUI().setVisible(true);
			}
		});
	}

	/** Initialize the window based on the configuration file. */
	private void initConfig() {
		try {
			File cx = new File("config.xml.gz");
			InputStream in = null;
			// open compressed config if there is one
			if (cx.exists()) {
			    in = new GZIPInputStream(new FileInputStream(cx), 4096);
			} else {
				in = new FileInputStream("config.xml");
			}
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
						listings.removeTabAt(0); // remove default tab
						for (int i = 0; i < panels; i++) {
							QuestionPanel component = new QuestionPanel(mainQuestionContext);
							listings.insertTab("", null, component, null, i);
							String title = p.getProperty("P" + i + "-Title");
							if (title == null) {
								title = "Questions " + (i + 1);
							}
							TitleWithClose tabTitle = new TitleWithClose(title, listings, component);
							component.setTabTitle(tabTitle);
							listings.setTabComponentAt(i, tabTitle);
							component.initPanel(i, p);
						}
						disableTabChange = false;
						listings.setSelectedIndex(0);
					}
				}
				String apc = p.getProperty("AnswerPanelCount");
				if (apc != null) {
					int panels = Integer.parseInt(apc);
					if (panels > 0) {
						disableAnswersChange = true;
						answers.removeTabAt(0); // remove default tab
						for (int i = 0; i < panels; i++) {
							JPanel component = new JPanel(); // TODO create appropriate panel
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
							UserPanel component = new UserPanel(mainQuestionContext);
							users.insertTab("", null, component, null, i);
							String title = p.getProperty("U" + i + "-Title");
							if (title == null) {
								title = "User " + (i + 1);
							}
							TitleWithClose tabTitle = new TitleWithClose(title, users, component);
							component.setTabTitle(tabTitle);
							users.setTabComponentAt(i, tabTitle);
							component.initPanel(i, p);
						}
						disableUsersChange = false;
						users.setSelectedIndex(0);
					}
				}
				String ignoreCnt = p.getProperty("IgnoreCount");
				if (ignoreCnt != null) {
					int ic = Integer.parseInt(ignoreCnt);
					for (int i = 0; i < ic; i++) {
						mainQuestionContext.globalIgnores.put(p.getProperty("Ignore" + i), p.getProperty("IgnoreTitle" + i));
					}
				}
				String wikiCnt = p.getProperty("KnownWikisCount");
				if (wikiCnt != null) {
					int ic = Integer.parseInt(wikiCnt);
					for (int i = 0; i < ic; i++) {
						String kwv = p.getProperty("KnownWikisValue" + i);
						mainQuestionContext.knownWikis.put(p.getProperty("KnownWikis" + i), kwv != null ? Boolean.parseBoolean(kwv) : true);
					}
				}
				String v = p.getProperty("ViewIndex");
				if (v != null) {
					views.setSelectedIndex(Integer.parseInt(v));
				}
				v = p.getProperty("TabsIndex");
				if (v != null) {
					listings.setSelectedIndex(Integer.parseInt(v));
				}
				v = p.getProperty("QuestionIndex");
				if (v != null) {
					answers.setSelectedIndex(Integer.parseInt(v));
				}
				v = p.getProperty("UserIndex");
				if (v != null) {
					users.setSelectedIndex(Integer.parseInt(v));
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
	private void createQuestionContext() {
		mainQuestionContext = new FrontendContext()
		.setPanelManager(this)
		;
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
			p.setProperty("PanelCount", Integer.toString(listings.getTabCount() - 1));
			
			p.setProperty("ViewIndex", Integer.toString(views.getSelectedIndex()));
			p.setProperty("TabsIndex", Integer.toString(listings.getSelectedIndex()));
			p.setProperty("QuestionIndex", Integer.toString(listings.getSelectedIndex()));
			p.setProperty("UserIndex", Integer.toString(users.getSelectedIndex()));
			
			for (int i = 0; i < listings.getTabCount(); i++) {
				Component c = listings.getComponentAt(i);
				if (c instanceof QuestionPanel) {
					QuestionPanel component = (QuestionPanel)c;
					TitleWithClose tc = (TitleWithClose)listings.getTabComponentAt(i);
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
				if (c instanceof UserPanel) {
					TitleWithClose tc = (TitleWithClose)users.getTabComponentAt(i);
					p.setProperty("U" + i + "-Title", tc.getTitle());
					((UserPanel)c).donePanel(i, p);
				}
			}
			p.setProperty("IgnoreCount", Integer.toString(mainQuestionContext.globalIgnores.size()));
			int i = 0;
			for (Map.Entry<String, String> e: mainQuestionContext.globalIgnores.entrySet()) {
				p.setProperty("Ignore" + i, e.getKey());
				p.setProperty("IgnoreTitle" + i, e.getValue());
				i++;
			}
			p.setProperty("KnownWikisCount", Integer.toString(mainQuestionContext.knownWikis.size()));
			for (Map.Entry<String, Boolean> e: mainQuestionContext.knownWikis.entrySet()) {
				p.setProperty("KnownWikis" + i, e.getKey());
				p.setProperty("KnownWikisValue" + i, Boolean.toString(e.getValue()));
				i++;
			}
			
			File cx = new File("config.xml");
			if (cx.exists()) {
				cx.delete();
			}
			OutputStream out = new GZIPOutputStream(new FileOutputStream("config.xml.gz"), 4096);
			try {
				p.storeToXML(out, "");
			} finally {
				out.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	/**
	 * 
	 */
	private void doExit() {
		mainQuestionContext.exec.shutdown();
		mainQuestionContext.globalIgnoreListGUI.dispose();
		mainQuestionContext.globalIgnoreListGUI = null;
		doneConfig();
	}
	protected void doVersionCheck() {
		SwingWorker<Void, Void> verWorker = new SwingWorker<Void, Void>() {
			private String ver;
			@Override
			protected Void doInBackground() throws Exception {
				ver = SOPageParsers.getOnlineVersion();
				return null;
			}
			@Override
			protected void done() {
				if (ver.length() > 0 && ver.compareTo(version) > 0) {
					if (JOptionPane.showConfirmDialog(FrontendGUI.this, 
							"<html><center>A newer version of the Open Stack Overflow Frontend is available:"
							+ "<br><font style='size: 16pt;'>" 
							+ ver + "</font><br>Do you want to download it?",
							"Open Stack Overflow Frontend " + version,
							JOptionPane.YES_NO_OPTION
							) == JOptionPane.YES_OPTION) {
						try {
							Desktop d = Desktop.getDesktop();
							if (d != null) {
								d.browse(new URI("http://code.google.com/p/open-so-frontend/"));
							}
						} catch (IOException ex) {
							ex.printStackTrace();
						} catch (URISyntaxException ex) {
							ex.printStackTrace();
						}
					}
				}
			}
		};
		verWorker.execute();
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void openListingFor(String site, String tag) {
		QuestionPanel component = new QuestionPanel(mainQuestionContext);
		int idx = listings.getTabCount() - 1;
		listings.insertTab("", null, component, null, idx);
		TitleWithClose tabTitle = new TitleWithClose("Questions:" + site + "|" +tag, listings, component);
		component.setTabTitle(tabTitle);
		listings.setTabComponentAt(idx, tabTitle);
		views.setSelectedIndex(0);
		listings.setSelectedIndex(idx);
		// TODO perform retrieve on the site and tag
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void openQuestion(String site, String qid, String aid) {
		
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void openUser(String site, String id, String name) {
		UserPanel component = new UserPanel(mainQuestionContext);
		int idx = users.getTabCount() - 1;
		users.insertTab("", null, component, null, idx);
		String title = null;
		if (name != null) {
			title = name + "@" + site;
		} else {
			title = id + " @ " + site;
		}
		TitleWithClose tabTitle = new TitleWithClose(title, users, component);
		component.setTabTitle(tabTitle);
		users.setTabComponentAt(idx, tabTitle);
		views.setSelectedIndex(2);
		users.setSelectedIndex(idx);
		component.openUser(site, id);
		component.doRetrieve();
	}
}
