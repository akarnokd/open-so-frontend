/*
 * Classname            : hu.openso.frontend.IgnoreListGUI
 * Version information  : 1.0
 * Date                 : 2009.07.31.
 * Copyright notice     : GE Consumer & Industrial
 */

package hu.openso.frontend;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/**
 * @author karnokd, 2009.07.31.
 * @version $Revision 1.0$
 */
public class IgnoreListGUI extends JFrame {
	private static final long serialVersionUID = -7772325341995514823L;
	final Map<String, String> ignoreMap;
	class IgnoreList extends AbstractTableModel {
		private static final long serialVersionUID = -7627505233354740030L;
		String[] columnNames = { "Site / Question ID", "Title" };
		Class<?>[] columnClasses = { String.class, String.class };
		final List<Map.Entry<String, String>> ignoreList = new ArrayList<Map.Entry<String, String>>();
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnClasses[columnIndex];
		}
		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}
		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public int getRowCount() {
			return ignoreList.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case 0: return ignoreList.get(rowIndex).getKey();
			case 1: return ignoreList.get(rowIndex).getValue();
			}
			return null;
		}
	}
	IgnoreList model;
	JTable table;
	JPopupMenu menu;
	JLabel count;
	public IgnoreListGUI(Map<String, String> ignoreMap) {
		super("Ignore list");
		this.ignoreMap = ignoreMap;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		model = new IgnoreList();
		table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				doMousePopupClicked(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				doMousePopupClicked(e);
			}
		});
		count = new JLabel();
		JScrollPane scroll = new JScrollPane(table);
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		c.add(scroll, BorderLayout.CENTER);
		c.add(count, BorderLayout.PAGE_END);
		menu = new JPopupMenu();
		JMenuItem remove = new JMenuItem("Remove");
		remove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doRemove();
			}
		});
		menu.add(remove);
		pack();
	}
	/**
	 * 
	 */
	protected void doRemove() {
		int idx0 = table.getSelectedRow();
		if (idx0 >= 0) {
			int idx = table.convertColumnIndexToModel(idx0);
			Map.Entry<String, String> e = model.ignoreList.get(idx);
			ignoreMap.remove(e.getKey());
			model.ignoreList.remove(idx);
			model.fireTableRowsDeleted(idx, idx);
			count.setText(String.format("Entries: %d", ignoreMap.size()));
		}
	}
	/**
	 * @param e
	 */
	protected void doMousePopupClicked(MouseEvent e) {
		if (e.isPopupTrigger()) {
			int i = table.rowAtPoint(e.getPoint());
			if (i >= 0 && i < table.getRowCount()) {
				table.getSelectionModel().setSelectionInterval(i, i);
				menu.show(table, e.getX(), e.getY());
			}
		}
	}
	public void remapIgnores() {
		model.ignoreList.clear();
		model.ignoreList.addAll(ignoreMap.entrySet());
		model.fireTableDataChanged();
		count.setText(String.format("Entries: %d", ignoreMap.size()));
	}
	/**
	 * 
	 */
	public void autoSizeTable() {
		GUIUtils.autoResizeColWidth(table, model);
		
	}
}
