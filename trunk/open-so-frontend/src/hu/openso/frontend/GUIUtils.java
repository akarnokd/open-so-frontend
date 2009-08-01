package hu.openso.frontend;

import java.awt.Component;
import java.lang.reflect.Field;
import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
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
	public static void saveLoadValues(Object instance, boolean save, Properties p, int panelIndex) {
		Class<?> clazz = instance.getClass();
		for (Field f : clazz.getDeclaredFields()) {
			SaveValue a = f.getAnnotation(SaveValue.class);
			if (a != null) {
				try {
					Object o = f.get(instance);
					if (o != null && Object[].class.isAssignableFrom(o.getClass())) {
						Object[] objs = (Object[])o;
						for (int i = 0; i < objs.length; i++) {
							doObjectLoadSave(save, p, f, objs[i], i, panelIndex);
						}
					} else {
						doObjectLoadSave(save, p, f, o, 0, panelIndex);
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
	protected static void doObjectLoadSave(boolean save, Properties p, Field f, Object o, int index, int panelIndex) {
		if (o instanceof JTextField) {
			JTextField v = (JTextField)o;
			if (save) {
				String s = v.getText();
				p.setProperty("P" + panelIndex + "-" + f.getName() + index, s != null ? s : "");
			} else {
				v.setText(p.getProperty("P" + panelIndex + "-" + f.getName() + index));
			}
		} else
		if (o instanceof JComboBox) {
			JComboBox v = (JComboBox)o;
			if (save) {
				if (v.isEditable()) {
					p.setProperty("P" + panelIndex + "-" + f.getName() + index, v.getSelectedItem() != null ? v.getSelectedItem().toString() : "");
				} else {
					p.setProperty("P" + panelIndex + "-" + f.getName() + index, Integer.toString(v.getSelectedIndex()));
				}
			} else {
				String s = p.getProperty("P" + panelIndex + "-" + f.getName() + index);
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
				p.setProperty("P" + panelIndex + "-" + f.getName() + index, v.isSelected() ? "true" : "false");
			} else {
				v.setSelected("true".equals(p.getProperty("P" + panelIndex + "-" + f.getName() + index)));
			}
		}
		if (o instanceof JCheckBox) {
			JCheckBox v = (JCheckBox)o;
			if (save) {
				p.setProperty("P" + panelIndex + "-" + f.getName() + index, v.isSelected() ? "true" : "false");
			} else {
				v.setSelected("true".equals(p.getProperty("P" + panelIndex + "-" + f.getName() + index)));
			}
		}
	}
}
