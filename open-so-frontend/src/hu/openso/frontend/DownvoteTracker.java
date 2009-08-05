/*
 * Classname            : hu.openso.frontend.DownvoteTracker
 * Version information  : 1.0
 * Date                 : 2009.08.04.
 * Copyright notice     : GE Consumer & Industrial
 */

package hu.openso.frontend;

import java.awt.Container;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.GroupLayout.Alignment;
import javax.swing.RowSorter.SortKey;
import javax.swing.table.AbstractTableModel;

import org.htmlparser.util.ParserException;

/**
 * GUI for tracking potential downvoteds and downvoters
 * @author karnokd, 2009.08.04.
 * @version $Revision 1.0$
 */
public class DownvoteTracker extends JFrame {
	/** The serial version id. */
	private static final long serialVersionUID = 3094478885868144996L;
	/** Constant to sleep between page retrievals. */
	private static final int SLEEP_BETWEEN_PAGES = 1000;
	/**
	 * Record to store downvote target information.
	 * @author karnokd, 2009.08.04.
	 * @version $Revision 1.0$
	 */
	public static class DownvoteTarget {
		/** The destination site. */
		String site;
		/** The user identifier. */
		String id;
		/** The associated avatar URL. */
		String avatarUrl;
		/** The user display name. */
		String name;
		/** The reputation before. */
		int repBefore;
		/** The reputation after. */
		int repAfter;
		/** The question id where we saw this user, holds the most recent question activity. */ 
		final List<SummaryEntry> questionsBefore = new ArrayList<SummaryEntry>();
		/** The list of questions after the test. */
		final List<SummaryEntry> questionsAfter = new ArrayList<SummaryEntry>();
		/** The timestamp of the analysis. */
		long analysisTimestamp;
		/** Set to true if this user is detected as a receiver for the downvote. */
		boolean isReceiver;
		/** 
		 * Set to true if this user is detected as a giver for the downvote: 
		 * both can happen between two refreshes!
		 */
		boolean isGiver;
		/** Indicator that the user undestood this entry by clicking on it. */
		boolean understood;
	}
	class DownvoteModel extends AbstractTableModel {
		private static final long serialVersionUID = 3274337550983240673L;
		final List<DownvoteTarget> list = new ArrayList<DownvoteTarget>();
		String[] colNames = {
			"Timestamp", "Avatar", "Name", "Before", "After", "Diff", "Recvr", "Giver"	
		};
		Class<?>[] colClasses = {
			String.class, ImageIcon.class, String.class, Integer.class, Integer.class,
			Integer.class, ImageIcon.class, ImageIcon.class
		};
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return colClasses[columnIndex];
		}
		@Override
		public String getColumnName(int column) {
			return colNames[column];
		}
		@Override
		public int getColumnCount() {
			return colNames.length;
		}
		@Override
		public int getRowCount() {
			return list.size();
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			DownvoteTarget dt = list.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return sdf.format(new Timestamp(dt.analysisTimestamp));
			case 1:
				ImageIcon aicon = fctx.avatars.get(dt.avatarUrl);
				if (aicon != null) {
					return aicon;
				}
				if (!fctx.avatarsLoading.containsKey(dt.avatarUrl)) {
					fctx.avatarsLoading.put(dt.avatarUrl, dt.avatarUrl);
					doLoadAvatar(dt.avatarUrl);
				}
				return null;
			case 2:
				return dt.understood ? dt.name : "<html><font color='red'>" + dt.name;
			case 3:
				return dt.repBefore;
			case 4:
				return dt.repAfter;
			case 5:
				return dt.repAfter - dt.repBefore;
			case 6:
				return dt.isReceiver ? fctx.okay : null;
			case 7:
				return dt.isGiver ? fctx.okay : null;
			}
			return null;
		}
	}
	JTable table;
	DownvoteModel model;
	JPopupMenu popup;
	JLabel siteIcon;
	@SaveValue
	JComboBox sites;
	@SaveValue
	JTextField tags;
	@SaveValue
	JCheckBox refresh;
	int refreshTimeLimit = 60;
	int refreshTimeValue;
	Timer refreshTimer;
	FrontendContext fctx;
	JButton go;
	JButton clear;
	List<SummaryEntry> before = new ArrayList<SummaryEntry>();
	List<SummaryEntry> after = new ArrayList<SummaryEntry>();
	Map<String, DownvoteTarget> downvoteMemory = Collections.synchronizedMap(new HashMap<String, DownvoteTarget>());
	volatile int userMemorySize;
	final AtomicInteger activePageCount = new AtomicInteger(0);
	final AtomicInteger userPageCount = new AtomicInteger(0);
	final AtomicInteger newestPageCount = new AtomicInteger(0);
	final AtomicInteger hotPageCount = new AtomicInteger(0);
	@SaveValue
	JCheckBox useActive;
	@SaveValue
	JTextField useActivePages;
	@SaveValue
	JCheckBox useHot;
	@SaveValue
	JTextField useHotPages;
	@SaveValue
	JCheckBox useNewest;
	@SaveValue
	JTextField useNewestPages;
	@SaveValue
	JCheckBox useUserListings;
	@SaveValue
	JTextField useUserListingsStart;
	@SaveValue
	JTextField useUserListingsPages;
	@SaveValue
	JCheckBox useRepBars;
	/** Use the user info on the top 30 listings? */
	@SaveValue
	JCheckBox useTop;
	/** Optional Additinoal tags to narrow down users. */
	@SaveValue
	JTextField useTopTags;
	private JLabel statusLabel;
	/**
	 * Constructor. Initializes the panel.
	 * @param fctx
	 */
	public DownvoteTracker(FrontendContext fctx) {
		super("EDD");
		this.fctx = fctx;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				doClose();
			}
		});
		
		Container c = getContentPane();
		GroupLayout gl = new GroupLayout(c);
		c.setLayout(gl);
		gl.setAutoCreateContainerGaps(true);
		gl.setAutoCreateGaps(true);
		
		siteIcon = new JLabel();
		sites = new JComboBox(new String[] { "stackoverflow.com" , "meta.stackoverflow.com", "serverfault.com", "superuser.com"} );
		sites.setSelectedIndex(0);
		sites.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doSiteSelectionChanged();
			}
		});
		tags = new JTextField(20);
		refresh = new JCheckBox();
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doRefreshToggle();
			}
		});
		refreshTimer = new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doTimerTick();
			}
		});
		
		go = new JButton(fctx.go);
		go.setToolTipText("Perform the analysis now");
		go.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				doAnalysisSteps();
			}
		});
		
		clear = new JButton(fctx.clear);
		clear.setToolTipText("Clears the entire analysis table but leaves the 'after' list intact");
		clear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doClearTable();
			}
		});
		
		createPopupMenu();
		statusLabel = new JLabel();
		
		model = new DownvoteModel();
		table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setRowHeight(32);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setFont(statusLabel.getFont().deriveFont(16.0f));
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				doTableClicked(e);
			}
			@Override
			public void mousePressed(MouseEvent e) {
				doTablePopupClick(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				doTablePopupClick(e);
			}
		});
		table.getColumnModel().getColumn(0).setPreferredWidth(170);
		table.getColumnModel().getColumn(2).setPreferredWidth(250);
		table.getRowSorter().setSortKeys(Collections.singletonList(new SortKey(0, SortOrder.DESCENDING)));
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				doKeyPressed(e);
			}
		});
		JScrollPane sp = new JScrollPane(table);

		useActive = new JCheckBox("Active", true);
		useActivePages = new JTextField("4", 2);
		useHot = new JCheckBox("Hot", true);
		useHotPages = new JTextField("3", 2);
		useNewest = new JCheckBox("Newest", true);
		useNewestPages = new JTextField("4", 2);
		useUserListings = new JCheckBox("Users", true);
		useUserListingsStart = new JTextField("1", 3);
		useUserListingsPages = new JTextField("6", 3);
		useRepBars = new JCheckBox("Rep-bars", true);
		useTop = new JCheckBox("Use Top30", true);
		useTopTags = new JTextField(15);
		
		gl.setHorizontalGroup(
			gl.createParallelGroup()
			.addGroup(
				gl.createSequentialGroup()
				.addComponent(siteIcon, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(sites, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(tags, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(go)
				.addComponent(clear)
				.addComponent(refresh, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			)
			.addGroup(
				gl.createSequentialGroup()
				.addComponent(useActive, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(useActivePages, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(useHot, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(useHotPages, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(useNewest, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(useNewestPages, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(useUserListings, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(useUserListingsStart, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(useUserListingsPages, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(useRepBars, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(useTop, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(useTopTags, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			)
			.addComponent(sp)
			.addComponent(statusLabel)
		);
		gl.setVerticalGroup(
			gl.createSequentialGroup()
			.addGroup(
				gl.createParallelGroup(Alignment.BASELINE)
				.addComponent(siteIcon)
				.addComponent(sites)
				.addComponent(tags)
				.addComponent(go, 25, 25, 25)
				.addComponent(clear)
				.addComponent(refresh)
			)
			.addGroup(
				gl.createParallelGroup(Alignment.BASELINE)
				.addComponent(useActive)
				.addComponent(useActivePages)
				.addComponent(useHot)
				.addComponent(useHotPages)
				.addComponent(useNewest)
				.addComponent(useNewestPages)
				.addComponent(useUserListings)
				.addComponent(useUserListingsStart)
				.addComponent(useUserListingsPages)
				.addComponent(useRepBars)
				.addComponent(useTop)
				.addComponent(useTopTags)
			)
			.addComponent(sp)
			.addComponent(statusLabel)
		);
		
		gl.linkSize(SwingUtilities.VERTICAL, siteIcon, sites, tags, refresh, go, clear);
		
		refreshTimeValue = refreshTimeLimit;
		setRefreshLabel();
		updateStatusLabel();
		doSiteSelectionChanged();
		pack();
	}
	/**
	 * Delete the selected entry
	 * @param e
	 */
	protected void doKeyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_DELETE) {
			int idx = table.getSelectedRow();
			doRemoveEntry();
			if (idx < table.getRowCount()) {
				table.getSelectionModel().setSelectionInterval(idx, idx);
			} else {
				table.getSelectionModel().setSelectionInterval(idx - 1, idx - 1);
			}
		}
	}
	/**
	 * 
	 */
	protected void doTableClicked(MouseEvent e) {
		DownvoteTarget dt = getSelectedItem();
		if (dt != null) {
			dt.understood = true;
			int idx = getSelectedIndex();
			model.fireTableRowsUpdated(idx, idx);
			setWindowTitle();
		}
		if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() > 1) {
			doOpenUserRecent();
		}
			
	}
	/**
	 * Sets the window title based on the unread amount.
	 */
	private void setWindowTitle() {
		setTitle(String.format("EDD: %s (%d)", sites.getSelectedItem(), getUnreadCount()));
	}
	void doTablePopupClick(MouseEvent e) {
		if (e.isPopupTrigger()) {
			int i = table.rowAtPoint(e.getPoint());
			if (i >= 0 && i < table.getRowCount()) {
				table.getSelectionModel().setSelectionInterval(i, i);
			}
			popup.show(e.getComponent(), e.getX(), e.getY());
		}
	}
	/** Update the status label. */
	protected void updateStatusLabel() {
		statusLabel.setText(String.format("Entries: %d | Before count: %d "
				+ "| After count: %d | User memory: %d "
				+ "| Current active page %d | Current user page %d | Current newest page %d "
				+ "| Current hottest page %d"
				, model.list.size(), before.size(), after.size(), userMemorySize
				, activePageCount.get(), userPageCount.get(), newestPageCount.get()
				, hotPageCount.get()
		));
		doSiteSelectionChanged();
	}
	/**
	 * @param avatarUrl
	 */
	public void doLoadAvatar(final String url) {
		fctx.exec.submit(new Runnable() {
			@Override
			public void run() {
				try {
					ImageIcon icon = new ImageIcon(ImageIO.read(new URL(url)));
					fctx.avatars.put(url, icon);
					fctx.avatarsLoading.remove(url);
					model.fireTableRowsUpdated(0, model.list.size() - 1);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		});
	}
	/**
	 * 
	 */
	protected void doClearTable() {
		model.list.clear();
		model.fireTableDataChanged();
		updateStatusLabel();
	}
	public DownvoteTarget getSelectedItem() {
		int idx = getSelectedIndex();
		if (idx >= 0) {
			return model.list.get(idx);
		}
		return null;
	}
	public int getSelectedIndex() {
		int idx = table.getSelectedRow();
		if (idx >= 0) {
			return table.convertRowIndexToModel(idx);
		}
		return -1;
	}
	/**
	 * 
	 */
	protected void doSiteSelectionChanged() {
		if (sites.getSelectedIndex() >= 0) {
			siteIcon.setIcon(fctx.siteIcons.get(sites.getSelectedItem()));
			setIconImage(fctx.siteIcons.get(sites.getSelectedItem()).getImage());
			setWindowTitle();
		}
	}
	/**
	 * @return the number of unread entries in the list
	 */
	private int getUnreadCount() {
		int sum = 0;
		for (DownvoteTarget dt : model.list) {
			if (!dt.understood) {
				sum++;
			}
		}
		return sum;
	}
	/**
	 * 
	 */
	private void createPopupMenu() {
		popup = new JPopupMenu();
		
		JMenuItem openUser = new JMenuItem("Open user");
		openUser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doOpenUser();
			}
		});
		JMenuItem openUserHere = new JMenuItem("Open user here");
		openUserHere.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doOpenUserHere();
			}
		});
		JMenuItem remove = new JMenuItem("Remove entry");
		remove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doRemoveEntry();
			}
		});
		JMenuItem autoSize = new JMenuItem("Auto resize columns");
		autoSize.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doAutoSize();
			}
		});

		JMenuItem copyName = new JMenuItem("Copy name");
		copyName.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doCopyName();
			}
		});
		JMenuItem openUserRecent = new JMenuItem("Open user recent");
		openUserRecent.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doOpenUserRecent();
			}
		});
		
		popup.add(openUser);
		popup.add(openUserRecent);
		popup.add(openUserHere);
		popup.addSeparator();
		popup.add(copyName);
		popup.addSeparator();
		popup.add(remove);
		popup.addSeparator();
		popup.add(autoSize);
	}
	/**
	 * 
	 */
	protected void doOpenUserRecent() {
		DownvoteTarget dt = getSelectedItem();
		if (dt != null) {
			Desktop d = Desktop.getDesktop();
			try {
				String siteStr = (String)sites.getSelectedItem();
				d.browse(new URI("http://" + siteStr + "/users/" + dt.id + "?tab=recent#sort-top"));
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (URISyntaxException ex) {
				ex.printStackTrace();
			}
		}
	}
	/**
	 * 
	 */
	protected void doCopyName() {
		DownvoteTarget dt = getSelectedItem();
		if (dt != null) {
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(dt.name), new ClipboardOwner() {
				@Override
				public void lostOwnership(Clipboard clipboard, Transferable contents) {
					// nothing
				}
			});		
		}
	}
	/**
	 * 
	 */
	protected void doAutoSize() {
		GUIUtils.autoResizeColWidth(table, model);
	}
	/**
	 * 
	 */
	protected void doRemoveEntry() {
		int idx = getSelectedIndex();
		if (idx >= 0) {
			model.list.remove(idx);
			model.fireTableRowsDeleted(idx, idx);
			updateStatusLabel();
		}
	}
	/**
	 * 
	 */
	protected void doOpenUserHere() {
		DownvoteTarget dt = getSelectedItem();
		if (dt != null) {
			String siteStr = (String)sites.getSelectedItem();
			fctx.panelManager.openUser(new String[] { siteStr }, new String[] { dt.id }, dt.name);
		}
	}
	/**
	 * 
	 */
	protected void doOpenUser() {
		DownvoteTarget dt = getSelectedItem();
		if (dt != null) {
			Desktop d = Desktop.getDesktop();
			try {
				String siteStr = (String)sites.getSelectedItem();
				d.browse(new URI("http://" + siteStr + "/users/" + dt.id));
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (URISyntaxException ex) {
				ex.printStackTrace();
			}
		}
	}
	/**
	 * 
	 */
	protected void doRefreshToggle() {
		if (refresh.isSelected()) {
			refreshTimer.start();
		} else {
			refreshTimer.stop();
		}
			
	}
	/**
	 * Perform actions on each timer tick: count down next refresh time.
	 */
	protected void doTimerTick() {
		if (refreshTimeValue > 0) {
			refreshTimeValue--;
			setRefreshLabel();
		}
		if (refreshTimeValue <= 0) {
			doAnalysisSteps();
		}
	}
	/**
	 * Perform the differential reputation analysis on the given site settings.
	 */
	protected void doAnalysisSteps() {
		refreshTimer.stop();
		go.setIcon(fctx.rolling);
		go.setEnabled(false);
		before = after; // swap
		final int beforeSize = before.size() + 50;
		after = new ArrayList<SummaryEntry>(beforeSize);
		final String siteStr = (String)sites.getSelectedItem();
		final String tagStr = tags.getText();
		
		// get search parameters into finals
		final boolean useActiveFlag = useActive.isSelected();
		final int useActivePageCount = Integer.parseInt(useActivePages.getText());

		final boolean useNewestFlag = useNewest.isSelected();
		final int useNewestPageCount = Integer.parseInt(useNewestPages.getText());

		final boolean useHotFlag = useHot.isSelected();
		final int useHotPageCount = Integer.parseInt(useHotPages.getText());
		
		final boolean useUsersFlag = useUserListings.isSelected();
		final int userPageStartValue = Integer.parseInt(useUserListingsStart.getText());
		final int userPageCountValue = Integer.parseInt(useUserListingsPages.getText());
		
		final boolean useTop30Flag = useTop.isSelected();
//		final String[] useTop30Tags = useTopTags.getText().split("\\s+");
		
		final boolean useRepBarsFlag = useRepBars.isSelected();
		
		GUIUtils.getWorker(new WorkItem() {
			List<DownvoteTarget> targets;
			private List<SummaryEntry> out;
			@Override
			public void run() {
				try {
					out = new ArrayList<SummaryEntry>(beforeSize);
					List<Future<List<SummaryEntry>>> parallels = new ArrayList<Future<List<SummaryEntry>>>();
					if (useActiveFlag) {
						parallels.add(loadSummaryEntries(siteStr, "active",
							tagStr.isEmpty() ? null : tagStr, useActivePageCount, activePageCount, 1));
					}
					if (useNewestFlag) {
						parallels.add(loadSummaryEntries(siteStr, "newest",
								tagStr.isEmpty() ? null : tagStr, useNewestPageCount, newestPageCount, 1)); 
					}
					if (useHotFlag) {
						parallels.add(loadSummaryEntries(siteStr, "hot",
								tagStr.isEmpty() ? null : tagStr, useHotPageCount, hotPageCount, 1)); 
					}
					if (useUsersFlag) {
						parallels.add(getTopUsers(siteStr, userPageStartValue, userPageCountValue));
					}
					if (useTop30Flag) {
						
					}
					if  (useRepBarsFlag) {
						parallels.add(getRepBarValues(siteStr));
					}
					
					for (Future<List<SummaryEntry>> f : parallels) {
						out.addAll(f.get());
					}
					targets = checkForDownvotes(before, out, false);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			} 
			
			@Override
			public void done() {
				after.addAll(out);
				model.list.addAll(targets);
				model.fireTableDataChanged();
				go.setIcon(fctx.go);
				go.setEnabled(true);
				updateStatusLabel();
				if (refresh.isSelected()) {
					refreshTimeValue = refreshTimeLimit;
					setRefreshLabel();
					refreshTimer.start();
				}
			}
		}).execute();
	}
	/** 
	 * Returns a future to retrieve current values from reputation bars floating around in the application
	 * @return the future
	 */
	protected Future<List<SummaryEntry>> getRepBarValues(final String site) {
		return fctx.exec.submit(new Callable<List<SummaryEntry>>() {
			final List<SummaryEntry> result = new ArrayList<SummaryEntry>();
			@Override
			public List<SummaryEntry> call() throws Exception {
				SwingUtilities.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						doInEDT();
					}
				});
				return result;
			}
			/** Perform this operation in the EDT. */
			protected void doInEDT() {
				for (ReputationFloat repFloat : fctx.panelManager.getRegisteredReputationFloats()) {
					for (UserProfile up : repFloat.repPanel.userProfiles) {
						if (up.site.equals(site)) {
							SummaryEntry se = new SummaryEntry();
							se.userId = up.id;
							se.userName = up.name;
							se.userRep = up.reputation;
							se.avatarUrl = up.avatarUrl;
							se.site = site;
							result.add(se);
						}
					}
					
				}
				
			}
		});
	}
	/**
	 * Returns the top users from the users listings.
	 * @param site the site
	 * @param out the output summary entry faked
	 * @throws IOException
	 * @throws ParserException
	 */
	protected Future<List<SummaryEntry>> getTopUsers(final String site, final int startPage, final int pageCount) {
		userPageCount.set(0);
		updateStatusLabel();
		return fctx.exec.submit(
			new Callable<List<SummaryEntry>>() {
				@Override
				public List<SummaryEntry> call() throws Exception {
					List<SummaryEntry> out = new ArrayList<SummaryEntry>();
					for (int i = 0; i < pageCount; i++) {
						userPageCount.set(i);
						updateStatusLabelEDT();
						byte[] data = SOPageParsers.getUsers("http://" + site, startPage + i);
						for (BasicUserInfo bui : SOPageParsers.parseUsers(data)) {
							SummaryEntry se = new SummaryEntry();
							se.userId = bui.id;
							se.userName = bui.name;
							se.userRep = bui.reputation;
							se.avatarUrl = bui.avatarUrl;
							se.site = site;
							out.add(se);
						}
						if (i < pageCount - 1) {
							try {
								TimeUnit.MILLISECONDS.sleep(SLEEP_BETWEEN_PAGES);
							} catch (InterruptedException ex) {
								break;
							}
						}
					}
					return out;
				}	
			}
		);
	}
	protected void updateStatusLabelEDT() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				updateStatusLabel();
			}
		});
	}
	/**
	 * Load the summary entries of 'active' for the given site and optional tags.
	 * @param site the site without http://
	 * @param tags the optional filter tags
	 * @param out the output list to fill in
	 * @throws IOException if an error occurs
	 * @throws ParserException if an error occurs
	 */
	public Future<List<SummaryEntry>> loadSummaryEntries(final String site, 
			final String type, final String tags, final int maxPage, 
			final AtomicInteger counter, final int pageStart) {
		counter.set(0);
		updateStatusLabel();
		return fctx.exec.submit(
			new Callable<List<SummaryEntry>>() {
				@Override
				public List<SummaryEntry> call() throws Exception {
					List<SummaryEntry> out = new ArrayList<SummaryEntry>();
					for (int i = 0; i < maxPage; i++) {
						counter.set(i);
						updateStatusLabelEDT();
						byte[] data = SOPageParsers.getQuestionsData("http://" + site, tags, type, pageStart + i, 50);
						out.addAll(SOPageParsers.processMainPage(data));
						if (i < maxPage - 1) {
							try {
								TimeUnit.MILLISECONDS.sleep(SLEEP_BETWEEN_PAGES);
							} catch (InterruptedException ex) {
								break;
							}
						}
					}
					return out;
				}
			});
	}
	protected void setRefreshLabel() {
		refresh.setText(String.format("Refresh (in %ds)", refreshTimeValue));
	}
	public void doClose() {
		refreshTimer.stop();
	}
	/**
	 * A differentiator algorithm to check for user activity differences based on
	 * their public listed reputation value
	 * @param before the list of questions before
	 * @param after the list of questions after
	 * @param debug print findings instantly
	 */
	public List<DownvoteTarget> checkForDownvotes(List<SummaryEntry> before, 
			List<SummaryEntry> after, boolean debug) {
		long analysisTimestamp = System.currentTimeMillis();
		Map<String, DownvoteTarget> users = new HashMap<String, DownvoteTarget>();
		for (SummaryEntry b : before) {
			String uid = b.userId;
			DownvoteTarget target = users.get(uid);
			if (target == null) {
				target = new DownvoteTarget();
				users.put(uid, target);
				target.id = uid;
				target.site = b.site;
				target.name = b.userName;
				target.repBefore = b.userRep;
				target.avatarUrl = b.avatarUrl;
				target.analysisTimestamp = analysisTimestamp;
			} else {
				// if the user page contains a more accurate value.
				target.repBefore = b.userRep;
			}
			
			target.questionsBefore.add(b);
			downvoteMemory.put(uid, target);
		}
		for (SummaryEntry a : after) {
			String uid = a.userId;
			DownvoteTarget target = users.get(uid);
			// if it was in the previous query, only then do we work with it
			if (target != null) {
				target.repAfter = a.userRep;
				target.questionsAfter.add(a);
			}
		}
		int[] diffDownvoteGiver = { -1, -3, -5, -7, 9, 7, 5, 3, 19, 17, 13,  29, 27, 23, 39, 37, 33, 49, 47, 43  };
		int[] diffDownvoteReceiver = { -2, -4, -6, -8, 8, 6, 4, 18, 16, 14, 12, 28, 26, 24, 22, 38, 36, 34, 32, 48, 46, 44, 42 };
		// filter those records from users who did not appear after - no diff there
		for (DownvoteTarget dt : new ArrayList<DownvoteTarget>(users.values())) {
			if (dt.repAfter == 0) {
				users.remove(dt.id);
				// try to remember further
				dt = downvoteMemory.get(dt.id);
				// if never seen, continue
				if (dt == null) {
					continue;
				}
			}
			downvoteMemory.put(dt.id, dt);
			int diff = dt.repAfter - dt.repBefore;
			if (diff == 0) {
				users.remove(dt.id);
				continue;
			}
			// check if the there was an odd/even transition
			if (Math.abs(diff) % 2 == 1) {
				for (int g : diffDownvoteGiver) {
					if (diff == g) {
						dt.isGiver = true;
						if (debug) {
							System.out.printf("GIVER: %s (%s) %d -> %d (%d)%n", dt.name, dt.id, dt.repAfter, dt.repBefore, diff);
						}
						break;
					}
				}
				for (int r : diffDownvoteReceiver) {
					if (diff == r) {
						dt.isReceiver = true;
						if (debug) {
							System.out.printf("RECVR: %s (%s) %d -> %d (%d)%n", dt.name, dt.id, dt.repAfter, dt.repBefore, diff);
						}
						break;
					}
				}
			}
			// now retry for the even changes
			if (!dt.isGiver && !dt.isReceiver) {
				for (int g : diffDownvoteGiver) {
					if (diff == g) {
						dt.isGiver = true;
						if (debug) {
							System.out.printf("GIVER*: %s (%s) %d -> %d (%d)%n", dt.name, dt.id, dt.repAfter, dt.repBefore, diff);
						}
						break;
					}
				}
				for (int r : diffDownvoteReceiver) {
					if (diff == r) {
						dt.isReceiver = true;
						if (debug) {
							System.out.printf("RECVR*: %s (%s) %d -> %d (%d)%n", dt.name, dt.id, dt.repAfter, dt.repBefore, diff);
						}
						break;
					}
				}
			}
		}
		userMemorySize = downvoteMemory.size();
		return new ArrayList<DownvoteTarget>(users.values());
	}
//	public static void main(String[] args) throws Exception {
//		List<SummaryEntry> before = new ArrayList<SummaryEntry>();
//		loadSummaryEntries("stackoverflow.com", null, before);
//		System.out.printf("Sleep 1 minute%n");
//		TimeUnit.MINUTES.sleep(1);
//		List<SummaryEntry> after = new ArrayList<SummaryEntry>();
//		loadSummaryEntries("stackoverflow.com", null, after);
//		System.out.println("---------- Analysis ----------");
//		checkForDownvotes(before, after, true);
//		System.out.println("------------ Done ------------");
//		
//	}
}
