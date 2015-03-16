# Current version: 0.79 #

The aim of this open source project is to provide a common, centralized and client side user interface for managing (currently: reading public info only) the Stack Overflow family of sites ([Stack Overflow](http://stackoverflow.com), [Meta Stack Overflow](http://meta.stackoverflow.com), [Server Fault](http://serverfault.com) and [Super User](http://superuser.com)).

## Feedback and test results welcome! ##

### Recent changes (0.79) ###

  * Fixed the reputation bar to accomodate the latest HTML page format of the users.

### Recent changes (0.78) ###

  * Added the option for combining multiple tag queries within one window: use the pipe | character to separate the alternatives. To search for a combination, separate the tags with spaces: Examples

| ` java|c# ` | List questions tagged with java or c# |
|:------------|:--------------------------------------|
| ` java swing|c++ mfc ` | List questions tagged with (java and swing) or (c++ and mfc) |


## Included in Softopedia.com ##

I'm happy to announce that the program was included on the Softopedia's http://mac.softpedia.com/get/Internet-Utilities/Open-Stack-Overflow-Frontend.shtml site. In the MAC section. At least I know it works and looks nice on MAC too.

## Warning! ##

To avoid flooding the main page, here you can find the [actual warning message](http://code.google.com/p/open-so-frontend/wiki/Warnings).

Btw. the most likely reason for my IP ban was that I cast and revoked a ton of upvotes in a quick succession to test my Reputation bar's display of green/red coloring for value changes. Only the team can confirm this, but I doubt my ban was due the EDD as I used it for several days prior to that from that particular IP address.

[Change history](http://code.google.com/p/open-so-frontend/wiki/ChangeHistory) | [More screenshots](http://code.google.com/p/open-so-frontend/wiki/Screenshots)

![![](http://karnokd.uw.hu/open-so-frontend-71-tn.png)](http://karnokd.uw.hu/open-so-frontend-71.png)

![![](http://karnokd.uw.hu/open-so-frontend-7b-tn.png)](http://karnokd.uw.hu/open-so-frontend-7b.png)

![![](http://karnokd.uw.hu/open-so-frontend-75-d-tn.png)](http://karnokd.uw.hu/open-so-frontend-75-d.png)

### Features ###
  * Requires **Java 1.6+**
  * List questions of four sites (SO, SF, Meta, SU)
  * Multiple pages with different query criteria
  * Rename tabs.
  * Periodically refresh question list
  * Display excerpts
  * Sort list by the columns
  * Track which questions were viewed by the user
  * Manage page level and global ignore lists
  * Save page settings on exit and load settings on startup
  * Popup menus on the question list and ignore lists
  * Go to question in the default browser
  * Go to the user in the default browser
  * Copy avatar URL into the clipboard

### Planned features ###
  * Display and render a question within the application
  * Prepare answer within the application (nasty ALTGR+B in SO editor!)
  * Question, Answer, Code and blockquote folding within a post
  * Get a syntax higlighter for the code blocks.
  * Repository for common book references, libraries, method names as links pointing to the javadoc
  * Determine a questions creator, wikiness (requires per question analysis as the sites work currently)
  * Watch a question for comment changes
  * Display user page info more expanded than the on the web (e.g full lists of Q and A, recent activity, reputation deltas, etc).
  * Watch user activity (rep changes, upvote, downvote changes)
  * Infer who downvoted a post with some heuristics (scientific challenge ;)


In the beginning, the frontend is written in Java utilizing Swing GUI, HTML parser, Apache Commons HTTPClient and various Apache Commons-X libraries.

The current version has the ability to read the public listings of the SO, SF and META sites (SU is not directly accessible at the moment). The retrieved list can be sorted based on the columns. Double clicking on an entry should open it in the default browser.

The column W stands for signalling a post is a community wiki - currently, there is no confirmed way of detecting this on the listings page (I saw pages with community-wiki occurring instead of the user block, but I didn't seem to be general). Later on, the user will be able to ask the system to identify that question by querying the actual question for the wiki-flags.

Please contact me, if you'd like to join with a different language (e.g C# based) / technology (web frontend).