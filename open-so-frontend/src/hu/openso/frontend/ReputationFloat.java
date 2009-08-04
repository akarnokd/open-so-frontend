/**
 * 
 */
package hu.openso.frontend;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.apache.commons.codec.binary.Base64;

/**
 * Floating reputation panel.
 * @author karnokd
 */
public class ReputationFloat extends JFrame {
	private static final long serialVersionUID = -8427195099319663967L;
	/** The backing reputation panel. */
	public final ReputationPanel repPanel;
	protected JPopupMenu panelMenu;
	protected JLabel avatar;
	private JPanel mainPanel;
	/** Avatar large images cache. */
	public ReputationFloat(final ReputationPanel repPanel) {
		super(getFirstUserName(repPanel));
		this.repPanel = repPanel;
		repPanel.invertColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				recolorContents(repPanel);
			}
		});
		repPanel.addOnRefreshCompleted(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onRefreshCompleted();
			}
		});
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				doClose();
			}
		});
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		mainPanel = new JPanel();

		mainPanel.setBorder(BorderFactory.createLineBorder(repPanel.invertColor.isSelected() ? Color.WHITE : Color.BLACK));
		mainPanel.setDoubleBuffered(true);
		getContentPane().add(mainPanel);
		GroupLayout gl = new GroupLayout(mainPanel);
		mainPanel.setLayout(gl);
		gl.setAutoCreateContainerGaps(false);
		gl.setAutoCreateGaps(false);
		
		avatar = new JLabel();
		avatar.setHorizontalAlignment(JLabel.CENTER);
		avatar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, repPanel.invertColor.isSelected() ? Color.WHITE : Color.BLACK));
		
		gl.setHorizontalGroup(
			gl.createSequentialGroup()
			.addGap(1)
			.addComponent(avatar, 54, 54, 54)
			.addComponent(repPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addGap(1)
		);
		gl.setVerticalGroup(
			gl.createSequentialGroup()
			.addGap(1)
			.addGroup(
				gl.createParallelGroup()
				.addComponent(avatar, 54, 54, 54)
				.addComponent(repPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			)
			.addGap(1)
		);
		onRefreshCompleted();
		
		panelMenu = new JPopupMenu();
		
		JMenuItem mnuClose = new JMenuItem("Close");
		mnuClose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doClose();
				dispose();
			}
		});
		JMenuItem mnuCloseForget = new JMenuItem("Close and forget");
		mnuCloseForget.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doCloseAndForget();
				dispose();
			}
		});
		JMenuItem mnuMinimize = new JMenuItem("Minimize");
		mnuMinimize.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
                setState (ICONIFIED);
			}
		});
		JMenuItem openUser = new JMenuItem("Open user");
		openUser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				repPanel.doOpenUser();
			}
		});
		
		JMenuItem openUserHere = new JMenuItem("Open user here");
		openUserHere.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				repPanel.doOpenUserHere();
			}
		});
		
		panelMenu.add(mnuMinimize);
		panelMenu.addSeparator();
		panelMenu.add(openUser);
		panelMenu.add(openUserHere);
		panelMenu.addSeparator();
		panelMenu.add(mnuClose);
		panelMenu.add(mnuCloseForget);
		
		avatar.addMouseListener(GUIUtils.getMousePopupAdapter(avatar, panelMenu));
		
		MouseAdapter moveAdapter = getMoveAdapter();
		addMouseListener(moveAdapter);
		addMouseMotionListener(moveAdapter);
		
		avatar.addMouseListener(moveAdapter);
		avatar.addMouseMotionListener(moveAdapter);

// TODO removed drag support from the repPanel to do not conflict with the read notification
		repPanel.addMouseListener(moveAdapter);
		repPanel.addMouseMotionListener(moveAdapter);
		
		setUndecorated(true);
		setAlwaysOnTop(true);
		setSize(859, 58);
		pack();
	}
	private MouseAdapter getMoveAdapter() {
		return new MouseAdapter() {
			int lastX;
			int lastY;
			boolean moveStart;
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					lastX = e.getX();
					lastY = e.getY();
					moveStart = true;
				}
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				moveStart = false;
			}
			@Override
			public void mouseDragged(MouseEvent e) {
				if (moveStart) {
					int dx = e.getX() - lastX;
					int dy = e.getY() - lastY;
					
					ReputationFloat.this.setLocation(ReputationFloat.this.getX() + dx, ReputationFloat.this.getY() + dy);
					
				}
			}
		};
	}
	protected void doClose() {
		repPanel.stopTimers();
	}
	protected void doCloseAndForget() {
		repPanel.fctx.panelManager.unregisterRepFloat(this);
		doClose();
	}
	private static String getFirstUserName(ReputationPanel repPanel) {
		return repPanel.userProfiles.size() > 0 ? repPanel.userProfiles.get(0).name : "";
	}
	protected void onRefreshCompleted() {
		if (repPanel.userProfiles.size() > 0) {
			avatar.setToolTipText(repPanel.userProfiles.get(0).name);
			repPanel.getUserAvatar(repPanel.userProfiles.get(0).avatarUrl, avatar, 54);
			setTitle(getFirstUserName(repPanel));
			repaint();
		}
	}
//	@Override
//	public void paint(Graphics g) {
//		super.paint(g);
//		if (repPanel.invertColor.isSelected()) {
//			g.setColor(Color.WHITE);
//		} else {
//			g.setColor(Color.BLACK);
//		}
//		int w = getWidth() - 1;
//		int h = getHeight() - 1;
//		g.drawRect(0, 0, w, h);
//		g.drawLine(55, 0, 55, 54);
//	}
	public void initPanel(int index, Properties p) {
		Rectangle rect = new Rectangle();
		rect.x = Integer.parseInt(p.getProperty("FX" + index));
		rect.y = Integer.parseInt(p.getProperty("FY" + index));
		rect.width = Integer.parseInt(p.getProperty("FWidth" + index));
		rect.height = Integer.parseInt(p.getProperty("FHeight" + index));
		setBounds(rect);
		String winstat = p.getProperty("FWindowStatus");
		setExtendedState(Integer.parseInt(winstat));
		String ql = p.getProperty("F" + index + "-" + "UserProfiles");
		if (ql != null) {
			try {
				ByteArrayInputStream bin = new ByteArrayInputStream(
						Base64.decodeBase64(ql.getBytes("ISO-8859-1")));
				ObjectInputStream oin = new ObjectInputStream(bin);
				@SuppressWarnings("unchecked")
				List<UserProfile> list = (List<UserProfile>)oin.readObject();
				repPanel.userProfiles.addAll(list);
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (ClassNotFoundException ex) {
				ex.printStackTrace();
			}
		}
		repPanel.initPanel("FR" + index + "-", p);
		onRefreshCompleted();
	}
	public void donePanel(int index, Properties p) {
		repPanel.donePanel("FR" + index + "-", p);
		Rectangle rect = getBounds();
		p.setProperty("FX" + index, Integer.toString(rect.x));
		p.setProperty("FY" + index, Integer.toString(rect.y));
		p.setProperty("FWidth" + index, Integer.toString(rect.width));
		p.setProperty("FHeight" + index, Integer.toString(rect.height));
		p.setProperty("FWindowStatus", Integer.toString(getExtendedState()));
		// save current profile values
		ByteArrayOutputStream bout = new ByteArrayOutputStream(16 * 1024);
		try {
			ObjectOutputStream os = new ObjectOutputStream(bout);
			os.writeObject(repPanel.userProfiles);
			os.close();
			p.setProperty("F" + index + "-" + "UserProfiles", 
					new String(Base64.encodeBase64(bout.toByteArray(), true), "ISO-8859-1"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	private void recolorContents(final ReputationPanel repPanel) {
		mainPanel.setBorder(BorderFactory.createLineBorder(repPanel.invertColor.isSelected() ? Color.WHITE : Color.BLACK));
		avatar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, repPanel.invertColor.isSelected() ? Color.WHITE : Color.BLACK));
		repaint();
	}
}
