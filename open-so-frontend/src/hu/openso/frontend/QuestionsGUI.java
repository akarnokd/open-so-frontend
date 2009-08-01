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
	static final String version = "0.57";
	
	Map<String, ImageIcon> avatars = new ConcurrentHashMap<String, ImageIcon>();
	Map<String, ImageIcon> avatarsLoading = new ConcurrentHashMap<String, ImageIcon>();
	Map<String, ImageIcon> siteIcons = new HashMap<String, ImageIcon>();
	ExecutorService exec = Executors.newCachedThreadPool();
	/** The global ignore table for site/id. */
	Map<String, String> globalIgnores = new LinkedHashMap<String, String>();
	JTabbedPane tabs;
	JPanel EMPTY_PANEL = new JPanel();
	boolean disableTabChange;
	IgnoreListGUI globalIgnoreListGUI;
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
		tabs = new JTabbedPane();
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(tabs, BorderLayout.CENTER);
		
		QuestionPanel p = new QuestionPanel(avatars, avatarsLoading, siteIcons, exec, globalIgnores, globalIgnoreListGUI);
		tabs.insertTab("Questions " + (tabs.getTabCount() + 1), null, p, null, tabs.getTabCount());
		TitleWithClose component = new TitleWithClose("Questions " + (tabs.getTabCount()), tabs, p);
		p.setTabTitle(component);
		
		tabs.setTabComponentAt(0, component);
		
		tabs.addTab("+", null, EMPTY_PANEL, "Open new tab");
		tabs.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (!disableTabChange) {
					doTabClicked();
				}
			}
		});
		
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
	protected void doTabClicked() {
		if (tabs.getSelectedComponent() == EMPTY_PANEL) {
			disableTabChange = true;
			QuestionPanel component = new QuestionPanel(avatars, avatarsLoading, siteIcons, exec, globalIgnores, globalIgnoreListGUI);
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
							QuestionPanel component = new QuestionPanel(avatars, avatarsLoading, siteIcons, exec, globalIgnores, globalIgnoreListGUI);
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
				String ignoreCnt = p.getProperty("IgnoreCount");
				if (ignoreCnt != null) {
					int ic = Integer.parseInt(ignoreCnt);
					for (int i = 0; i < ic; i++) {
						globalIgnores.put(p.getProperty("Ignore" + i), p.getProperty("IgnoreTitle" + i));
					}
				}
			} finally {
				in.close();
			}
		} catch (IOException ex) {
			// ignore
		}
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
			p.setProperty("IgnoreCount", Integer.toString(globalIgnores.size()));
			int i = 0;
			for (Map.Entry<String, String> e: globalIgnores.entrySet()) {
				p.setProperty("Ignore" + i, e.getKey());
				p.setProperty("IgnoreTitle" + i, e.getValue());
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
