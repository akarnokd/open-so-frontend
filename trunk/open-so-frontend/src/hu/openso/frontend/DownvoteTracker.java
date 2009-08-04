/*
 * Classname            : hu.openso.frontend.DownvoteTracker
 * Version information  : 1.0
 * Date                 : 2009.08.04.
 * Copyright notice     : GE Consumer & Industrial
 */

package hu.openso.frontend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/**
 * GUI for tracking potential downvoteds and downvoters
 * @author karnokd, 2009.08.04.
 * @version $Revision 1.0$
 */
public class DownvoteTracker extends JFrame {
	private static final long serialVersionUID = 3094478885868144996L;
	static class DownvoteTarget {
		/** The destination site. */
		String site;
		/** The user identifier. */
		String id;
		/** The associated avatar URL. */
		String avatarUrl;
		/** The user display name. */
		String name;
		/** The reputation before. */
		int repBefore;
		/** The reputation after. */
		int repAfter;
		/** The question id where we saw this user, holds the most recent question activity. */ 
		final List<SummaryEntry> questionsBefore = new ArrayList<SummaryEntry>();
		/** The list of questions after the test. */
		final List<SummaryEntry> questionsAfter = new ArrayList<SummaryEntry>();
		/** The timestamp of the analysis. */
		long analysisTimestamp;
		/** Set to true if this user is detected as a receiver for the downvote. */
		boolean isReceiver;
		/** 
		 * Set to true if this user is detected as a giver for the downvote: 
		 * both can happen between two refreshes!
		 */
		boolean isGiver;
		/** Indicator that the user undestood this entry by clicking on it. */
		boolean understood;
	}
	class DownvoteModel extends AbstractTableModel {
		final List<DownvoteTarget> list = new ArrayList<DownvoteTarget>();
		String[] colNames = {
				
		};
		Class<?>[] colClasses = {
			
		};
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return colClasses[columnIndex];
		}
		@Override
		public String getColumnName(int column) {
			return colNames[column];
		}
		@Override
		public int getColumnCount() {
			return colNames.length;
		}
		@Override
		public int getRowCount() {
			return list.size();
		}
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			DownvoteTarget dt = list.get(rowIndex);
			switch (columnIndex) {
			// TODO Auto-generated method stub
			}
			return null;
		}
	}
	JTable table;
	
	/**
	 * A differentiator algorithm to check for user activity differences based on
	 * their public listed reputation value
	 * @param before the list of questions before
	 * @param after the list of questions after
	 * @param debug print findings instantly
	 */
	public static List<DownvoteTarget> checkForDownvotes(List<SummaryEntry> before, 
			List<SummaryEntry> after, boolean debug) {
		long analysisTimestamp = System.currentTimeMillis();
		Map<String, DownvoteTarget> users = new HashMap<String, DownvoteTarget>();
		for (SummaryEntry b : before) {
			String uid = b.userId;
			DownvoteTarget target = users.get(uid);
			if (target == null) {
				target = new DownvoteTarget();
				users.put(uid, target);
				target.id = uid;
				target.name = b.userName;
				target.repBefore = b.userRep;
				target.avatarUrl = b.avatarUrl;
				target.analysisTimestamp = analysisTimestamp;
			}
			target.questionsBefore.add(b);
		}
		for (SummaryEntry a : after) {
			String uid = a.userId;
			DownvoteTarget target = users.get(uid);
			// if it was in the previous query, only then do we work with it
			if (target != null) {
				target.repAfter = a.userRep;
				target.questionsAfter.add(a);
			}
		}
		int[] diffDownvoteGiver = { -1, -2, -3, -4, 9, 8, 7, 6, 19, 18, 17, 16, 29, 28, 27, 26  };
		int[] diffDownvoteReceiver = { -2, -4, -6, -8, 8, 6, 4, 18, 16, 14, 28, 26, 24 };
		// filter those records from users who did not appear after - no diff there
		for (DownvoteTarget dt : new ArrayList<DownvoteTarget>(users.values())) {
			if (dt.repAfter == 0) {
				users.remove(dt.id);
			} else {
				int diff = dt.repAfter - dt.repBefore;
				// check if the there was an odd/even transition
				if (Math.abs(diff) % 2 == 1) {
					for (int g : diffDownvoteGiver) {
						if (diff == g) {
							dt.isGiver = true;
							if (debug) {
								System.out.printf("GIVER: %s (%s) %d -> %d (%d)%n", dt.name, dt.id, dt.repAfter, dt.repBefore, diff);
							}
							break;
						}
					}
					for (int r : diffDownvoteReceiver) {
						if (diff == r) {
							dt.isReceiver = true;
							if (debug) {
								System.out.printf("RECVR: %s (%s) %d -> %d (%d)%n", dt.name, dt.id, dt.repAfter, dt.repBefore, diff);
							}
							break;
						}
					}
				}
				// now retry for the even changes
				if (!dt.isGiver && !dt.isReceiver) {
					for (int g : diffDownvoteGiver) {
						if (diff == g) {
							dt.isGiver = true;
							if (debug) {
								System.out.printf("GIVER*: %s (%s) %d -> %d (%d)%n", dt.name, dt.id, dt.repAfter, dt.repBefore, diff);
							}
							break;
						}
					}
					for (int r : diffDownvoteReceiver) {
						if (diff == r) {
							dt.isReceiver = true;
							if (debug) {
								System.out.printf("RECVR*: %s (%s) %d -> %d (%d)%n", dt.name, dt.id, dt.repAfter, dt.repBefore, diff);
							}
							break;
						}
					}
				}
			}
		}
		return new ArrayList<DownvoteTarget>(users.values());
	}
	public static void main(String[] args) throws Exception {
		List<SummaryEntry> before = new ArrayList<SummaryEntry>();
		// get 200 of the latest activity
		for (int i = 0; i < 4; i++) {
			System.out.printf("Before Page #%d%n", i);
			byte[] data = SOPageParsers.getQuestionsData("http://stackoverflow.com", null, "active", i, 50);
			before.addAll(SOPageParsers.processMainPage(data));
			System.out.printf("Sleep 1 second%n");
			TimeUnit.SECONDS.sleep(1);
		}
		System.out.printf("Sleep 1 minute%n");
		TimeUnit.MINUTES.sleep(1);
		List<SummaryEntry> after = new ArrayList<SummaryEntry>();
		for (int i = 0; i < 4; i++) {
			System.out.printf("After Page #%d%n", i);
			byte[] data = SOPageParsers.getQuestionsData("http://stackoverflow.com", null, "active", i, 50);
			after.addAll(SOPageParsers.processMainPage(data));
			TimeUnit.SECONDS.sleep(1);
			System.out.printf("Sleep 1 second%n");
		}
		System.out.println("---------- Analysis ----------");
		checkForDownvotes(before, after, true);
		System.out.println("------------ Done ------------");
		
	}
}
