# Warning #

If you are greedy, the automatic misbehavior detector (AMD) on the family sites can and will block your IP address. It seems that frequent non-logged-in polling of profile info and/or user toplisting can trigger these defensive mechanisms, and your IP address gets banned from ALL 4 SITES, not just the one or two you are watching.

In theory, it is possible to programmatically log you in into the sites, so the mediator program does thinks in your name, similary as a browser would do. To accomplish this, I have to cheat the OpenID login some way, so HTTPClient can lie himself in with the appropriate cookies.

Of course, if the sites gain some API for polling info in (near-)real time, most of our problems will be solved, as I know they will implement some non-OpenID authentication on those webservice calls.