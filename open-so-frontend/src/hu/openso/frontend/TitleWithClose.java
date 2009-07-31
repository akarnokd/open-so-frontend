package hu.openso.frontend;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;

public class TitleWithClose extends JPanel {
	private static final long serialVersionUID = 2314996247881493085L;
	private final Component component;
	private final JTabbedPane tabbed;

	public TitleWithClose(String title, JTabbedPane tab, Component c) {
		setOpaque(false);
		this.tabbed = tab;
		this.component = c;
		setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 2));
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		add(new JLabel(title));
		add(Box.createHorizontalStrut(5));
		JButton btn = new JButton("X");
		btn.setPreferredSize(new Dimension(17, 17));
		btn.setToolTipText("Close this tab");
		btn.setFocusable(false);
		btn.setBorderPainted(false);
		btn.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		btn.setRolloverEnabled(true);
		btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int i = tabbed.indexOfComponent(component);
				if (i > 0) {
					tabbed.setSelectedIndex(i - 1);
				}
				tabbed.removeTabAt(i);
			}
		});
		add(btn);
	}
}
