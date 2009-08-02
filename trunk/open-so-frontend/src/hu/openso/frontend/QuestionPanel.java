package hu.openso.frontend;

import java.awt.Color;
import java.awt.Component;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.DefaultRowSorter;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import org.apache.commons.httpclient.HttpException;
import org.htmlparser.util.ParserException;

public class QuestionPanel extends JPanel {
	private static final long serialVersionUID = 2165339317109256363L;
	JTable table;
	QuestionModel model;
	JButton go;
	@SaveValue
	JTextField page;
	ImageIcon rolling;
	ImageIcon okay;
	ImageIcon unknown;
	ImageIcon error;
	ImageIcon wiki;
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
	ImageIcon goImage;
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
	Map<String, ImageIcon> avatars;
	Map<String, ImageIcon> avatarsLoading;
	Map<String, ImageIcon> siteIcons;
	ExecutorService exec;
	boolean readCheck = true;
	/** Set of site name + question id to ignore to the title. */
	Map<String, String> ignores = new LinkedHashMap<String, String>();
	Map<String, String> globalIgnores;
	IgnoreListGUI ignoreListGUI;
	final IgnoreListGUI globalIgnoreListGUI;
	private TitleWithClose tabTitle;
	public class QuestionModel extends AbstractTableModel {
		private static final long serialVersionUID = -898209429130786969L;
		List<SummaryEntry> questions = new ArrayList<SummaryEntry>();
		Class<?>[] columnClasses = {
			ImageIcon.class, ImageIcon.class, ImageIcon.class, String.class,
			Integer.class, Integer.class, Integer.class, String.class, String.class, 
			ImageIcon.class, String.class, Integer.class, String.class,
			String.class
		};
		String[] columnNames = {
			"S", "A", "W", "B",
			"Votes", "Answers", "Views", "Question", "Time",
			"Avatar", "User", "Rep", "Badges", 
			"Tags"
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
			case 0: return siteIcons.get(se.site);
			case 1: return se.accepted ? okay : (se.deleted ? error : null);
			case 2:
				Boolean isWiki = se.wiki;
				return isWiki != null ? (isWiki.booleanValue() ? wiki : null) : unknown;
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
					ImageIcon icon = avatars.get(url);
					if (icon == null) {
						if (!avatarsLoading.containsKey(url)) {
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
			}
			return null;
		}
		private void loadImageFor(final int rowIndex, final int columnIndex,
				final String url) {
			exec.submit(new Runnable() {
				@Override
				public void run() {
					try {
//						System.out.printf("Getting avatar (%d): %s%n", rowIndex, url);
						ImageIcon icon = new ImageIcon(ImageIO.read(new URL(url)));
						avatars.put(url, icon);
						avatarsLoading.remove(url);
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
	public QuestionPanel(Map<String, ImageIcon> avatars, 
				Map<String, ImageIcon> avatarsLoading, 
				Map<String, ImageIcon> siteIcons,
				ExecutorService exec, Map<String, String> globalIgnores, 
				IgnoreListGUI globalIgnoreListGUI) {
		this.avatars = avatars;
		this.avatarsLoading = avatarsLoading;
		this.siteIcons = siteIcons;
		this.exec = exec;
		this.globalIgnores = globalIgnores;
		this.globalIgnoreListGUI = globalIgnoreListGUI;
		init();
		
	}
	private void init() {
		rolling = new ImageIcon(getClass().getResource("res/loading.gif"));
		okay = new ImageIcon(getClass().getResource("res/ok.png"));
		unknown = new ImageIcon(getClass().getResource("res/unknown.png"));
		error = new ImageIcon(getClass().getResource("res/error.png"));
		wiki = new ImageIcon(getClass().getResource("res/wiki.png"));
		
		siteIcons.put("stackoverflow.com", new ImageIcon(getClass().getResource("res/so.png")));
		siteIcons.put("serverfault.com", new ImageIcon(getClass().getResource("res/sf.png")));
		siteIcons.put("superuser.com", new ImageIcon(getClass().getResource("res/su.png")));
		siteIcons.put("meta.stackoverflow.com", new ImageIcon(getClass().getResource("res/meta.png")));
		
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
		
		goImage = new ImageIcon(getClass().getResource("res/go.png"));
		go = new JButton(goImage);
		go.setToolTipText("Read the first page of the selected sites and subpages");
		ActionListener doRetrieveAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				page.setText("1");
				doRetrieve(1);
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
			new JLabel(siteIcons.get("stackoverflow.com")),	
			new JLabel(siteIcons.get("meta.stackoverflow.com")),	
			new JLabel(siteIcons.get("serverfault.com")),	
			new JLabel(siteIcons.get("superuser.com")),	
		};
		tags = new JTextField[] {
			new JTextField(15),
			new JTextField(15),
			new JTextField(15),
			new JTextField(15),
		};
		totalLabel = new JLabel("Welcome to Open Stack Overflow Frontend");
		status = new JLabel[] {
			new JLabel("Stack Overflow", siteIconLabels[0].getIcon(), JLabel.LEFT), 
			new JLabel("Meta", siteIconLabels[1].getIcon(), JLabel.LEFT), 
			new JLabel("Server Fault", siteIconLabels[2].getIcon(), JLabel.LEFT),
			new JLabel("Super User", siteIconLabels[3].getIcon(), JLabel.LEFT),
		};
		wikiBackgroundTask = new JLabel();
		
		page = new JFormattedTextField(1);
		page.setColumns(4);
		more = new JButton(new ImageIcon(getClass().getResource("res/more.png")));
		more.setToolTipText("Read the Nth page of the selected sites and subpages");
		more.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doGetMore();
			}
		});
		clear = new JButton(new ImageIcon(getClass().getResource("res/clear.png")));
		clear.setToolTipText("Clears the entire list of questions");
		clear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.questions.clear();
				model.fireTableDataChanged();
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
		sg2.addComponent(wikiBackgroundTask);
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
						.addComponent(more)
						.addComponent(clear)
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
		pg2.addComponent(wikiBackgroundTask);
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
						.addComponent(more)
						.addComponent(clear)
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
		List<Component> comps = new LinkedList<Component>(Arrays.<Component>asList(sort, go, more, page, clear));
		for (int i = 0; i < siteUrls.length; i++) {
			comps.add(siteIconLabels[i]);
			comps.add(siteUrls[i]);
			comps.add(tags[i]);
		}
		gl.linkSize(SwingConstants.VERTICAL, comps.toArray(new Component[0]));
		
		createMenu();
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

		JMenuItem wikiDelTest = new JMenuItem("Test for Wiki/Deleted");
		wikiDelTest.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doWikiDelTest();
			}
		});
		JMenuItem wikiDelTestAll = new JMenuItem("Test ALL for Wiki/Deleted");
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
		
		menu.add(openQuestion);
		menu.add(openUser);
		menu.add(copyAvatarUrl);
		menu.add(unread);
		menu.addSeparator();
		menu.add(wikiDelTest);
		menu.add(wikiDelTestAll);
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
	}
	/**
	 * 
	 */
	protected void doAutowith() {
		GUIUtils.autoResizeColWidth(table, model);
	}
	protected void doWikiDelTest() {
		SummaryEntry se = getSelectedEntry();
		if (se != null) {
			final String site = se.site;
			final String id = se.id;
			loadQuestionInBackground(site, id);
		}
	}
	/**
	 * @param site
	 * @param id
	 */
	private void loadQuestionInBackground(final String site, final String id) {
		exec.submit(new Runnable() {
			@Override
			public void run() {
				try {
					byte[] data = SOPageParsers.getAQuestionData("http://" + site, id);
					QuestionEntry qe = SOPageParsers.processQuestionPage(data);
					qe.site = site;
					doUpdateListForWiki(qe, id, 1, 0);
				} catch (ParserException ex) {
					ex.printStackTrace();
				} catch (HttpException e) {
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	/** Retrieve wiki status for all listed entries. */
	protected void doWikiDelTestAll() {
		// once
		if (wikiBackgroundTask.getIcon() != null) {
			return;
		}
		final String[] sites = new String[model.questions.size()];
		final String[] ids = new String [sites.length];
		int i = 0;
		for (SummaryEntry se : model.questions) {
			sites[i] = se.site;
			ids[i] = se.id;
			i++;
		}
		wikiBackgroundTask.setIcon(rolling);
		wikiBackgroundTask.setToolTipText("Analyzing all questions");
		SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				for (int i = 0; i < sites.length; i++) {
					if (i > 0) {
						TimeUnit.MILLISECONDS.sleep(250); // sleep to avoid overwhelming the site
					}
					byte[] data = SOPageParsers.getAQuestionData("http://" + sites[i], ids[i]);
					QuestionEntry qe = SOPageParsers.processQuestionPage(data);
					qe.site = sites[i];
					doUpdateListForWiki(qe, ids[i], i + 1, sites.length);
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
		sw.execute();
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
						se.deleted = isDeleted; 
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
		}
	}
	/**
	 * 
	 */
	protected void doShowGlobalIgnores() {
		globalIgnoreListGUI.remapIgnores();
		globalIgnoreListGUI.setVisible(true);
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
			globalIgnores.put(se.site + "/" + se.id, se.title);
			model.questions.remove(idx);
			model.fireTableRowsDeleted(idx, idx);
			globalIgnoreListGUI.remapIgnores();
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
			doRetrieve(1);
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
	protected void doGetMore() {
		if (!"".equals(page.getText())) {
			int i = Integer.parseInt(page.getText().trim());
			doRetrieve(i + 1);
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
	protected void doRetrieve(final int page) {
		List<SwingWorker<Void, Void>> workers = new LinkedList<SwingWorker<Void, Void>>();
		boolean once = true;
		for (int i = 0; i < siteUrls.length; i++) {
			JCheckBox cb = siteUrls[i];
			JTextField tf = tags[i];
			JLabel lbl = status[i];
			if (cb.isSelected()) {
				if (once) {
					go.setIcon(rolling);
					go.setEnabled(false);
					more.setEnabled(false);
					tabTitle.setIcon(rolling);
					refreshTimer.stop();
				}
				
				final String tgs = tf.getText();
				final String sorts = (String)sort.getSelectedItem();
				final String siteStr = cb.getText();
				final boolean mergeVal = merge.isSelected();
				SwingWorker<Void, Void> worker = createWorker(page, tgs, sorts,
						siteStr, mergeVal, lbl);
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
			final boolean mergeVal, final JLabel statusLabel) {
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
							data = SOPageParsers.getUnansweredData(siteStr, tgs1, s1, page);
						} else {
							data = SOPageParsers.getUnansweredData(siteStr, null, s1, page);
						}
					} else {
						if (tgs.length() > 0) {
							String tgs1 = tgs.replaceAll("\\s", "+");
							data = SOPageParsers.getQuestionsData(siteStr, tgs1, s1, page);
						} else {
							data = SOPageParsers.getQuestionsData(siteStr, null, s1, page);
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
		GUIUtils.saveLoadValues(this, false, p, panelIndex);
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
		GUIUtils.saveLoadValues(this, true, p, panelIndex);
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
				if (globalIgnores.containsKey(e.site + "/" + e.id)) {
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
							e.markRead = m.markRead;
							e.wiki = m.wiki;
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
		if (retrieveWip.decrementAndGet() == 0) {
			// the last will restore the button status
			if (!summary.isEmpty() && !adjusted) {
				GUIUtils.autoResizeColWidth(table, model);
				adjusted = true;
			}
			go.setIcon(goImage);
			tabTitle.setIcon(null);
			go.setEnabled(true);
			more.setEnabled(true);
			// continue the refresh loop if it was selected
			refreshCounter = REFRESH_TIME;
			if (refresh.isSelected()) {
				setRefreshLabel();
				refreshTimer.start();
			}
			totalLabel.setText(String.format("Total: %d", model.questions.size()));
			countUnreadAndSet();
		}
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
}
