package hu.openso.frontend;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.GroupLayout.Alignment;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class QuestionsGUI extends JFrame {
	private static final long serialVersionUID = 5676803531378664660L;
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
				return "<html><font style='font-size: 16pt; font-weight: bold; color: " + color + ";'>" + replaceEntities(se.title);
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
	String replaceEntities(String s) {
		return s.replaceAll("&ldquo;", "\u201C")
		.replaceAll("&rdquo;", "\u201D")
		.replaceAll("&lsquo;", "\u2018")
		.replaceAll("&rsquo;", "\u2019")
		.replaceAll("&gt;", ">")
		.replaceAll("&amp;", "&")
		.replaceAll("&lt;", "<");
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
	JTable table;
	QuestionModel model;
	JButton go;
	JFormattedTextField page;
	JComboBox url;
	JTextField tags;
	ImageIcon rolling;
	ImageIcon okay;
	ImageIcon unknown;
	ImageIcon wiki;
	JComboBox sort;
	JButton more;
	JButton clear;
	boolean adjusted;
	JPopupMenu menu;
	JCheckBox merge;
	JLabel status;
	public QuestionsGUI() {
		super("Questions");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				exec.shutdown();
			}
		});
		
		rolling = new ImageIcon(getClass().getResource("loading.gif"));
		okay = new ImageIcon(getClass().getResource("ok.png"));
		unknown = new ImageIcon(getClass().getResource("unknown.png"));
		wiki = new ImageIcon(getClass().getResource("wiki.png"));
		
		siteIcons.put("stackoverflow.com", new ImageIcon(getClass().getResource("so.png")));
		siteIcons.put("serverfault.com", new ImageIcon(getClass().getResource("sf.png")));
		siteIcons.put("superuser.com", new ImageIcon(getClass().getResource("su.png")));
		siteIcons.put("meta.stackoverflow.com", new ImageIcon(getClass().getResource("meta.png")));
		
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
		JScrollPane scroll = new JScrollPane(table);
		go = new JButton("Get First");
		ActionListener doRetrieveAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				page.setValue(1);
				doRetrieve(1);
			}
		};
		go.addActionListener(doRetrieveAction);
		url = new JComboBox(new String[] { 
			"http://stackoverflow.com", 
			"http://meta.stackoverflow.com",
			"http://serverfault.com",
			"http://superuser.com" 
		});
		url.setSelectedIndex(0);
//		url.addActionListener(doRetrieveAction);
		
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
		
		tags = new JTextField(25);
		tags.addActionListener(doRetrieveAction);
		
		merge = new JCheckBox("Merge");
		merge.setSelected(true);
		status = new JLabel("Welcome to Open Stack Overflow Frontend");
		
		gl.setHorizontalGroup(
			gl.createParallelGroup()
			.addGroup(
				gl.createSequentialGroup()
				.addComponent(url)
				.addComponent(tags)
				.addComponent(sort, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(merge, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(go)
				.addComponent(page, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(more)
				.addComponent(clear)
			)
			.addComponent(scroll)
			.addComponent(status)
		);
		gl.setVerticalGroup(
			gl.createSequentialGroup()
			.addGroup(
				gl.createParallelGroup(Alignment.BASELINE)
				.addComponent(url)
				.addComponent(tags)
				.addComponent(sort)
				.addComponent(merge)
				.addComponent(go)
				.addComponent(page)
				.addComponent(more)
				.addComponent(clear)
			)
			.addComponent(scroll)
			.addComponent(status)
		);
		gl.linkSize(SwingConstants.VERTICAL, url, tags, sort, go, more, page, clear);
		pack();
		setLocationRelativeTo(null);
		
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
		Integer i = (Integer)page.getValue();
		if (i != null) {
			doRetrieve(i + 1);
			page.setValue(i + 1);
		}
	}
	void doTableClicked(MouseEvent e) {
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
	protected void doRetrieve(final int page) {
		go.setIcon(rolling);
		go.setEnabled(false);
		more.setEnabled(false);
		final String tgs = tags.getText();
		final String sorts = (String)sort.getSelectedItem();
		final String siteStr = (String)url.getSelectedItem();
		final boolean mergeVal = merge.isSelected();
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
				int s0 = model.questions.size();
				if (!mergeVal) {
					model.questions.addAll(summary);
				} else {
					for (SummaryEntry e : summary) {
						int i = 0;
						boolean exists = false;
						for (SummaryEntry m : new LinkedList<SummaryEntry>(model.questions)) {
							if (e.site.equals(m.site) && e.id.equals(m.id)) {
								model.questions.set(i, e);
								exists = true;
							}
							i++;
						}
						if (!exists) {
							model.questions.add(e);
						}
					}
				}
				model.fireTableDataChanged();
				if (!summary.isEmpty() && !adjusted) {
					autoResizeColWidth(table, model);
					adjusted = true;
				}
				go.setIcon(null);
				go.setEnabled(true);
				more.setEnabled(true);
				status.setText(String.format("Total: %d, difference: %d", model.questions.size(), model.questions.size() - s0));
			}
		};
		worker.execute();
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

}
