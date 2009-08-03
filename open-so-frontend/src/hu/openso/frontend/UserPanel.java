/*
 * Classname            : hu.openso.frontend.UserPanel
 * Version information  : 1.0
 * Date                 : 2009.08.03.
 * Copyright notice     : GE Consumer & Industrial
 */

package hu.openso.frontend;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;

/**
 * @author karnokd, 2009.08.03.
 * @version $Revision 1.0$
 */
public class UserPanel extends JPanel {
	private static final long serialVersionUID = -3203777741625037787L;
	/** The associatged tab title component. */
	protected TitleWithClose tabTitle;
	/**
	 * The user context object.
	 */
	protected final FrontendContext uctx;
	@SaveValue
	JTextField[] userId;
	String[] sites = { 
			"stackoverflow.com", "meta.stackoverflow.com",
			"serverfault.com", "superuser.com"
	};
	private JLabel userName;
	private int refreshTimeLimit = 20;
	private int refreshTimeCount;
	/** The page refresh timer. */
	private Timer refreshTimer;
	@SaveValue
	JCheckBox refresh;
	/** The retrieve button. */
	private JButton retrieve;
	/** The current user profile. */
	protected UserProfile[] up;
	/** The avatar. */
	protected JLabel avatar;
	private JPopupMenu avatarMenu;
	protected final AtomicInteger retrieveWip = new AtomicInteger(0);
	/** The reputation panel for this user settings. */
	protected ReputationPanel repPanel;
	/** Avatar large images cache. */
	protected static Map<String, ImageIcon> avatarLargeImages = new ConcurrentHashMap<String, ImageIcon>();
	/**
	 * Constructor. Builds the panel.
	 */
	public UserPanel(FrontendContext uctx) {
		this.uctx = uctx;
		init();
	}
	/**
	 * Initialize the panel elements.
	 */
	private void init() {
		GroupLayout gl = new GroupLayout(this);
		setLayout(gl);
		gl.setAutoCreateContainerGaps(true);
		gl.setAutoCreateGaps(true);
		
		// load icons
		List<Component> compsToLink = new ArrayList<Component>();

		up = new UserProfile[sites.length];
		
		userId = new JTextField[sites.length];
		
		ParallelGroup pg = gl.createParallelGroup(Alignment.BASELINE);
		SequentialGroup sg = gl.createSequentialGroup();
		for (int i = 0; i < sites.length / 2; i++) {
			createSiteBoxesFor(pg, sg, i, compsToLink);
		}
		ParallelGroup pg1 = gl.createParallelGroup(Alignment.BASELINE);
		SequentialGroup sg1 = gl.createSequentialGroup();
		for (int i = sites.length / 2; i < sites.length; i++) {
			createSiteBoxesFor(pg1, sg1, i, compsToLink);
		}
		
		userName = new JLabel();
		userName.setHorizontalAlignment(JLabel.CENTER);
		userName.setFont(userName.getFont().deriveFont(20.0f));
		userName.setBackground(new Color(0xE0E0E0));
		userName.setOpaque(true);
		retrieve = new JButton(uctx.go);
		retrieve.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doRetrieve();
			}
		});
		refresh = new JCheckBox("Refresh (in " + refreshTimeLimit + ")");
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doToggleRefresh();
			}
		});
		refreshTimer = new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doRefreshTimerTick();
			}
		});
		JButton setAsTabTitle = new JButton("Set tab title");
		setAsTabTitle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doSetAsTabTitle();
			}
		});
		avatar = new JLabel();
		avatar.setHorizontalAlignment(JLabel.CENTER);
		avatar.setVerticalAlignment(JLabel.CENTER);
		
		repPanel = new ReputationPanel(uctx);
		
		gl.setHorizontalGroup(
			gl.createParallelGroup()
			.addGroup(
				gl.createSequentialGroup()
				.addGroup(
					gl.createParallelGroup()
					.addGroup(sg)
					.addGroup(sg1)
				)
				.addGroup(
					gl.createSequentialGroup()
					.addComponent(retrieve, 30, 30, 30)
					.addComponent(refresh)
				)
			)
			.addGroup(
				gl.createSequentialGroup()
				.addComponent(avatar, 128, 128, 128)
				.addGroup(
					gl.createParallelGroup()
					.addGroup(
						gl.createSequentialGroup()
						.addComponent(userName, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
						.addComponent(setAsTabTitle)
					)
					.addComponent(repPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				)
			)
		);
		gl.setVerticalGroup(
			gl.createSequentialGroup()
			.addGroup(
				gl.createParallelGroup()
				.addGroup(
					gl.createSequentialGroup()
					.addGroup(pg)
					.addGroup(pg1)
				)
				.addComponent(retrieve, 25, 25, 25)
				.addComponent(refresh)
			)
			.addGroup(
				gl.createParallelGroup(Alignment.BASELINE)
				.addComponent(avatar, 128, 128, 128)
				.addGroup(
					gl.createSequentialGroup()
					.addGroup(
						gl.createParallelGroup()
						.addComponent(userName, 30, 30, 30)
						.addComponent(setAsTabTitle)
					)
					.addComponent(repPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				)
			)
		);
		compsToLink.add(retrieve);
		if (compsToLink.size() > 1) {
			gl.linkSize(SwingUtilities.VERTICAL, compsToLink.toArray(new Component[0]));
		}
		refreshTimeCount = refreshTimeLimit;
		setRefreshLabel();
		createMenus();
		avatar.addMouseListener(GUIUtils.getMousePopupAdapter(avatar, avatarMenu));
	}
	/**
	 * Creates the site boxes for the given groups
	 * @param pg
	 * @param sg
	 * @param i
	 * @param j
	 */
	private void createSiteBoxesFor(ParallelGroup pg, SequentialGroup sg,
			final int i, List<Component> compsToLink) {
		JLabel uidIcon = new JLabel("", uctx.siteIcons.get(sites[i]), JLabel.LEFT);

		uidIcon.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				openSite(sites[i]);
			}
		});
		uidIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		uidIcon.setToolTipText("Open " + sites[i] + " in browser");

		JLabel uidLabel = new JLabel("ID@" + sites[i]);
		
		userId[i] = new JTextField(8);
		
		sg.addComponent(uidIcon, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
		sg.addComponent(uidLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
		sg.addComponent(userId[i], GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE);

		pg.addComponent(uidIcon);
		pg.addComponent(uidLabel);
		pg.addComponent(userId[i]);

		compsToLink.add(uidIcon);
		compsToLink.add(uidLabel);
		compsToLink.add(userId[i]);
}
	/**
	 * Open the site URL in the browser.
	 * @param site the site without the http://
	 */
	protected void openSite(String site) {
		Desktop d = Desktop.getDesktop();
		if (d != null) {
			try {
				d.browse(new URI("http://" + site));
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (URISyntaxException ex) {
				ex.printStackTrace();
			}
		}
	}
	/**
	 * 
	 */
	private void createMenus() {
		avatarMenu = new JPopupMenu();
		
		JMenuItem copyUrl = new JMenuItem("Copy URL");
		copyUrl.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doCopyAvatarUrl();
			}
		});
		JMenuItem copyImage = new JMenuItem("Copy Image");
		copyImage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doCopyAvatarImage();
			}
		});
		
		
		avatarMenu.add(copyUrl);
		avatarMenu.add(copyImage);

	}
	/**
	 * Copy the avatar image to the clipboard.
	 */
	protected void doCopyAvatarImage() {
		if (isValidUser()) {
			new ImageSelection().exportToClipboard(avatar, getToolkit().getSystemClipboard(),
		            TransferHandler.COPY);
		}
	}
	/**
	 * Copy the avatar URL to the clipboard
	 */
	protected void doCopyAvatarUrl() {
		if (isValidUser()) {
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(up[0].avatarUrl), new ClipboardOwner() {
				@Override
				public void lostOwnership(Clipboard clipboard, Transferable contents) {
					// nothing
				}
			});		
		}
		
	}
	/**
	 * Set user name as the tab title
	 */
	protected void doSetAsTabTitle() {
		if (isValidUser()) {
			tabTitle.setTitle(up[0].name);
		}
	}
	/**
	 * Returns true if the currently loaded user data is valid.
	 * @return valid?
	 */
	protected boolean isValidUser() {
		return up[0] != null && up[0].avatarUrl != null;
	}
	/**
	 * Refresh timer tick method.
	 */
	protected void doRefreshTimerTick() {
		refreshTimeCount--;
		setRefreshLabel();
		if (refreshTimeCount <= 0) {
			refreshTimer.stop();
			doRetrieve();
		}
	}
	/**
	 * Set remaining time on the refresh timer.
	 */
	private void setRefreshLabel() {
		refresh.setText("Refresh (in " + refreshTimeCount + "s)");
	}
	/**
	 * Toggle refresh timer on/off.
	 */
	protected void doToggleRefresh() {
		if (refresh.isSelected()) {
			refreshTimer.start();
		} else {
			refreshTimer.stop();
		}
	}
	/**
	 * Retrieve the user's profile page(s).
	 */
	protected void doRetrieve() {
		List<String> siteIds = new ArrayList<String>();
		List<String> userIds = new ArrayList<String>();
		for (int i = 0; i < userId.length; i++) {
			if (!userId[i].getText().isEmpty()) {
				siteIds.add(sites[i]);
				userIds.add(userId[i].getText());
			}
		}
		retrieveWip.set(siteIds.size());
		boolean once = true;
		for (int i = 0; i < siteIds.size(); i++) {
			if (once) {
				userName.setIcon(null);
				retrieve.setIcon(uctx.rolling);
				retrieve.setEnabled(false);
				once = false;
			}
			final String sid = siteIds.get(i);
			final String uid = userIds.get(i);
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
					setUserProfileToFields(upl, index);
					if (retrieveWip.decrementAndGet() <= 0) {
						retrieve.setIcon(uctx.go);
						retrieve.setEnabled(true);
						if (refresh.isSelected()) {
							refreshTimeCount = refreshTimeLimit;
							setRefreshLabel();
							refreshTimer.start();
						}
						List<UserProfile> ups = new ArrayList<UserProfile>();
						for (int i = 0; i < up.length; i++) {
							if (up[i] != null && up[i].avatarUrl != null) {
								ups.add(up[i]);
							}
						}
						repPanel.userProfiles.clear();
						repPanel.userProfiles.addAll(ups);
						repPanel.invalidate();
						repPanel.repaint();
					}
				}
			}).execute();
		}
	}
	/**
	 * Sets the tab title associated with this panel.
	 * @param tabTitle the tab title.
	 */
	public void setTabTitle(TitleWithClose tabTitle) {
		this.tabTitle = tabTitle;
	}
	/**
	 * Initializes the user panel based on the properties
	 * stored in the Prop.
	 * @param index the panel index for configuration lookup
	 * @param p the properties object
	 */
	public void initPanel(int index, Properties p) {
		// XXX init settings
		GUIUtils.saveLoadValues(this, false, p, "U" + index + "-");
		if (refresh.isSelected()) {
			refreshTimer.start();
		}
		repPanel.initPanel(index, p);
	}
	/**
	 * Saves the panel settings into the properties object
	 * whith the given panel index.
	 * @param index the panel index for configuration lookup
	 * @param p the properties object
	 */
	public void donePanel(int index, Properties p) {
		// XXX save settings
		GUIUtils.saveLoadValues(this, true, p, "U" + index + "-");
		refreshTimer.stop();
		repPanel.donePanel(index, p);
	}
	/**
	 * Retrieve user data for the site and id.
	 * @param site the site
	 * @param id the user id
	 */
	public void openUser(String site, String id) {
		for (int i = 0; i < sites.length; i++) {
			if (sites[i].equals(site)) {
				userId[i].setText(id);
				doRetrieve();
			} else {
				userId[i].setText("");
			}
		}
	}
	/**
	 * @param up
	 */
	protected void setUserProfileToFields(UserProfile upl, int index) {
		this.up[index] = upl;
		// TODO Auto-generated method stub
		if (upl == null || upl.avatarUrl == null) {
			userName.setIcon(uctx.warning);
			return;
		}
		userName.setText(upl.name);
		
		getUserAvatar(upl.avatarUrl);
	}
	/**
	 * Retrieves the user's avatar in the background.
	 */
	protected void getUserAvatar(final String avatarUrl) {
		ImageIcon ic = avatarLargeImages.get(avatarUrl);
		if (ic != null) {
			avatar.setIcon(ic);
			return;
		}
		avatar.setIcon(uctx.rolling); // progress indication
		GUIUtils.getWorker(new WorkItem() {
			ImageIcon icon;
			@Override
			public void run() {
				try {
					BufferedImage img = ImageIO.read(new URL(avatarUrl));
					icon = new ImageIcon(img);
					avatarLargeImages.put(avatarUrl, icon);
				} catch (IOException ex) {
					icon =  uctx.unknown;
					ex.printStackTrace();
				}
			}
			@Override
			public void done() {
				avatar.setIcon(icon);
			}
		}).execute();
	}
}
