/*
 * Classname            : hu.openso.frontend.ReputationPanel
 * Version information  : 1.0
 * Date                 : 2009.08.03.
 * Copyright notice     : GE Consumer & Industrial
 */

package hu.openso.frontend;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.TransferHandler;

/**
 * Custom rendered component for displaying reputation information
 * about an user - statically, e.g. needs external refresh source.
 * @author karnokd, 2009.08.03.
 * @version $Revision 1.0$
 */
public class ReputationPanel extends JComponent {
	private static final long serialVersionUID = -5352384801697098524L;
	protected final FrontendContext fctx;
	public final List<UserProfile> userProfiles = new ArrayList<UserProfile>();
	int awidth = 200;
	DecimalFormat df = new DecimalFormat("#,###");
	/** Invert the color scheme? */
	private int refreshTimeLimit = 20;
	private int refreshTimeCount;
	/** The page refresh timer. */
	private Timer refreshTimer;
	private JPopupMenu repPopup;
	@SaveValue
	JCheckBoxMenuItem refreshToggle;
	protected final AtomicInteger retrieveWip = new AtomicInteger(0);
	@SaveValue
	JCheckBoxMenuItem invertColor;
	@SaveValue
	JCheckBoxMenuItem refreshFeedbackToggle;

	public ReputationPanel(FrontendContext fctx) {
		this.fctx = fctx;
		setOpaque(true);
		
		repPopup = new JPopupMenu();
		invertColor = new JCheckBoxMenuItem("Invert color scheme");
		invertColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				repaint();
			}
		});
		refreshToggle = new JCheckBoxMenuItem("Auto refresh");
		refreshToggle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doToggleRefresh();
			}
		});
		
		refreshFeedbackToggle = new JCheckBoxMenuItem("Indicate time to refresh");
		refreshFeedbackToggle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				repaint();
			}
		});
		
		JMenuItem copyImage = new JMenuItem("Copy image");
		copyImage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doCopyImage();
			}
		});

		addMouseListener(GUIUtils.getMousePopupAdapter(this, repPopup));
		
		repPopup.add(invertColor);
		repPopup.add(refreshToggle);
		repPopup.add(refreshFeedbackToggle);
		repPopup.addSeparator();
		repPopup.add(copyImage);
		refreshTimer = new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doTimerTick();
			}
		});
		refreshTimeCount = refreshTimeLimit;
	}
	/**
	 * Copy the reputation panel image to clipboard.
	 */
	protected void doCopyImage() {
		BufferedImage bimg = new BufferedImage(getPreferredSize().width, getPreferredSize().height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = bimg.createGraphics();
		paint(g2);
		g2.dispose();
		JLabel lbl = new JLabel(new ImageIcon(bimg));
		new ImageSelection().exportToClipboard(lbl, getToolkit().getSystemClipboard(), TransferHandler.COPY);
	}
	/**
	 * 
	 */
	protected void doTimerTick() {
		// TODO Auto-generated method stub
		refreshTimeCount--;
		if (refreshTimeCount <= 0) {
			refreshTimer.stop();
			doRefreshReputation();
		}
		repaint();
	}
	/**
	 * 
	 */
	private void doRefreshReputation() {
		boolean once = true;
		for (int i = 0; i < userProfiles.size(); i++) {
			if (once) {
				once = false;
			}
			UserProfile up = userProfiles.get(i);
			final String sid = up.site;
			final String uid = up.id;
			final int index = i;
			GUIUtils.getWorker(new WorkItem() {
				private UserProfile upl;
				@Override
				public void run() {
					try {
						byte[] data = SOPageParsers.getAUserData("http://" + sid, uid);
						upl = SOPageParsers.parseUserProfileStats(data);
					} catch (Throwable ex) {
						ex.printStackTrace();
					}
				}
				@Override
				public void done() {
					if (upl != null) {
						upl.site = sid;
						upl.id = uid;
					}
					userProfiles.set(index, upl);
					repaint();
					if (retrieveWip.decrementAndGet() <= 0) {
						if (refreshToggle.isSelected()) {
							refreshTimeCount = refreshTimeLimit;
							refreshTimer.start();
						}
					}
				}
			}).execute();
		}
	}
	/**
	 * 
	 */
	protected void doToggleRefresh() {
		if (refreshToggle.isSelected()) {
			refreshTimer.start();
		} else {
			refreshTimer.stop();
		}
	}
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(awidth * userProfiles.size(), 54);
	}
	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		boolean invert = invertColor.isSelected();
		g2.setColor(invert ? Color.BLACK : Color.WHITE);
		g2.fillRect(0, 0, getWidth(), getHeight());
		int x = 0;
		int i = 0;
		for (UserProfile up : userProfiles) {
			if (refreshFeedbackToggle.isSelected()) {
				g2.setColor(invert ? new Color(0x600000) : new Color(0xFFE0E0));
				g2.fillRect(x, 0, awidth * (refreshTimeLimit - refreshTimeCount) / refreshTimeLimit, getHeight());
			}
			ImageIcon siteIcon = fctx.siteIcons.get(up.site);
			
			int sy = (getHeight() - siteIcon.getIconHeight()) / 2;
			siteIcon.paintIcon(this, g, x + 1, sy);
			
			Font f = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
			g2.setFont(f);
			FontMetrics fm1 = g2.getFontMetrics();
			g2.setColor(invert ? new Color(0xBBBBBB) : new Color(0x555555));
			if (i < userProfiles.size() - 1) {
				g2.drawLine(x + awidth - 1, 0, x + awidth - 1, getHeight() - 1);
			}
			int rx = x + siteIcon.getIconWidth() + 3;
			g2.drawString("reputation", rx, 12);
			
			int bl = fm1.stringWidth("badges");
			g2.drawString("badges", x + awidth - 3 - bl, 12);
			
			int y = 50;
			String badgeChar = "\u25CF";
			int bcw = fm1.stringWidth(badgeChar);
			for (BadgeLevel bgl : BadgeLevel.values()) {
				int cnt = up.getBadgeCount(bgl);
				String v = df.format(cnt);
				int w = fm1.stringWidth(v);
				g2.setColor(new Color(bgl.color));
				g2.drawString(badgeChar, x + awidth - 3 - bcw, y);
				g2.setColor(!invert ? Color.BLACK : Color.WHITE);
				g2.drawString(v, x + awidth - 6 - bcw - w, y);
				y -= 12;
			}
			
			f = f.deriveFont(Font.BOLD, 36.0f);
			g2.setFont(f);
			
			g2.drawString(df.format(up.reputation), rx + 2, getHeight() - g2.getFontMetrics().getDescent());
			
			
			x += awidth;
			i++;
		}
	}
	/**
	 * Initializes the user panel based on the properties
	 * stored in the Prop.
	 * @param index the panel index for configuration lookup
	 * @param p the properties object
	 */
	public void initPanel(int index, Properties p) {
		// XXX init settings
		GUIUtils.saveLoadValues(this, false, p, "R" + index + "-");
		if (refreshToggle.isSelected()) {
			refreshTimer.start();
		}
	}
	/**
	 * Saves the panel settings into the properties object
	 * whith the given panel index.
	 * @param index the panel index for configuration lookup
	 * @param p the properties object
	 */
	public void donePanel(int index, Properties p) {
		// XXX save settings
		GUIUtils.saveLoadValues(this, true, p, "R" + index + "-");
		refreshTimer.stop();
	}
}
