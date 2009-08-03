package hu.openso.frontend;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.TransferHandler;

/**
 * Image selection for copy-pasting images to and from clipboard.
 * http://www.java2s
 * .com/Code/Java/Development-Class/SendingImageObjectsthroughtheClipboard.htm
 * 
 * @author karnokd, 2009.08.03.
 * @version $Revision 1.0$
 */
public class ImageSelection extends TransferHandler implements Transferable {
	private static final long serialVersionUID = -371601312296712557L;
	/** The data flawor array. */
	private static final DataFlavor flavors[] = { DataFlavor.imageFlavor };
	/** The source of the image. */
	private JLabel source;
    /** The actual image. */
	private Image image;
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getSourceActions(JComponent c) {
		return TransferHandler.COPY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean canImport(JComponent comp, DataFlavor flavor[]) {
		if (!(comp instanceof JLabel)) {
			return false;
		}
		for (int i = 0, n = flavor.length; i < n; i++) {
			for (int j = 0, m = flavors.length; j < m; j++) {
				if (flavor[i].equals(flavors[j])) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Transferable createTransferable(JComponent comp) {
		// Clear
		source = null;
		image = null;

		if (comp instanceof JLabel) {
			JLabel label = (JLabel) comp;
			Icon icon = label.getIcon();
			if (icon instanceof ImageIcon) {
				image = ((ImageIcon) icon).getImage();
				source = label;
				return this;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean importData(JComponent comp, Transferable t) {
		if (comp instanceof JLabel) {
			JLabel label = (JLabel) comp;
			if (t.isDataFlavorSupported(flavors[0])) {
				try {
					image = (Image) t.getTransferData(flavors[0]);
					ImageIcon icon = new ImageIcon(image);
					label.setIcon(icon);
					return true;
				} catch (UnsupportedFlavorException ignored) {
				} catch (IOException ignored) {
				}
			}
		}
		return false;
	}

	// Transferable
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getTransferData(DataFlavor flavor) {
		if (isDataFlavorSupported(flavor)) {
			return image;
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return flavors;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(DataFlavor.imageFlavor);
	}
	/**
	 * Returns the source object.
	 * @return the source object
	 */
	public JLabel getSource() {
		return source;
	}
}