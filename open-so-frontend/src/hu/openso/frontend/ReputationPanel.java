/*
 * Classname            : hu.openso.frontend.ReputationPanel
 * Version information  : 1.0
 * Date                 : 2009.08.03.
 * Copyright notice     : GE Consumer & Industrial
 */

package hu.openso.frontend;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
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
	private int refreshTimeLimit = 30;
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
	/** Value change noticed. */
	boolean noticed;
	protected final Set<ActionListener> onRefreshCompleted = new LinkedHashSet<ActionListener>(); 
	protected Map<String, ImageIcon> avatarLargeImages = new ConcurrentHashMap<String, ImageIcon>();
	protected Map<String, String> avatarLargeImagesLoading = new ConcurrentHashMap<String, String>();
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
		
		JMenuItem refreshNow = new JMenuItem("Refresh now");
		refreshNow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doRefreshReputation();
			}
		});
		
		JMenuItem openUser = new JMenuItem("Open user");
		openUser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doOpenUser();
			}
		});
		
		JMenuItem openUserHere = new JMenuItem("Open user here");
		openUserHere.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doOpenUserHere();
			}
		});
		
		repPopup.add(refreshNow);
		repPopup.addSeparator();
		repPopup.add(openUser);
		repPopup.add(openUserHere);
		repPopup.addSeparator();
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
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				doMarkRead(e);
			}
		});
	}
	protected void doOpenUserHere() {
		// TODO Auto-generated method stub
		String[] sites = new String[userProfiles.size()];
		String[] ids = new String[userProfiles.size()];
		String name = null;
		for (int i = 0; i < userProfiles.size(); i++) {
			UserProfile up = userProfiles.get(i);
			sites[i] = up.site;
			ids[i] = up.id;
			name = up.name;
		}
		fctx.panelManager.openUser(sites, ids, name);
	}
	protected void doOpenUser() {
		Desktop d = Desktop.getDesktop();
		try {
			for (UserProfile up : userProfiles) {
				d.browse(new URI("http://" + up.site + "/users/" + up.id));
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (URISyntaxException ex) {
			ex.printStackTrace();
		}
	}
	/** Mark elements as read. */
	protected void doMarkRead(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			for (UserProfile up : userProfiles) {
				up.markRead = true;
			}
			repaint();
		}
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
			doRefreshReputation();
		}
		repaint();
	}
	/**
	 * 
	 */
	private void doRefreshReputation() {
		boolean once = true;
		retrieveWip.set(userProfiles.size());
		for (int i = 0; i < userProfiles.size(); i++) {
			if (once) {
				refreshTimer.stop();
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
					updateUserProfile(index, upl);
					repaint();
					if (retrieveWip.decrementAndGet() <= 0) {
						if (refreshToggle.isSelected()) {
							refreshTimeCount = refreshTimeLimit;
							refreshTimer.start();
							repaint();
						}
						fireOnRefreshCompleted(new ActionEvent(ReputationPanel.this, 0, "OnRefreshCompleted"));
					}
				}
			}).execute();
		}
	}
	protected void updateUserProfile(int index, UserProfile upl) {
		if (upl.avatarUrl != null) {
			UserProfile curr = userProfiles.get(index);
			// analize difference
			boolean diff = false;
			upl.repChanged = upl.reputation - curr.reputation; 
			diff |= upl.repChanged != 0;
			for (BadgeLevel bl : BadgeLevel.values()) {
				int bdiff = upl.getBadgeCount(bl) - curr.getBadgeCount(bl);
				upl.badgeChanged.put(bl, bdiff);
				diff |= bdiff != 0;
			}
			upl.markRead = curr.markRead;
			if (diff) {
				upl.markRead = false;
			}
			userProfiles.set(index, upl);
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
				Integer bold = 0;
				if (up.badgeChanged != null) {
					bold = up.badgeChanged.get(bgl); 
				}
				if (!up.markRead && bold != null && bold != 0) {
					g2.setColor(bold < 0 ? Color.RED : Color.GREEN);
				} else {
					g2.setColor(!invert ? Color.BLACK : Color.WHITE);
				}
				g2.drawString(v, x + awidth - 6 - bcw - w, y);
				y -= 12;
			}
			
			f = f.deriveFont(Font.BOLD, 36.0f);
			g2.setFont(f);
			
			if (!up.markRead && up.repChanged != 0) {
				g2.setColor(up.repChanged < 0 ? Color.RED : Color.GREEN);
			} else {
				g2.setColor(!invert ? Color.BLACK : Color.WHITE);
			}
			
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
	public void initPanel(String prefix, Properties p) {
		// XXX init settings
		GUIUtils.saveLoadValues(this, false, p, prefix);
		startTimersIf();
	}
	/**
	 * Saves the panel settings into the properties object
	 * whith the given panel index.
	 * @param index the panel index for configuration lookup
	 * @param p the properties object
	 */
	public void donePanel(String prefix, Properties p) {
		// XXX save settings
		GUIUtils.saveLoadValues(this, true, p, prefix);
		stopTimers();
	}
	/**
	 * Add listener for the on refresh completed event.
	 * @param a the action listener, nulls ignored
	 */
	public void addOnRefreshCompleted(ActionListener a) {
		if (a != null) {
			onRefreshCompleted.add(a);
		}
	}
	/**
	 * Remove listener for the on refresh completed event.
	 * @param a the action listener to remove
	 */
	public void removeOnRefreshCompleted(ActionListener a) {
		onRefreshCompleted.remove(a);
	}
	/**
	 * Fire the on refresh completed event.
	 * @param ae the action event
	 */
	protected void fireOnRefreshCompleted(ActionEvent ae) {
		for (ActionListener al : onRefreshCompleted) {
			al.actionPerformed(ae);
		}
	}
	/**
	 * Retrieves the user's avatar in the background.
	 * @param avatarUrl the URL to the avatar
	 * @param avatar the target label to set its icon
	 * @param resize the optional size parameter to resize the image
	 */
	public void getUserAvatar(final String avatarUrl, final JLabel avatar, final Integer resize) {
		ImageIcon ic = avatarLargeImages.get(avatarUrl);
		if (ic != null) {
			if (resize != null) {
				int ns = resize;
				Image img = ic.getImage();
				BufferedImage bimg = new BufferedImage(ns, ns, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2 = bimg.createGraphics();
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				g2.drawImage(img, 0, 0, ns, ns, null);
				g2.dispose();
				avatar.setIcon(new ImageIcon(bimg));
			} else {
				avatar.setIcon(ic);
			}
			return;
		}
		if (avatarLargeImagesLoading.containsKey(avatarUrl)) {
			return;
		}
		avatar.setIcon(fctx.rolling); // progress indication
		avatarLargeImagesLoading.put(avatarUrl, avatarUrl);
		GUIUtils.getWorker(new WorkItem() {
			ImageIcon icon;
			@Override
			public void run() {
				try {
					BufferedImage img = ImageIO.read(new URL(avatarUrl));
					if (resize != null) {
						int ns = resize;
						BufferedImage bimg = new BufferedImage(ns, ns, BufferedImage.TYPE_INT_ARGB);
						Graphics2D g2 = bimg.createGraphics();
						g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
						g2.drawImage(img, 0, 0, ns, ns, null);
						g2.dispose();
						img = bimg;
					}
					icon = new ImageIcon(img);
					avatarLargeImages.put(avatarUrl, icon);
					avatarLargeImagesLoading.remove(avatarUrl);
				} catch (Throwable ex) {
					icon =  fctx.unknown;
					ex.printStackTrace();
				}
			}
			@Override
			public void done() {
				avatar.setIcon(icon);
			}
		}).execute();
	}
	/** Start refresh timers if they are enabled. */
	public void startTimersIf() {
		if (refreshToggle.isSelected()) {
			refreshTimer.start();
		}
	}
	/** Stop refresh timers. */
	public void stopTimers() {
		refreshTimer.stop();
	}
}
