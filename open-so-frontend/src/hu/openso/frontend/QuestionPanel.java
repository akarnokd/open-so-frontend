package hu.openso.frontend;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.DefaultRowSorter;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.commons.codec.binary.Base64;

public class QuestionPanel extends JPanel {
	private static final long serialVersionUID = 2165339317109256363L;
	JTable table;
	QuestionModel model;
	JButton go;
	@SaveValue
	JTextField page;
	@SaveValue
	JTextField pageSize;
	@SaveValue
	JComboBox sort;
	JButton more;
	JButton clear;
	boolean adjusted;
	JPopupMenu menu;
	@SaveValue
	JCheckBox merge;
	JLabel[] status;
	@SaveValue
	JCheckBox excerpts;
	@SaveValue
	JCheckBox refresh;
	Timer refreshTimer;
	int refreshCounter;
	static final int REFRESH_TIME = 30;
	@SaveValue
	JCheckBox[] siteUrls;
	@SaveValue
	JTextField[] tags;
	JLabel[] siteIconLabels;
	AtomicInteger retrieveWip = new AtomicInteger(0);
	JLabel totalLabel;
	/** Label for background task running. */
	JLabel wikiBackgroundTask;
	JButton markAsRead;
	boolean readCheck = true;
	IgnoreListGUI ignoreListGUI;
	/** Set of site name + question id to ignore to the title. */
	
	private TitleWithClose tabTitle;
	/** The question context object. */
	private final FrontendContext qctx;
	/** The map of local ignores. */
	Map<String, String> ignores = new LinkedHashMap<String, String>();
	@SaveValue
	private JTextField findValue;
	private SwingWorker<Void, Void> wikiSwingWorker;
	private JPopupMenu wikiTestMenu;
	@SaveValue
	JCheckBoxMenuItem detailUnread;
	/**
	 * The question listings table model.
	 * @author karnokd, 2009.08.04.
	 * @version $Revision 1.0$
	 */
	public class QuestionModel extends AbstractTableModel {
		private static final long serialVersionUID = -898209429130786969L;
		List<SummaryEntry> questions = new ArrayList<SummaryEntry>();
		Class<?>[] columnClasses = {
			ImageIcon.class, ImageIcon.class, ImageIcon.class, String.class,
			Integer.class, Integer.class, Integer.class, String.class, String.class, 
			ImageIcon.class, String.class, Integer.class, String.class,
			String.class, Integer.class
		};
		String[] columnNames = {
			"S", "A", "W", "B",
			"Votes", "Answers", "Views", "Question", "Time",
			"Avatar", "User", "Rep", "Badges", 
			"Tags", "Fav"
		};
		@Override
		public int getColumnCount() {
			return columnNames.length;
		}
		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnClasses[columnIndex];
		}
		@Override
		public int getRowCount() {
			return questions.size();
		}
		SimpleDateFormat sdf = new SimpleDateFormat("'<html>'yyyy-MM-dd'<br>'HH:mm:ss");
		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex) {
			SummaryEntry se = questions.get(rowIndex);
			switch (columnIndex) {
			case 0: return qctx.siteIcons.get(se.site);
			case 1: return se.accepted ? qctx.okay : (se.deleted ? qctx.error : null);
			case 2:
				Boolean isWiki = se.wiki;
				Boolean isKnown = qctx.knownWikis.get(se.site + "/" + se.id);
				if ((isWiki != null && isWiki.booleanValue()) 
						|| (isKnown != null && isKnown.booleanValue())) {
					return qctx.wiki;
				} else
				if (isWiki == null && isKnown == null) {
					return qctx.unknown;
				}
				return null;
			case 3: 
				int b = se.bounty;
				if (b > 0) {
					return "<html><font style='background-color: #0077DD; color: white;'>&nbsp;+" + b + "&nbsp;</font></html>";
				}
				return null;
			case 4: return se.votes;
			case 5: return se.answers;
			case 6: return se.views;
			case 7:
				return se.title;
			case 8: return sdf.format(new Timestamp(se.time));
			case 9:
				final String url = se.avatarUrl;
				if (url != null) {
					ImageIcon icon = qctx.avatars.get(url);
					if (icon == null) {
						if (!qctx.avatarsLoading.containsKey(url)) {
							qctx.avatarsLoading.put(url, url);
							loadImageFor(rowIndex, columnIndex, url);
						}
					}
					return icon;
				}
				return null;
			case 10: return se.userName;
			case 11: return se.userRep;
			case 12: 
				StringBuilder sb = new StringBuilder();
				sb.append("<html>");
				if (se.goldBadges > 0) {
					sb.append("<font color='#FFCC00'>&#9679;</font>").append(pad(se.goldBadges)).append(se.goldBadges).append(" ");
				}
				if (se.silverBadges > 0) {
					sb.append("<font color='#C0C0C0'>&#9679;</font>").append(pad(se.silverBadges)).append(se.silverBadges).append(" ");
				}
				if (se.bronzeBadges > 0) {
					sb.append("<font color='#CC9966'>&#9679;</font>").append(pad(se.bronzeBadges)).append(se.bronzeBadges).append(" ");
				}
				return sb.toString();
			case 13: 
				StringBuilder tgb = new StringBuilder();
				tgb.append("<html>");
				for (String s : se.tags) {
					if (tgb.length() > 6) {
						tgb.append(", ");
					}
					tgb.append(s);
				}
				tgb.append("<br>&nbsp;");
				return tgb.toString();
			case 14:
				return se.favored;
			}
			return null;
		}
		private void loadImageFor(final int rowIndex, final int columnIndex,
				final String url) {
			qctx.exec.submit(new Runnable() {
				@Override
				public void run() {
					try {
//						System.out.printf("Getting avatar (%d): %s%n", rowIndex, url);
						ImageIcon icon = new ImageIcon(ImageIO.read(new URL(url)));
						qctx.avatars.put(url, icon);
						qctx.avatarsLoading.remove(url);
						doRefreshTable(rowIndex, columnIndex);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			});
		}
		
	}
	class CustomCellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = -1187003361272971515L;
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			se = model.questions.get(table.convertRowIndexToModel(row));
			JLabel lbl = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setText("");
			setToolTipText(se.title);
			return lbl;
		}
		@Override
		public void paint(Graphics g) {
			super.paint(g);
			Graphics2D g2 = (Graphics2D)g;
			if (se != null) {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Font f0 = g2.getFont();
				g2.setFont(getFont().deriveFont(Font.BOLD, 16));
				Color c0 = g2.getBackground();
				FontMetrics fm0 = g2.getFontMetrics(); 
				int h = fm0.getHeight();
				if (!se.markRead) {
					g2.setColor(new Color(0xFFFFE0E0));
					g2.fillRect(0, 1 + fm0.getDescent(), getWidth(), h);
				}
				g2.setColor(Color.BLACK);
				if (se.site.equals("stackoverflow.com")) {
					g2.setColor(new Color(0x0077CC));
				} else
				if (se.site.equals("meta.stackoverflow.com")) {
					g2.setColor(new Color(0x3D3D3D));
				} else
				if (se.site.equals("serverfault.com")) {
					g2.setColor(new Color(0x10456A));
				} else
				if (se.site.equals("superuser.com")) {
					g2.setColor(new Color(0x1086A4));
				}
				int ellw = fm0.stringWidth("\u2026");
				int titleWidth = fm0.stringWidth(se.title);
				if (titleWidth > getWidth()) {
					for (int i = se.title.length() - 1; i > 0; i--) {
						String tstr = se.title.substring(0, i);
						int tstrw = fm0.stringWidth(tstr);
						if (tstrw + ellw < getWidth()) {
							g2.drawString(tstr, 2, h);
							g2.drawString("\u2026", 2 + tstrw, h);
							break;
						}
					}
				} else {
					g2.drawString(se.title, 2, h);
				}
				// -----------------------------------
				if (excerpts.isSelected()) {
					g2.setFont(f0);
					g2.setColor(Color.BLACK);
					g2.setBackground(c0);
					int h2 = g2.getFontMetrics().getHeight();
					
					int w = getWidth();
					String[] split = se.excerpt.trim().split("\\s+");
					int y = h + h2;
					int x = 2;
					FontMetrics fm = g2.getFontMetrics();
					int spw = fm.stringWidth(" ");
					for (String s : split) {
						int tw = fm.stringWidth(s);
						if (x + tw > w) {
							y += h2;
							x = 2;
						}
						g2.drawString(s, x, y);
						x += tw + spw;
					}
				}
			}
		}
		SummaryEntry se;
	}
	String pad(int value) {
		StringBuilder result = new StringBuilder();
		if (value < 100) {
			result.append("&nbsp;");
		}
		if (value < 10) {
			result.append("&nbsp;");
		}
		return result.toString();
	}
	void doRefreshTable(final int row, final int col) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (model.questions.size() > row) {
					model.fireTableCellUpdated(row, col);
				}
			}
		});
	}
	public QuestionPanel(FrontendContext qcontext) {
		this.qctx = qcontext;
		init();
		
	}
	private void init() {
		
		GroupLayout gl = new GroupLayout(this);
		setLayout(gl);
		gl.setAutoCreateContainerGaps(true);
		gl.setAutoCreateGaps(true);
		
		sort = new JComboBox(new String[] { 
			"newest-Q", "featured-Q", "hot-Q", "votes-Q", "active-Q", // on the questions page
			"newest-U", "votes-U" }); // on the unanswered page		
		sort.setSelectedItem("active-Q");
		model = new QuestionModel();
		table = new JTable(model);
		table.setRowHeight(32);
		table.setAutoCreateRowSorter(true);
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
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				doQuestionClick();
			}
		});
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				doKeyPressed(e);
			}
		});
		table.getColumnModel().getColumn(7).setCellRenderer(new CustomCellRenderer());
		JScrollPane scroll = new JScrollPane(table);
		scroll.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				doTablePopupClick(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				doTablePopupClick(e);
			}
		});
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getTableHeader().setReorderingAllowed(true);
		
		go = new JButton(qctx.go);
		go.setToolTipText("Read the first page of the selected sites and subpages");
		ActionListener doRetrieveAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				page.setText("1");
				doRetrieve(1, getPageSize());
			}
		};
		go.addActionListener(doRetrieveAction);
		siteUrls = new JCheckBox[] {
			new JCheckBox("http://stackoverflow.com", true),
			new JCheckBox("http://meta.stackoverflow.com", false),
			new JCheckBox("http://serverfault.com", false),
			new JCheckBox("http://superuser.com", false),
		};
		siteIconLabels  = new JLabel[] {
			new JLabel(qctx.siteIcons.get("stackoverflow.com")),	
			new JLabel(qctx.siteIcons.get("meta.stackoverflow.com")),	
			new JLabel(qctx.siteIcons.get("serverfault.com")),	
			new JLabel(qctx.siteIcons.get("superuser.com")),	
		};
		tags = new JTextField[] {
			new JTextField(15),
			new JTextField(15),
			new JTextField(15),
			new JTextField(15),
		};
		totalLabel = new JLabel("Welcome to Open Stack Overflow Frontend");
		status = new JLabel[] {
			new JLabel("", siteIconLabels[0].getIcon(), JLabel.LEFT), 
			new JLabel("", siteIconLabels[1].getIcon(), JLabel.LEFT), 
			new JLabel("", siteIconLabels[2].getIcon(), JLabel.LEFT),
			new JLabel("", siteIconLabels[3].getIcon(), JLabel.LEFT),
		};
		
		siteIconLabels[0].setToolTipText("Open Stack Overflow in browser");
		siteIconLabels[1].setToolTipText("Open Meta Stack Overflow in browser");
		siteIconLabels[2].setToolTipText("Open Server Fault in browser");
		siteIconLabels[3].setToolTipText("Open Super User in browser");
		for (int i = 0; i < status.length; i++) {
			final int j = i;
			siteIconLabels[i].addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1) {
						doOpenSite(siteUrls[j].getText());
					}
				}
			});
			siteIconLabels[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			status[i].setVisible(false);
		}
		
		wikiBackgroundTask = new JLabel();
		wikiBackgroundTask.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				doWikiBackgroundPopup(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				doWikiBackgroundPopup(e);
			}
		});
		
		page = new JFormattedTextField(1);
		page.setColumns(2);
		page.setToolTipText("The current page index");
		
		pageSize = new JFormattedTextField(15);
		pageSize.setColumns(3);
		pageSize.setToolTipText("The page size length between 1 and 50");
		
		more = new JButton(qctx.more);
		more.setToolTipText("Read the Nth page of the selected sites and subpages");
		more.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doGetMore();
			}
		});
		clear = new JButton(qctx.clear);
		clear.setToolTipText("Clears the entire list of questions");
		clear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doClearListing();
			}
		});
		
		JButton downVoter = new JButton(qctx.downvote);
		downVoter.setToolTipText("Experimental: Start new downvote detector");
		downVoter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doStartDownvoteDetector();
			}
		});
		
		merge = new JCheckBox("Merge");
		merge.setSelected(true);
		
		excerpts = new JCheckBox("Excerpts");
		excerpts.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doExcerptToggle();
			};
		});
		refresh = new JCheckBox();
		refreshCounter = REFRESH_TIME;
		setRefreshLabel();
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doRefreshToggle();
			}
		});
		refreshTimer = new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doRefreshCounter();
			}
		});
		markAsRead = new JButton("All read");
		markAsRead.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doMarkAsRead();
			}
		});
		
		JLabel findLabel = new JLabel("Find:");
		
		ActionListener findNextAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doFindNext();
			}
		};
		findValue = new JTextField(15);
		findValue.setToolTipText("<html>Find in title, excerpt, tags and user name, <br>space separates keyword fragments, ENTER to find next");
		findValue.addActionListener(findNextAction);
		JButton findPrev = new JButton("\u2190");
		findPrev.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doFindPrevious();
			}
		});
		findPrev.setToolTipText("Find backward");
		JButton findNext = new JButton("\u2192");
		findNext.addActionListener(findNextAction);
		findNext.setToolTipText("Find forward");
		
		SequentialGroup sg = gl.createSequentialGroup();
		for (int i = 0; i < siteUrls.length / 2; i++) {
			sg.addComponent(siteIconLabels[i], GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
			sg.addComponent(siteUrls[i], GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
			sg.addComponent(tags[i], 20, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
		}
		SequentialGroup sg1 = gl.createSequentialGroup();
		for (int i = siteUrls.length / 2; i < siteUrls.length; i++) {
			sg1.addComponent(siteIconLabels[i], GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
			sg1.addComponent(siteUrls[i], GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
			sg1.addComponent(tags[i], 20, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
		}
		
		SequentialGroup sg2 = gl.createSequentialGroup();
		sg2.addComponent(totalLabel);
		for (int i = 0; i < siteUrls.length; i++) {
			sg2.addComponent(status[i]);
		}		
		sg2
		.addComponent(wikiBackgroundTask)
		.addGap(1, 10, Short.MAX_VALUE)
		.addComponent(findLabel)
		.addComponent(findValue, 20, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
		.addComponent(findPrev)
		.addComponent(findNext)
		;
		
		gl.setHorizontalGroup(
			gl.createParallelGroup()
			.addGroup(
				gl.createSequentialGroup()
				.addGroup(
					gl.createParallelGroup()
					.addGroup(
						sg
					)
					.addGroup(
						sg1
					)
				)
				.addGroup(
					gl.createParallelGroup()
					.addGroup(
						gl.createSequentialGroup()
						.addComponent(go)
						.addComponent(markAsRead)
						.addComponent(page, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(pageSize, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(more)
						.addComponent(clear)
						.addComponent(downVoter)
					)
					.addGroup(
						gl.createSequentialGroup()
						.addComponent(sort, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(merge, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(excerpts, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(refresh, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					)
				)
			)
			.addComponent(scroll)
			.addGroup(
				sg2
			)
		);
		ParallelGroup pg = gl.createParallelGroup(Alignment.BASELINE);
		for (int i = 0; i < siteUrls.length / 2; i++) {
			pg.addComponent(siteIconLabels[i]);
			pg.addComponent(siteUrls[i]);
			pg.addComponent(tags[i]);
		}
		ParallelGroup pg1 = gl.createParallelGroup(Alignment.BASELINE);
		for (int i = siteUrls.length / 2; i < siteUrls.length; i++) {
			pg1.addComponent(siteIconLabels[i]);
			pg1.addComponent(siteUrls[i]);
			pg1.addComponent(tags[i]);
		}
		
		ParallelGroup pg2 = gl.createParallelGroup(Alignment.BASELINE);
		pg2.addComponent(totalLabel);
		for (int i = 0; i < siteUrls.length; i++) {
			pg2.addComponent(status[i]);
		}		
		pg2
		.addComponent(wikiBackgroundTask)
		.addComponent(findLabel)
		.addComponent(findValue)
		.addComponent(findPrev)
		.addComponent(findNext)
		;
		gl.setVerticalGroup(
			gl.createSequentialGroup()
			.addGroup(
				gl.createParallelGroup(Alignment.LEADING)
				.addGroup(
					gl.createSequentialGroup()
					.addGroup(
						pg
					)
					.addGroup(
						pg1
					)
				)
				.addGroup(
					gl.createSequentialGroup()
					.addGroup(
						gl.createParallelGroup(Alignment.BASELINE)
						.addComponent(go, 25, 25, 25)
						.addComponent(markAsRead)
						.addComponent(page)
						.addComponent(pageSize)
						.addComponent(more)
						.addComponent(clear)
						.addComponent(downVoter)
					)
					.addGroup(
						gl.createParallelGroup(Alignment.BASELINE)
						.addComponent(sort)
						.addComponent(merge)
						.addComponent(excerpts)
						.addComponent(refresh)
					)
				)
			)
			.addComponent(scroll)
			.addGroup(
				pg2
			)
		);
		List<Component> comps = new LinkedList<Component>(Arrays.<Component>asList(sort, go, more, page, pageSize, clear, downVoter));
		for (int i = 0; i < siteUrls.length; i++) {
			comps.add(siteIconLabels[i]);
			comps.add(siteUrls[i]);
			comps.add(tags[i]);
		}
		gl.linkSize(SwingConstants.VERTICAL, comps.toArray(new Component[0]));
		gl.linkSize(SwingConstants.VERTICAL, findValue, findPrev, findNext);
		
		createMenu();
	}
	/**
	 * Opens a new downvote detector window.
	 */
	protected void doStartDownvoteDetector() {
		new DownvoteTracker(qctx).setVisible(true);
	}
	/**
	 * @param e
	 */
	protected void doWikiBackgroundPopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			wikiTestMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}
	/**
	 * Test the summary entry for the list of keywords
	 * @param se
	 * @param text
	 * @return
	 */
	private boolean checkEntryFor(SummaryEntry se, String[] text) {
		for (int i = 0; i < text.length; i++) {
			String kw = text[i];
			boolean any = false;
			
			any |= se.title.contains(kw);
			any |= se.excerpt.contains(kw);
			any |= se.userName.contains(kw);
			any |= se.tags.contains(kw);
			
			if (!any) {
				return false;
			}
		}
		return true;
	}
	/**
	 * Find next element starting after the current selection
	 */
	protected void doFindNext() {
		String[] keywords = findValue.getText().split("\\s+");
		int idx = table.getSelectedRow() + 1;
		if (idx >= table.getRowCount()) {
			idx = 0;
		}
		for (int i = 0; i < table.getRowCount(); i++) {
			int i0 = (i + idx) % table.getRowCount();
			int mdl = table.convertRowIndexToModel(i0);
			SummaryEntry se = model.questions.get(mdl);
			if (checkEntryFor(se, keywords)) {
				table.getSelectionModel().setSelectionInterval(i0, i0);
				table.scrollRectToVisible(table.getCellRect(i0, 0, true));
				break;
			}
		}
	}
	/**
	 * 
	 */
	protected void doFindPrevious() {
		String[] keywords = findValue.getText().split("\\s+");
		int idx = table.getSelectedRow() - 1;
		if (idx < 0) {
			idx = table.getSelectedRow() - 1;
		}
		for (int i = table.getRowCount() - 1; i >= 0; i--) {
			int i0 = (i + idx) % table.getRowCount();
			int mdl = table.convertRowIndexToModel(i0);
			SummaryEntry se = model.questions.get(mdl);
			if (checkEntryFor(se, keywords)) {
				table.getSelectionModel().setSelectionInterval(i0, i0);
				table.scrollRectToVisible(table.getCellRect(i0, 0, true));
				break;
			}
		}
	}
	/**
	 * @param component
	 */
	protected void doOpenSite(final String site) {
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				Desktop d = Desktop.getDesktop();
				if (d != null) {
					d.browse(new URI(site));
				}
				return null;
			}
		};
		worker.execute();
	}
	/**
	 * 
	 */
	private void createMenu() {
		menu = new JPopupMenu();
		JMenuItem openQuestion = new JMenuItem("Open question");
		openQuestion.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doOpenQuestion();
			}
		});
		JMenuItem openUser = new JMenuItem("Open user");
		openUser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doOpenUser();
			}
		});
		JMenuItem copyAvatarUrl = new JMenuItem("Copy Avatar URL");
		copyAvatarUrl.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doCopyAvatarUrl();
			}
		});
		JMenuItem ignore = new JMenuItem("Ignore locally");
		ignore.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doAddIgnore();
			}
		});
		JMenuItem ignoreGlobal = new JMenuItem("Ignore globally");
		ignoreGlobal.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doAddIgnoreGlobal();
			}
		});
		JMenuItem resetLocal = new JMenuItem("Reset local ignore");
		resetLocal.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				resetLocalIgnores();
			}
		});
		
		JMenuItem showLocalIgnores = new JMenuItem("Show local ignores...");
		showLocalIgnores.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doShowLocalIgnores();
			}
		});
		JMenuItem showGlobalIgnores = new JMenuItem("Show global ignores...");
		showGlobalIgnores.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doShowGlobalIgnores();
			}
		});
		JMenuItem removeFromList = new JMenuItem("Remove");
		removeFromList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doRemoveFromList();
			}
		});
		JMenuItem unread = new JMenuItem("Unread");
		unread.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doUnread();
			}
		});

		JMenuItem wikiDelTest = new JMenuItem("Detail this");
		wikiDelTest.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doWikiDelTest();
			}
		});
		JMenuItem wikiDelTestAll = new JMenuItem("Detail ALL");
		wikiDelTestAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doWikiDelTestAll();
			}
		});
		
		JMenuItem autowith = new JMenuItem("Auto size columns");
		autowith.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doAutowith();
			}
		});
		JMenuItem wikiUntesed = new JMenuItem("Detail (?)");
		wikiUntesed.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doUnknownWikiDelTest();
			}
		});
		
		JMenuItem openUserLocally = new JMenuItem("Open user here");
		openUserLocally.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doOpenUserLocally();
			}
		});
		JMenuItem openQuestionLocally = new JMenuItem("Open question here");
		openQuestionLocally.setEnabled(false);
		openUserLocally.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doOpenQuestionLocally();
			}
		});
		JMenuItem openUserRecent = new JMenuItem("Open user recent");
		openUserRecent.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doOpenUserRecent();
			}
		});
		detailUnread = new JCheckBoxMenuItem("Auto-detail unread");
		detailUnread.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (detailUnread.isSelected()) {
					doDetailUnread();
				}
			}
		});
		
		menu.add(openQuestion);
		menu.add(openUser);
		menu.add(openUserRecent);
		menu.add(copyAvatarUrl);
		menu.add(unread);
		menu.addSeparator();
		menu.add(openQuestionLocally);
		menu.add(openUserLocally);
		menu.addSeparator();
		menu.add(wikiDelTest);
		menu.add(wikiUntesed);
		menu.add(wikiDelTestAll);
		menu.add(detailUnread);
		menu.addSeparator();
		menu.add(removeFromList);
		menu.addSeparator();
		menu.add(ignore);
		menu.add(ignoreGlobal);
		menu.add(resetLocal);
		menu.addSeparator();
		menu.add(showLocalIgnores);
		menu.add(showGlobalIgnores);
		menu.add(autowith);
		
		wikiTestMenu = new JPopupMenu();
		JMenuItem cancelWiki = new JMenuItem("Cancel Wiki/Deleted test");
		cancelWiki.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doCancelWiki();
			}
		});
		
		wikiTestMenu.add(cancelWiki);
	}
	/**
	 * 
	 */
	protected void doOpenUserRecent() {
		if (table.getSelectedRow() >= 0) {
			Desktop d = Desktop.getDesktop();
			int idx = table.getRowSorter().convertRowIndexToModel(table.getSelectedRow());
			if (d != null) {
				try {
					SummaryEntry se = model.questions.get(idx);
					d.browse(new URI("http://" + se.site + "/users/" +se.userId + "?tab=recent#sort-top"));
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	/**
	 * Delete the selected entry
	 * @param e
	 */
	protected void doKeyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		if (e.getKeyCode() == KeyEvent.VK_DELETE) {
			int idx = table.getSelectedRow();
			doRemoveFromList();
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
	protected void doOpenQuestionLocally() {
		// TODO Auto-generated method stub
		
	}
	/**
	 * 
	 */
	protected void doOpenUserLocally() {
		SummaryEntry se = getSelectedEntry();
		if (se != null) {
			qctx.panelManager.openUser(new String[] { se.site }, new String[] { se.userId }, se.userName);
		}
	}
	/**
	 * Cancel running background wiki process
	 */
	protected void doCancelWiki() {
		if (wikiSwingWorker != null) {
			wikiSwingWorker.cancel(true);
			wikiSwingWorker = null;
		}
	}
	/**
	 * 
	 */
	protected void doUnknownWikiDelTest() {
		// once
		if (wikiBackgroundTask.getIcon() != null) {
			return;
		}
		final List<String> sites = new ArrayList<String>();
		final List<String> ids = new ArrayList<String>();
		for (SummaryEntry se : model.questions) {
			if (((se.wiki == null && !qctx.knownWikis.containsKey(se.site + "/" + se.id)) 
					|| se.favored == null) 
					&& !se.deleted) {
				sites.add(se.site);
				ids.add(se.id);
			}
		}
		doProcessWikiBackground(sites, ids);
		
	}
	/**
	 * Detail all unread entries.
	 */
	protected void doDetailUnread() {
		// once
		if (wikiBackgroundTask.getIcon() != null) {
			return;
		}
		final List<String> sites = new ArrayList<String>();
		final List<String> ids = new ArrayList<String>();
		SummaryEntry se0 = getSelectedEntry();
		// add current to be sure
		if (se0 != null) {
			sites.add(se0.site);
			ids.add(se0.id);
		}
		for (SummaryEntry se : model.questions) {
			if (!se.markRead) {
				sites.add(se.site);
				ids.add(se.id);
			}
		}
		doProcessWikiBackground(sites, ids);
		
	}
	/** Retrieve wiki status for all listed entries. */
	protected void doWikiDelTestAll() {
		// once
		if (wikiBackgroundTask.getIcon() != null) {
			return;
		}
		final List<String> sites = new ArrayList<String>();
		final List<String> ids = new ArrayList<String>();
		for (SummaryEntry se : model.questions) {
			sites.add(se.site);
			ids.add(se.id);
		}
		doProcessWikiBackground(sites, ids);
	}
	/**
	 * @param sites
	 * @param ids
	 */
	private void doProcessWikiBackground(final List<String> sites,
			final List<String> ids) {
		wikiBackgroundTask.setIcon(qctx.rolling);
		wikiBackgroundTask.setToolTipText("Analyzing all questions");
		wikiSwingWorker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				for (int i = 0; i < sites.size(); i++) {
					if (isCancelled()) {
						break;
					}
					if (i > 0) {
						TimeUnit.MILLISECONDS.sleep(250); // sleep to avoid overwhelming the site
					}
					byte[] data = SOPageParsers.getAQuestionData("http://" + sites.get(i), ids.get(i));
					QuestionEntry qe = SOPageParsers.processQuestionPage(data);
					qe.site = sites.get(i);
					doUpdateListForWiki(qe, ids.get(i), i + 1, sites.size());
				}
				return null;
			}
			@Override
			protected void done() {
				wikiBackgroundTask.setIcon(null);
				wikiBackgroundTask.setText("");
				wikiBackgroundTask.setToolTipText(null);
			}
		};
		wikiSwingWorker.execute();
	}
	/**
	 * 
	 */
	protected void doAutowith() {
		GUIUtils.autoResizeColWidth(table, model);
	}
	protected void doWikiDelTest() {
		if (wikiBackgroundTask.getIcon() != null) {
			return;
		}
		SummaryEntry se = getSelectedEntry();
		if (se != null) {
			final String site = se.site;
			final String id = se.id;
			doProcessWikiBackground(Collections.singletonList(site), Collections.singletonList(id));
		}
	}
	/**
	 * @param qe the question entry object, non null
	 * @param originalId the original question id
	 * @param index the current element index
	 * @param total the total amount of work, zero to indicate no label
	 */
	protected void doUpdateListForWiki(final QuestionEntry qe, 
			final String originalId, final int index, final int total) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				String id = qe.id;
				boolean isDeleted = false;
				if (id == null || id.length() == 0) {
					id = originalId;
					isDeleted = true;
				}
				// locate all questions with this id
				for (SummaryEntry se : model.questions) {
					if (se.site.equals(qe.site) && se.id.equals(id)) {
						se.wiki = qe.wiki;
						se.favored = qe.favorite;
						se.deleted = isDeleted;
						qctx.knownWikis.put(qe.site + "/" + id, qe.wiki);
					}
				}
				model.fireTableDataChanged();
				if (total > 0) {
					wikiBackgroundTask.setText(index + "/" + total);
				}
			}
		});
	}
	/**
	 * 
	 */
	protected void doUnread() {
		if (table.getSelectedRow() >= 0) {
			int idx = table.convertRowIndexToModel(table.getSelectedRow());
			SummaryEntry se = model.questions.get(idx);
			se.markRead = false;
			countUnreadAndSet();
			model.fireTableRowsUpdated(idx, idx);
		}		
	}
	/**
	 * Removes the currently selected item from the view only.
	 */
	protected void doRemoveFromList() {
		if (table.getSelectedRow() >= 0) {
			int idx = table.convertRowIndexToModel(table.getSelectedRow());
			model.questions.remove(idx);
			model.fireTableRowsDeleted(idx, idx);
			updateTotalLabel();
		}
	}
	/**
	 * 
	 */
	protected void doShowGlobalIgnores() {
		qctx.globalIgnoreListGUI.remapIgnores();
		qctx.globalIgnoreListGUI.setVisible(true);
	}
	/**
	 * 
	 */
	protected void resetLocalIgnores() {
		ignores.clear();
		if (ignoreListGUI != null) {
			ignoreListGUI.remapIgnores();
		}
	}
	/**
	 * 
	 */
	protected void doShowLocalIgnores() {
		if (ignoreListGUI == null) {
			ignoreListGUI = new IgnoreListGUI(ignores);
			ignoreListGUI.setLocationRelativeTo(this);
			ignoreListGUI.autoSizeTable();
		}
		ignoreListGUI.remapIgnores();
		ignoreListGUI.setVisible(true);
	}
	/**
	 * 
	 */
	protected void doAddIgnoreGlobal() {
		if (table.getSelectedRow() >= 0) {
			int idx = table.getRowSorter().convertRowIndexToModel(table.getSelectedRow());
			SummaryEntry se = model.questions.get(idx);
			qctx.globalIgnores.put(se.site + "/" + se.id, se.title);
			model.questions.remove(idx);
			model.fireTableRowsDeleted(idx, idx);
			qctx.globalIgnoreListGUI.remapIgnores();
		}		
	}
	/**
	 * 
	 */
	protected void doAddIgnore() {
		if (table.getSelectedRow() >= 0) {
			int idx = table.getRowSorter().convertRowIndexToModel(table.getSelectedRow());
			SummaryEntry se = model.questions.get(idx);
			ignores.put(se.site + "/" + se.id, se.title);
			model.questions.remove(idx);
			model.fireTableRowsDeleted(idx, idx);
			if (ignoreListGUI != null) {
				ignoreListGUI.remapIgnores();
			}
		}		
	}
	protected void doCopyAvatarUrl() {
		if (table.getSelectedRow() >= 0) {
			Desktop d = Desktop.getDesktop();
			int idx = table.getRowSorter().convertRowIndexToModel(table.getSelectedRow());
			if (d != null) {
				SummaryEntry se = model.questions.get(idx);
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(se.avatarUrl), new ClipboardOwner() {
					@Override
					public void lostOwnership(Clipboard clipboard, Transferable contents) {
						// nothing
					}
				});		
			}
		}
	}
	protected void doMarkAsRead() {
		for (SummaryEntry se : model.questions) {
			se.markRead = true;
		}
		countUnreadAndSet();
		model.fireTableDataChanged();
	}
	protected void doRefreshCounter() {
		refreshCounter--;
		setRefreshLabel();
		if (refreshCounter <= 0) {
			doRetrieve(1, getPageSize());
		}
	}
	private void setRefreshLabel() {
		refresh.setText(String.format("Refresh (in %ds)", refreshCounter));
	}
	protected void doExcerptToggle() {
		if (excerpts.isSelected()) {
			table.setRowHeight(32 + 3 * 10);
		} else {
			table.setRowHeight(32);
		}
	}
	protected void doOpenUser() {
		if (table.getSelectedRow() >= 0) {
			Desktop d = Desktop.getDesktop();
			int idx = table.getRowSorter().convertRowIndexToModel(table.getSelectedRow());
			if (d != null) {
				try {
					SummaryEntry se = model.questions.get(idx);
					d.browse(new URI("http://" + se.site + "/users/" +se.userId));
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	protected void doOpenQuestion() {
		if (table.getSelectedRow() >= 0) {
			final int idx = table.getRowSorter().convertRowIndexToModel(table.getSelectedRow());
			SwingWorker<?, ?> sw = new SwingWorker<Void, Void>() {
				protected Void doInBackground() throws Exception {
					Desktop d = Desktop.getDesktop();
					if (d != null) {
						try {
							SummaryEntry se = model.questions.get(idx);
							d.browse(new URI("http://" + se.site + "/questions/" +se.id));
						} catch (IOException e1) {
							e1.printStackTrace();
						} catch (URISyntaxException e1) {
							e1.printStackTrace();
						}
					}
					return null;
				};
			};
			sw.execute();
			invalidate();
			repaint();
		}
	}
	protected int getPageSize() {
		int ps = 15;
		if (!"".equals(pageSize.getText())) {
			ps = Integer.parseInt(pageSize.getText().trim());
		}
		return ps;
	}
	protected void doGetMore() {
		if (!"".equals(page.getText())) {
			int i = Integer.parseInt(page.getText().trim());
			doRetrieve(i + 1, getPageSize());
			page.setText(Integer.toString(i + 1));
		}
	}
	void doTableClicked(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 1) {
			doQuestionClick();
		}
		if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
			doOpenQuestion();
		}
	}
	void doTablePopupClick(MouseEvent e) {
		if (e.isPopupTrigger()) {
			int i = table.rowAtPoint(e.getPoint());
			if (i >= 0 && i < table.getRowCount()) {
				table.getSelectionModel().setSelectionInterval(i, i);
			}
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}
	private void doQuestionClick() {
		if (table.getSelectedRow() >= 0 && readCheck) {
			int idx = table.getRowSorter().convertRowIndexToModel(table.getSelectedRow());
			SummaryEntry se = model.questions.get(idx);
			se.markRead = true;
			countUnreadAndSet();
			model.fireTableRowsUpdated(idx, idx);
		}		
	}
	/**
	 * Retrieve a page indexed.
	 * @param page the page index
	 * @param ps the page size
	 */
	protected void doRetrieve(final int page, final int ps) {
		List<SwingWorker<Void, Void>> workers = new LinkedList<SwingWorker<Void, Void>>();
		boolean once = true;
		for (int i = 0; i < siteUrls.length; i++) {
			JCheckBox cb = siteUrls[i];
			JTextField tf = tags[i];
			JLabel lbl = status[i];
			if (cb.isSelected()) {
				if (once) {
					go.setIcon(qctx.rolling);
					go.setEnabled(false);
					more.setEnabled(false);
					tabTitle.setIcon(qctx.rolling);
					refreshTimer.stop();
				}
				
				final String tgs = tf.getText();
				final String sorts = (String)sort.getSelectedItem();
				final String siteStr = cb.getText();
				final boolean mergeVal = merge.isSelected();
				SwingWorker<Void, Void> worker = createWorker(page, tgs, sorts,
						siteStr, mergeVal, lbl, ps);
				workers.add(worker);
				retrieveWip.incrementAndGet();
			}
		}
		for (SwingWorker<?, ?> worker : workers) {
			worker.execute();
		}
	}
	protected SwingWorker<Void, Void> createWorker(final int page,
			final String tgs, final String sorts, final String siteStr,
			final boolean mergeVal, final JLabel statusLabel, final int ps) {
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			List<SummaryEntry> summary = Collections.emptyList();
			@Override
			protected Void doInBackground() throws Exception {
				try {
					byte[] data = null;
					String s1 = sorts.substring(0, sorts.indexOf("-")); 
					if (sorts.endsWith("-U")) {
						if (tgs.length() > 0) {
							String tgs1 = tgs.replaceAll("\\s", "+");
							data = SOPageParsers.getUnansweredData(siteStr, tgs1, s1, page, ps);
						} else {
							data = SOPageParsers.getUnansweredData(siteStr, null, s1, page, ps);
						}
					} else {
						if (tgs.length() > 0) {
							String tgs1 = tgs.replaceAll("\\s", "+");
							data = SOPageParsers.getQuestionsData(siteStr, tgs1, s1, page, ps);
						} else {
							data = SOPageParsers.getQuestionsData(siteStr, null, s1, page, ps);
						}
					}
					;
					summary = SOPageParsers.processMainPage(data);
				} catch (Throwable e) {
					e.printStackTrace();
				}
				return null;
			}
			@Override
			protected void done() {
				displayResponse(siteStr, mergeVal, statusLabel, summary);
			}
		};
		return worker;
	}
	protected void doRefreshToggle() {
		if (refresh.isSelected()) {
			refreshTimer.start();
		} else {
			refreshTimer.stop();
		}
	}
	public void initPanel(int panelIndex, Properties p) {
		GUIUtils.saveLoadValues(this, false, p, "P" + panelIndex + "-");
		// restore default sort order
		DefaultRowSorter<?, ?> drs = (DefaultRowSorter<?, ?>)table.getRowSorter();
		String sortKey = p.getProperty("P" + panelIndex + "-" + "SortKey");
		String sortIndex = p.getProperty("P" + panelIndex + "-" + "SortIndex");
		if (sortKey != null && sortIndex != null) {
			int j = Integer.parseInt(sortIndex);
			drs.setSortKeys(Collections.singletonList(new SortKey(j, SortOrder.valueOf(sortKey))));
		}
		String ignoreCnt = p.getProperty("P" + panelIndex + "-" + "IgnoreCount");
		if (ignoreCnt != null) {
			int ic = Integer.parseInt(ignoreCnt);
			for (int i = 0; i < ic; i++) {
				ignores.put(p.getProperty("P" + panelIndex + "-" + "Ignore" + i), p.getProperty("P" + panelIndex + "-" + "IgnoreTitle" + i));
			}
		}
		doLoadColumnWidths(panelIndex, p);
		
		String ql = p.getProperty("P" + panelIndex + "-" + "Questions");
		if (ql != null) {
			try {
				ByteArrayInputStream bin = new ByteArrayInputStream(
						Base64.decodeBase64(ql.getBytes("ISO-8859-1")));
				ObjectInputStream oin = new ObjectInputStream(bin);
				@SuppressWarnings("unchecked")
				List<SummaryEntry> list = (List<SummaryEntry>)oin.readObject();
				model.questions.addAll(list);
				countUnreadAndSet();
				model.fireTableDataChanged();
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (ClassNotFoundException ex) {
				ex.printStackTrace();
			}
		}
		
		doExcerptToggle();
		doRefreshToggle();
	}
	private void doLoadColumnWidths(int panelIndex, Properties p) {
		boolean firstTime = true;

		TableColumnModel tcm = table.getTableHeader().getColumnModel();
		int[] cols = new int[table.getColumnCount()];
		TableColumn[] tcs = new TableColumn[cols.length];
		for (int i = 0; i < table.getColumnCount(); i++) {
			String mi = p.getProperty("P" + panelIndex + "-" + "ColumnIndex" + i);
			if (mi != null) {
				cols[i] = Integer.parseInt(mi);
			}
			tcs[i] = tcm.getColumn(i);
		}
		for (int i = 0; i < cols.length; i++) {
			int j = 0;
			for (int k = 0; k < cols.length; k++) {
				if (tcm.getColumn(k).getModelIndex() == cols[i]) {
					j = k;
					break;
				}
			}
			tcm.moveColumn(j, i);
		}
		
		for (int i = 0; i < model.getColumnCount(); i++) {
			String s = p.getProperty("P" + panelIndex + "-" + "Column" + i);
			if (s != null) {
				if (firstTime) {
					table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
					adjusted = true;
					firstTime = false;
				}
				int w = Integer.parseInt(s);
				table.getColumnModel().getColumn(i).setWidth(w);
				table.getColumnModel().getColumn(i).setPreferredWidth(w);
			}
		}
	}
	private void doSaveColumnWidths(int panelIndex, Properties p) {
		for (int i = 0; i < model.getColumnCount(); i++) {
			TableColumn tc = table.getTableHeader().getColumnModel().getColumn(i);
			p.setProperty("P" + panelIndex + "-" + "Column" + i, Integer.toString(tc.getWidth()));
			int j = tc.getModelIndex();
			p.setProperty("P" + panelIndex + "-" + "ColumnIndex" + i, Integer.toString(j));
		}
	}
	public void donePanel(int panelIndex, Properties p) {
		GUIUtils.saveLoadValues(this, true, p, "P" + panelIndex + "-");
		DefaultRowSorter<?, ?> drs = (DefaultRowSorter<?, ?>)table.getRowSorter();
		List<? extends SortKey> list = drs.getSortKeys();
		if (list.size() > 0) {
			p.setProperty("P" + panelIndex + "-" + "SortKey", list.get(0).getSortOrder().name());
			p.setProperty("P" + panelIndex + "-" + "SortIndex", Integer.toString(list.get(0).getColumn()));
		}
		doSaveColumnWidths(panelIndex, p);
		p.setProperty("P" + panelIndex + "-" + "IgnoreCount", Integer.toString(ignores.size()));
		int i = 0;
		for (Map.Entry<String, String> e: ignores.entrySet()) {
			p.setProperty("P" + panelIndex + "-" + "Ignore" + i, e.getKey());
			p.setProperty("P" + panelIndex + "-" + "IgnoreTitle" + i, e.getValue());
			i++;
		}
		// save the current list of records to be restored on load
		ByteArrayOutputStream bout = new ByteArrayOutputStream(16 * 1024);
		try {
			ObjectOutputStream os = new ObjectOutputStream(bout);
			os.writeObject(model.questions);
			os.close();
			p.setProperty("P" + panelIndex + "-" + "Questions", 
					new String(Base64.encodeBase64(bout.toByteArray(), true), "ISO-8859-1"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		if (ignoreListGUI != null) {
			ignoreListGUI.dispose();
			ignoreListGUI = null;
		}
		refreshTimer.stop();
	}
	/**
	 * @param siteStr
	 * @param mergeVal
	 * @param statusLabel
	 * @param summary 
	 */
	private void displayResponse(final String siteStr, final boolean mergeVal,
			final JLabel statusLabel, List<SummaryEntry> summary) {
		String st = siteStr;
		if (st.startsWith("http://")) {
			st = st.substring(7);
		}
		for (SummaryEntry e : summary) {
			e.site = st;
		}
		int updated = 0;
		int newer = 0;
		readCheck = false;
		SummaryEntry curr = getSelectedEntry();
		if (!mergeVal) {
			model.questions.addAll(summary);
		} else {
			for (SummaryEntry e : summary) {
				if (ignores.containsKey(e.site + "/" + e.id)) {
					continue;
				}
				if (qctx.globalIgnores.containsKey(e.site + "/" + e.id)) {
					continue;
				}
				int i = 0;
				boolean exists = false;
				for (SummaryEntry m : new LinkedList<SummaryEntry>(model.questions)) {
					if (e.site.equals(m.site) && e.id.equals(m.id)) {
						model.questions.set(i, e);
						if (e.time > m.time) {
							updated++;
						} else {
							// TODO copy read marker if views haven't changed
							// could indicate a new favor
//							if (e.views == m.views || !detailUnread.isSelected()) {
								e.markRead = m.markRead;
//							}
							e.wiki = m.wiki;
							e.favored = m.favored;
//							e.deleted = m.deleted; //?
						}
						exists = true;
					}
					i++;
				}
				if (!exists) {
					model.questions.add(e);
					newer++;
				}
			}
		}
		model.fireTableDataChanged();
		if (curr != null) {
			setSelectedEntry(curr.id);
		}
		readCheck = true;
		
		statusLabel.setText(String.format("U: %d, N: %d", updated, newer));
		statusLabel.setVisible(true);
		if (retrieveWip.decrementAndGet() == 0) {
			// the last will restore the button status
			if (!summary.isEmpty() && !adjusted) {
				GUIUtils.autoResizeColWidth(table, model);
				adjusted = true;
			}
			go.setIcon(qctx.go);
			tabTitle.setIcon(null);
			go.setEnabled(true);
			more.setEnabled(true);
			// continue the refresh loop if it was selected
			refreshCounter = REFRESH_TIME;
			if (refresh.isSelected()) {
				setRefreshLabel();
				refreshTimer.start();
			}
			updateTotalLabel();
			if (detailUnread.isSelected()) {
				doDetailUnread();
			}
			countUnreadAndSet();
		}
	}
	/** Update total label with the current amount. */
	private void updateTotalLabel() {
		totalLabel.setText(String.format("Total: %d", model.questions.size()));
	}
	public SummaryEntry getSelectedEntry() {
		int idx = table.getSelectedRow();
		if (idx >= 0) {
			return model.questions.get(table.convertRowIndexToModel(idx));
		}
		return null;
	}
	public void setSelectedEntry(String id) {
		for (int i = 0; i < model.questions.size(); i++) {
			SummaryEntry se = model.questions.get(i);
			if (se.id.equals(id)) {
				int idx = table.convertRowIndexToView(i);
				table.getSelectionModel().setSelectionInterval(idx, idx);
			}
		}
	}
	/**
	 * @param tabTitle the tabTitle to set
	 */
	public void setTabTitle(TitleWithClose tabTitle) {
		this.tabTitle = tabTitle;
	}
	/**
	 * @return the tabTitle
	 */
	public TitleWithClose getTabTitle() {
		return tabTitle;
	}
	protected void countUnreadAndSet() {
		if (tabTitle != null) {
			int value = 0;
			for (SummaryEntry se : model.questions) {
				if (!se.markRead) {
					value++;
				}
			}
			tabTitle.setUnread(value);
		}
	}
	/**
	 * 
	 */
	private void doClearListing() {
		model.questions.clear();
		model.fireTableDataChanged();
		tabTitle.setUnread(0);
		updateTotalLabel();
	}
}
