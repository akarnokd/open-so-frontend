package hu.openso.frontend;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.DefaultRowSorter;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
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
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class QuestionsGUI extends JFrame {
	/** Annotation for value saving. */
	@Retention(RetentionPolicy.RUNTIME)
	public @interface SaveValue { }
	private static final long serialVersionUID = 5676803531378664660L;
	JTable table;
	QuestionModel model;
	JButton go;
	@SaveValue
	JTextField page;
	ImageIcon rolling;
	ImageIcon okay;
	ImageIcon unknown;
	ImageIcon wiki;
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
	JButton markAsRead;
	
	Map<String, ImageIcon> avatars = new ConcurrentHashMap<String, ImageIcon>();
	Map<String, ImageIcon> avatarsLoading = new ConcurrentHashMap<String, ImageIcon>();
	Map<String, ImageIcon> siteIcons = new HashMap<String, ImageIcon>();
	ExecutorService exec = Executors.newCachedThreadPool();
	class QuestionModel extends AbstractTableModel {
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
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex) {
			SummaryEntry se = questions.get(rowIndex);
			switch (columnIndex) {
			case 0: return siteIcons.get(se.site);
			case 1: return se.accepted ? okay : null;
			case 2:
				Boolean wiki = se.wiki;
				return wiki != null ? (wiki.booleanValue() ? wiki : null) : null;
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
				String color = "#000000";
				if (se.site.equals("stackoverflow.com")) {
					color = "#0077CC";
				} else
				if (se.site.equals("meta.stackoverflow.com")) {
					color = "#3D3D3D";
				} else
				if (se.site.equals("serverfault.com")) {
					color = "#10456A";
				}
				StringBuilder tb = new StringBuilder();
				tb.append("<html><font style='font-size: 16pt; font-weight: bold; color: ")
				.append(color).append(";");
				if (!se.markRead) {
					tb.append("background-color: #FFE0E0;");
				}
				tb.append("'>");
				tb.append(se.title);
				if (excerpts.isSelected()) {
					tb.append("</font><br>")
					.append(se.excerpt)
					.append("<br>&nbsp;</html>");
				}
				return tb.toString();
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
			case 13: return se.tags;
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
				model.fireTableCellUpdated(row, col);
			}
		});
	}
	public ImageIcon createBountyImage(int b) {
		BufferedImage img = new BufferedImage(100, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		String value = "+" + Integer.toString(b);
		g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
		int w = g2.getFontMetrics().stringWidth(value) + 10;
		g2.setColor(new Color(0xFF0077DD));
		g2.fillRoundRect(0, 0, w - 1, 15, 6, 6);
		g2.setColor(Color.WHITE);
		g2.drawString(value, 5, 10);
		return new ImageIcon(img.getSubimage(0, 0, w, 16));
	}
	/**
	 * Resizes the table columns based on the column and data preferred widths.
	 * @param table the original table
	 * @param model the data model
	 * @return the table itself
	 */
    public static JTable autoResizeColWidth(JTable table, AbstractTableModel model) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setModel(model);
 
        int margin = 5;
 
        for (int i = 0; i < table.getColumnCount(); i++) {
            int                     vColIndex = i;
            DefaultTableColumnModel colModel  = (DefaultTableColumnModel) table.getColumnModel();
            TableColumn             col       = colModel.getColumn(vColIndex);
            int                     width     = 0;
 
            // Get width of column header
            TableCellRenderer renderer = col.getHeaderRenderer();
 
            if (renderer == null) {
                renderer = table.getTableHeader().getDefaultRenderer();
            }
 
            Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
 
            width = comp.getPreferredSize().width;
 
            // Get maximum width of column data
            for (int r = 0; r < table.getRowCount(); r++) {
                renderer = table.getCellRenderer(r, vColIndex);
                comp     = renderer.getTableCellRendererComponent(table, table.getValueAt(r, vColIndex), false, false,
                        r, vColIndex);
                width = Math.max(width, comp.getPreferredSize().width);
            }
 
            // Add margin
            width += 2 * margin;
 
            // Set the width
            col.setPreferredWidth(width);
        }
 
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(
            SwingConstants.LEFT);
 
        // table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
 
//        for (int i = 0; i < table.getColumnCount(); i++) {
//            TableColumn column = table.getColumnModel().getColumn(i);
// 
//            column.setCellRenderer(new DefaultTableColour());
//        }
 
        return table;
    }
	public QuestionsGUI() {
		super("Questions");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				exec.shutdown();
				refreshTimer.stop();
				doneConfig();
			}
		});
		
		rolling = new ImageIcon(getClass().getResource("res/loading.gif"));
		okay = new ImageIcon(getClass().getResource("res/ok.png"));
		unknown = new ImageIcon(getClass().getResource("res/unknown.png"));
		wiki = new ImageIcon(getClass().getResource("res/wiki.png"));
		
		siteIcons.put("stackoverflow.com", new ImageIcon(getClass().getResource("res/so.png")));
		siteIcons.put("serverfault.com", new ImageIcon(getClass().getResource("res/sf.png")));
		siteIcons.put("superuser.com", new ImageIcon(getClass().getResource("res/su.png")));
		siteIcons.put("meta.stackoverflow.com", new ImageIcon(getClass().getResource("res/meta.png")));
		
		Container c = getContentPane();
		GroupLayout gl = new GroupLayout(c);
		c.setLayout(gl);
		gl.setAutoCreateContainerGaps(true);
		gl.setAutoCreateGaps(true);
		
		sort = new JComboBox(new String[] { "", "newest", "featured", "hot", "votes", "active" });		
		sort.setSelectedIndex(sort.getItemCount() - 1);
		model = new QuestionModel();
		table = new JTable(model);
		table.setRowHeight(32);
		table.setAutoCreateRowSorter(true);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				doTableClicked(e);
			}
		});
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				doQuestionClick();
			}
		});
		JScrollPane scroll = new JScrollPane(table);
		go = new JButton("Get First");
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
		
		page = new JFormattedTextField(1);
		page.setColumns(4);
		more = new JButton("More");
		more.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doGetMore();
			}
		});
		clear = new JButton("Clear");
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
			sg.addComponent(tags[i], GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
		}
		SequentialGroup sg1 = gl.createSequentialGroup();
		for (int i = siteUrls.length / 2; i < siteUrls.length; i++) {
			sg1.addComponent(siteIconLabels[i], GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
			sg1.addComponent(siteUrls[i], GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
			sg1.addComponent(tags[i], GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
		}
		SequentialGroup sg2 = gl.createSequentialGroup();
		sg2.addComponent(totalLabel);
		for (int i = 0; i < siteUrls.length; i++) {
			sg2.addComponent(status[i]);
		}		
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
						.addComponent(go)
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
		menu.add(openQuestion);
		menu.add(openUser);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				pack();
				setLocationRelativeTo(null);
				initConfig();
				doExcerptToggle();
				setVisible(true);
			}
		});
	}
	protected void doMarkAsRead() {
		for (SummaryEntry se : model.questions) {
			se.markRead = true;
		}
	}
	protected void doRefreshCounter() {
		refreshCounter--;
		setRefreshLabel();
		if (refreshCounter == 0) {
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
			Desktop d = Desktop.getDesktop();
			int idx = table.getRowSorter().convertRowIndexToModel(table.getSelectedRow());
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
		if (e.getButton() == MouseEvent.BUTTON3) {
			int i = table.rowAtPoint(e.getPoint());
			if (i >= 0 && i < table.getRowCount()) {
				table.getSelectionModel().setSelectionInterval(i, i);
				menu.show(table, e.getX(), e.getY());
			}
		}
	}
	private void doQuestionClick() {
		if (table.getSelectedRow() >= 0) {
			int idx = table.getRowSorter().convertRowIndexToModel(table.getSelectedRow());
			SummaryEntry se = model.questions.get(idx);
			se.markRead = true;
			model.fireTableRowsUpdated(idx, idx);
		}		
	}
	protected void doRetrieve(final int page) {
		List<SwingWorker<Void, Void>> workers = new LinkedList<SwingWorker<Void, Void>>();
		for (int i = 0; i < siteUrls.length; i++) {
			JCheckBox cb = siteUrls[i];
			JTextField tf = tags[i];
			JLabel lbl = status[i];
			if (cb.isSelected()) {
				go.setIcon(rolling);
				go.setEnabled(false);
				more.setEnabled(false);
				refreshTimer.stop();
				
				final String tgs = tf.getText();
				final String sorts = (String)sort.getSelectedItem();
				final String siteStr = (String)cb.getText();
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
					if (tgs.length() > 0) {
						String tgs1 = tgs.replaceAll("\\s", "+");
						data = SOPageParsers.getQuestionsData(siteStr, tgs1, sorts, page);
					} else {
						data = SOPageParsers.getQuestionsData(siteStr, null, sorts, page);
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
				String st = siteStr;
				if (st.startsWith("http://")) {
					st = st.substring(7);
				}
				for (SummaryEntry e : summary) {
					e.site = st;
				}
				int updated = 0;
				int newer = 0;
				if (!mergeVal) {
					model.questions.addAll(summary);
				} else {
					for (SummaryEntry e : summary) {
						int i = 0;
						boolean exists = false;
						for (SummaryEntry m : new LinkedList<SummaryEntry>(model.questions)) {
							if (e.site.equals(m.site) && e.id.equals(m.id)) {
								model.questions.set(i, e);
								if (e.time > m.time) {
									updated++;
								} else {
									e.markRead = m.markRead;
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
				
				statusLabel.setText(String.format("U: %d, N: %d", updated, newer));
				if (retrieveWip.decrementAndGet() == 0) {
					// the last will restore the button status
					if (!summary.isEmpty() && !adjusted) {
						autoResizeColWidth(table, model);
						adjusted = true;
					}
					go.setIcon(null);
					go.setEnabled(true);
					more.setEnabled(true);
					// continue the refresh loop if it was selected
					if (refresh.isSelected()) {
						refreshCounter = REFRESH_TIME;
						setRefreshLabel();
						refreshTimer.start();
					}
					totalLabel.setText(String.format("Total: %d", model.questions.size()));
				}
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

	/**
	 * Saves or loads the values for various fields annotated with savevalue.
	 * @param save if true the values are saved
	 * @param p the properties to load/save from
	 */
	private void saveLoadValues(boolean save, Properties p) {
		Class<?> clazz = this.getClass();
		for (Field f : clazz.getDeclaredFields()) {
			SaveValue a = f.getAnnotation(SaveValue.class);
			if (a != null) {
				try {
					Object o = f.get(this);
					if (o != null && Object[].class.isAssignableFrom(o.getClass())) {
						Object[] objs = (Object[])o;
						for (int i = 0; i < objs.length; i++) {
							doObjectLoadSave(save, p, f, objs[i], i);
						}
					} else {
						doObjectLoadSave(save, p, f, o, 0);
					}
				} catch (NumberFormatException ex) {
					// ignored
				} catch (IllegalArgumentException ex) {
					// ignored
				} catch (IllegalAccessException ex) {
					// ignored
				}
			}
		}
	}
	private void doObjectLoadSave(boolean save, Properties p, Field f, Object o, int index) {
		if (o instanceof JTextField) {
			JTextField v = (JTextField)o;
			if (save) {
				String s = v.getText();
				p.setProperty(f.getName() + index, s != null ? s : "");
			} else {
				v.setText(p.getProperty(f.getName() + index));
			}
		} else
		if (o instanceof JComboBox) {
			JComboBox v = (JComboBox)o;
			if (save) {
				if (v.isEditable()) {
					p.setProperty(f.getName() + index, v.getSelectedItem() != null ? v.getSelectedItem().toString() : "");
				} else {
					p.setProperty(f.getName() + index, Integer.toString(v.getSelectedIndex()));
				}
			} else {
				String s = p.getProperty(f.getName() + index);
				if (v.isEditable()) {
					v.setSelectedItem(s);
				} else {
					v.setSelectedIndex(s != null && s.length() > 0 ? Integer.parseInt(s) : -1);
				}
			}
		} else
		if (o instanceof JRadioButton) {
			JRadioButton v = (JRadioButton)o;
			if (save) {
				p.setProperty(f.getName() + index, v.isSelected() ? "true" : "false");
			} else {
				v.setSelected("true".equals(p.getProperty(f.getName() + index)));
			}
		}
		if (o instanceof JCheckBox) {
			JCheckBox v = (JCheckBox)o;
			if (save) {
				p.setProperty(f.getName() + index, v.isSelected() ? "true" : "false");
			} else {
				v.setSelected("true".equals(p.getProperty(f.getName() + index)));
			}
		}
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
				saveLoadValues(false, p);
				// restore default sort order
				DefaultRowSorter<?, ?> drs = (DefaultRowSorter<?, ?>)table.getRowSorter();
				String sortKey = p.getProperty("SortKey");
				String sortIndex = p.getProperty("SortIndex");
				if (sortKey != null && sortIndex != null) {
					int j = Integer.parseInt(sortIndex);
					drs.setSortKeys(Collections.singletonList(new SortKey(j, SortOrder.valueOf(sortKey))));
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
			FileOutputStream out = new FileOutputStream("config.xml");
			saveLoadValues(true, p);
			DefaultRowSorter<?, ?> drs = (DefaultRowSorter<?, ?>)table.getRowSorter();
			List<? extends SortKey> list = drs.getSortKeys();
			if (list.size() > 0) {
				p.setProperty("SortKey", list.get(0).getSortOrder().name());
				p.setProperty("SortIndex", Integer.toString(list.get(0).getColumn()));
			}
			try {
				p.storeToXML(out, "");
			} finally {
				out.close();
			}
		} catch (IOException ex) {
			
		}
	}
}
