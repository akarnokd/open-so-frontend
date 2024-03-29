package hu.openso.frontend;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public final class GUIUtils {
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
//        table.getTableHeader().setReorderingAllowed(false);
 
//        for (int i = 0; i < table.getColumnCount(); i++) {
//            TableColumn column = table.getColumnModel().getColumn(i);
// 
//            column.setCellRenderer(new DefaultTableColour());
//        }
 
        return table;
    }
	/**
	 * Saves or loads the values for various fields annotated with savevalue.
	 * @param save if true the values are saved
	 * @param p the properties to load/save from
	 */
	public static void saveLoadValues(Object instance, boolean save, Properties p, String prefix) {
		Class<?> clazz = instance.getClass();
		for (Field f : clazz.getDeclaredFields()) {
			SaveValue a = f.getAnnotation(SaveValue.class);
			if (a != null) {
				try {
					Object o = f.get(instance);
					if (o != null && Object[].class.isAssignableFrom(o.getClass())) {
						Object[] objs = (Object[])o;
						for (int i = 0; i < objs.length; i++) {
							doObjectLoadSave(save, p, f, objs[i], i, prefix);
						}
					} else {
						doObjectLoadSave(save, p, f, o, 0, prefix);
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
	protected static void doObjectLoadSave(boolean save, Properties p, 
			Field f, Object o, int index, String prefix) {
		if (o instanceof JTextField) {
			JTextField v = (JTextField)o;
			if (save) {
				String s = v.getText();
				p.setProperty(prefix + f.getName() + index, s != null ? s : "");
			} else {
				v.setText(p.getProperty(prefix + f.getName() + index));
			}
		} else
		if (o instanceof JComboBox) {
			JComboBox v = (JComboBox)o;
			if (save) {
				if (v.isEditable()) {
					p.setProperty(prefix + f.getName() + index, v.getSelectedItem() != null ? v.getSelectedItem().toString() : "");
				} else {
					p.setProperty(prefix + f.getName() + index, Integer.toString(v.getSelectedIndex()));
				}
			} else {
				String s = p.getProperty(prefix + f.getName() + index);
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
				p.setProperty(prefix + f.getName() + index, v.isSelected() ? "true" : "false");
			} else {
				v.setSelected("true".equals(p.getProperty(prefix + f.getName() + index)));
			}
		}
		if (o instanceof JCheckBox) {
			JCheckBox v = (JCheckBox)o;
			if (save) {
				p.setProperty(prefix + f.getName() + index, v.isSelected() ? "true" : "false");
			} else {
				v.setSelected("true".equals(p.getProperty(prefix + f.getName() + index)));
			}
		} else
		if (o instanceof JCheckBoxMenuItem) {
			JCheckBoxMenuItem v = (JCheckBoxMenuItem)o;
			if (save) {
				p.setProperty(prefix + f.getName() + index, v.isSelected() ? "true" : "false");
			} else {
				v.setSelected("true".equals(p.getProperty(prefix + f.getName() + index)));
			}
		} else
		if (o instanceof JRadioButtonMenuItem) {
			JRadioButtonMenuItem v = (JRadioButtonMenuItem)o;
			if (save) {
				p.setProperty(prefix + f.getName() + index, v.isSelected() ? "true" : "false");
			} else {
				v.setSelected("true".equals(p.getProperty(prefix + f.getName() + index)));
			}
		}
	}
	/**
	 * Returns a SwingWorker for the given workitem object.
	 * @param work the work item to do using SW.
	 * @return the swing worker object
	 */
	public static SwingWorker<Void, Void> getWorker(final WorkItem work) { 
		return new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				work.run();
				return null;
			}
			@Override
			protected void done() {
				work.done();
			}
		};
	}
	/**
	 * Creates a mouse popup adapter for the given component and popup menu.
	 * @param component the target component, non null
	 * @param popup the popup menu, non null
	 * @return the mouse adapter
	 */
	public static MouseAdapter getMousePopupAdapter(final Component component, final JPopupMenu popup) {
		return new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				handlePopup(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				handlePopup(e);
			}
			private void handlePopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					popup.show(component, e.getX(), e.getY());
				}
			}
		};
	}
}
