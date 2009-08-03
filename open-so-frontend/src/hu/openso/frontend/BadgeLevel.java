/*
 * Classname            : hu.openso.frontend.BadgeLevel
 * Version information  : 1.0
 * Date                 : 2009.08.03.
 * Copyright notice     : GE Consumer & Industrial
 */

package hu.openso.frontend;

/**
 * Badge level enumeration.
 * @author karnokd, 2009.08.03.
 * @version $Revision 1.0$
 */
public enum BadgeLevel {
	BRONZE(0xCC9966),
	SILVER(0xC0C0C0),
	GOLD(0xFFCC00)
	;
	public final int color;
	BadgeLevel(int color) {
		this.color = color;
	}
}