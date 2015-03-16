# Disclaimer #
It seems, potentially due my recent test-activity from the EDD, that I have drawn attention to myself and I'm mostly blocked out from the SO sites (e.g. I get blank page everywhere). The current EDD polling pattern is to periodic and to much correlated, and it accesses the public face of the site without logging in. Obviously, the detection algorithm is still valid, proven and working, and if you can create a GreaseMonkey script or other browser plugin, you can do the tracking as you normally browse the site - no suspicious frequent public access from your IP address.

Because I'm locked out, I don't care about my reputation (because I don't know the value) and anyway, I was only vegetating on the sites recently (e.g. few new answers or questions of my interest).

Consequently, as I'm now unable to test any of my improvements to the application, I'm not sure I will continue to develop it further.

Take care - David

# Introduction #

_For your public eyes only_

Who downvoted me? Question many times asked and asked on meta. Even though now you get a box saying: explain your downvote, I doubt it changed the common user behavior on downvotes.

Me, like other users, don't like to be downvoted (unexplained I might add), so I thought, there should be a way of figuring out who downvoted me!

It turns out there is a way, and luckily, we need only publicly available information to detect that - with a certainty of course. And this publicly and massively available information is:

# User Reputation! #

You remember the rule about if you cast a downvote, you loose 1 rep? Right! If we somehow could detect the numerical change in the reputation value we should be able to detect who was responsible!

Of course, retrieving current user reputation values is tedious without a program, but hey, we'll always have <strike>Paris</strike> my Frontend, which is excellent at parsing question listings, user profiles (and many more pages later on).

From there on, it was easy - in theory. The way of performing differential analysis was easy enough: snapshot current visible values, wait a minute, then snapshot again. Consequently, do a differential analysis and check for reputation changes.

The hard part was to put this entire thing into a GUI - and have proper UI behavior and features. I should add, I still don't feel the 0.74 variant is feature complete - more usability functions are required -  I feel it too when I test the application.

## This is data mining! ##

According to my academic colleagues, this activity cannot be called data mining, more like data analysis, aggregation and inference. Data mining involves learning algorithms - mine just remembers.

# But there are some drawbacks! #

I'm not naive about the fact that my differential algorithm is not 100% safe - there are some strange value change patterns occurring, but I would say, in 80% of the case, there is an obvious -1 and -2 pair of reputation change: the downvoter and the downvoted.

The technology is also in some way abusive - it queries the public listing pages of the target SO sites on every 60 seconds, and for multi-page (e.g. active page) listings, it keeps a 1 second wait between site queries - currently this configuration is hard-coded.

Another drawback is, that not everyone's reputation change is visible. If the user doesn't appear on the active, hot or newest listings, on the top user listings (or later on, in the Top30 badge activists) there is no good way to detect that change.

I should also mention, if you see a pair of rep change, it might not mean they are related, the downvoter operated on a different user whereas the downvoted received it from somebody else. **Therefore, THINK ABOUT IT BEFORE ACCUSING SOMEONE!**

# And what about users with rep above 10k #

Good eyes man! This is a very dark spot in face of the algorithm, because these users have most of their reputation display rounded up to thousand. You see 10, 10.1, 10.2 etc., N x 100 jumps! Today (08-05) only the main stackoverflow.com is affected - the other sites have users currently below 10k - but not for long I guess.

There is one place you can get an exact reputation value: the public User Profile. Hmm.
The 0.74 has the ability to watch some user's profile in the RepBar - and use this frequently updated information in the detection algorithm - especially if you are suspecting someone hates you (right Rich B?).

Unfortunately, retrieving user profile is slower and has number and frequency limitations.  Just by looking at the server log it becomes obvious: your IP address queries more than one (e.g your own reputation) user profile in a loop. You might get banned, or worse - the site starts to lie to you randomly: the user profile becomes unreliable information source and what not. **BE CAREFUL WHO YOU WATCH. WATCH YOURSELF!**

# This is unethical! #

Yes and no. Watching others is bad, but if nobody forces them to behave nicely and even you start to hate the words `by-design`, this is your window. But remember the quote: "Who watches the watchers?" - if we had **Transparency**, there were no need for such tricks.

To balance the weight, here I give the owners some hints how to _defeat_ this kind of data aggregation:

# Howto design a proper reputation system: from beginners up to horror masters #

  * Hide reputation completely from mass listings and use approximate values in the user design to defeat small value change detection
  * Limit the non-logged-in visitors ability to see recent changes - display cached values for longer than 60 seconds.
  * Actively detect frequent polling and use cached images or 404 randomly.
  * Actively force the user to explain the reason, and if it is already expained by other, auto-link that post/comment for clarity.
  * Or make it public to the target who did it - we have the right to face our accusers in real life.
  * Have a different <a href='http://meta.stackoverflow.com/questions/7322/should-the-weight-of-downvotes-be-increased/7711#7711'>cost model for downvoting</a>.