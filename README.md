# HubitatScrapeBool

"Scraping switch" for Hubitat Elevation

Project state: New, incomplete! This is not anywhere near a polished
finished tool.

This device driver manages a virtual switch whose state is controlled
by polling a specified URL and checking the data returned against an
"on regexp" and an "off regexp". If it matches one, the switch is set
to that state. If it fails to match and only one regexp was provided,
then the switch state is set to the opposite of the regexp it failed
to match. So, you can either specify precise "on" and "off"
conditions, or else you can specify just one and anything not matching
that gets set to the opposite state. 

Screen-scraping, which is what that is, is a terrible idea. Attempting
to parse modern web screens intended for humans well enough to get
reliable information from them is hard, and then once you get it
working they'll redesign the page behind your back. But sometimes it's
the only way to get information, and it might be reliable enough for
convenience data.

The original use-case that got me started developing this was wanting
to get my local city's snow emergency information into my HE, so that
I could then include that in low-priority notifications (displayed on
LED bars on some of my wired-in smart switches). I've lived decades
without it, I receive email announcing any snow emergencies, I can
check their web page manually if in doubt, and looking out at the
sidewalk lets me make a tolerable guess as to whether they're going to
declare an emergency. So, not urgent or life-critical. And I haven't
found anywhere the city makes the data available on a simple web
API. This is good enough for that, for me. (And, besides, it was an
excuse to develop my first Hubitat device driver.)

(Given how web APIs mostly work, you could probably also use this to
get simple boolean data out of an actual web API with it.)

The frequency of polling is controlled by a crontab-style string; see
the Hubitat documentation on the schedule() call for the format.

The other issue with screen-scraping can be the load it imposes on the
servers serving the pages being scraped. Please be thoughtful and
polite when choosing your polling schedule!

So, to use this effectively, you need to find the URL to a page that
contains the data you want, write a regular expression that
distinguishes (sufficiently reliably) between the two cases you care
about, and write a crontab schedule line to poll at the right times
but not too often.

Oh, and know how to install user driver code. That's in the Hubitat
docs and commonly discussed in the forums, I'm not going to try to
write (and then support) my own version. I learned it less than a week
ago myself.

Have fun!
