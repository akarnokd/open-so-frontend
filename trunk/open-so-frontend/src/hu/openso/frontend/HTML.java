/*
 * Classname            : hu.openso.frontend.HTML
 * Version information  : $Revision 1.0$
 * Date                 : 2009.04.17.
 * Copyright notice     : David Karnok
 */

package hu.openso.frontend;

/**
 * HTML related utility functions.
 * @author karnokd, 2009.04.17.
 * @version $Revision 1.0$
 */
public final class HTML {
	/** Utility class. */
	private HTML() {
		// utility class
	}
	/**
	 * Connverts all sensitive characters to its HTML entity equivalent.
	 * @param s the string to convert, can be null
	 * @return the converted string, or an empty string
	 */
	public static String toHTML(String s) {
		if (s != null) {
			StringBuilder b = new StringBuilder(s.length());
			for (int i = 0, count = s.length(); i < count; i++) {
				char c = s.charAt(i);
				switch (c) {
				case '<':
					b.append("&lt;");
					break;
				case '>':
					b.append("&gt;");
					break;
				case '\'':
					b.append("&#39;");
					break;
				case '"':
					b.append("&quot;");
					break;
				case '&':
					b.append("&amp;");
					break;
				default:
					b.append(c);
				}
			}
			return b.toString();
		}
		return "";
	}
	/**
	 * Connverts all sensitive characters to its HTML entity equivalent then for javascript string parameters.
	 * @param s the string to convert, can be null
	 * @return the converted string, or an empty string
	 */
	public static String toHTMLJS(String s) {
		if (s != null) {
			StringBuilder b = new StringBuilder(s.length());
			for (int i = 0, count = s.length(); i < count; i++) {
				char c = s.charAt(i);
				switch (c) {
				case '<':
					b.append("&lt;");
					break;
				case '>':
					b.append("&gt;");
					break;
				case '\'':
					b.append("&#39;");
					break;
				case '"':
					b.append("\\&quot;");
					break;
				case '&':
					b.append("&amp;");
					break;
				case '\\':
					b.append('\\').append('\\');
					break;
				default:
					b.append(c);
				}
			}
			return b.toString();
		}
		return "";
	}

}
