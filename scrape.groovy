/*
	"Scraping switch" for Hubitat Elevation

	Copyright 2021 David Dyer-Bennet.

	MIT License

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in all
	copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
	SOFTWARE.
*/

// This code lives in a repository at https://github.com/dd-b/HubitatScrapeBool
// You can see change history there, I'm not going to try to keep it updated
// in the file also.

static String version()	{  return '0.1.195'  }

metadata {
    definition (
	name: "Scraping Switch",
	namespace: "dd-b",
	author: "David Dyer-Bennet",
	importUrl: "https://raw.githubusercontent.com/dd-b/HubitatScrapeBool/main/scrape.groovy",
	component: true
    ) {
        capability "Switch"	// on() and off()
        capability "Refresh"	// refresh()
        capability "Actuator"	// has no commands, copied from example
    }
    preferences {
        input (name: "txtEnable", type: "bool", title: "Enable descriptionText logging",
	       displayDuringSetup: true, defaultValue: true)
	input (name: "scrapeUrl", type: "string", title: "URL to scrape",
	       defaultValue: "http://example.com/API/snow-emergency/your-city.html",
	       displayDuringSetup: true)
	input (name: "pollSched", type: "string", title: "Polling schedule, crontab format",
	       defaultValue: "47 16 4,6,8,12,14-19 * * *", // Change at least the seconds!
	       displayDuringSetup: true)
	input (name: "testRegexp", type: "string", title: "Regular expression to set switch on",
	       defaultValue: "snow-emergency: yes",
	       displayDuringSetup: true)
	input (name: 'debugOn', type: 'bool', title: 'Enable debug logging?',
	       displayDuringSetup: true, defaultValue: true)
    }
}

// Early on, kick off the polling. Make this configurable and MUCH slower
// when it's working. Maybe use fancy schedule for MPLS case? Know by 6pm,
// don't check at all frequently when snow emergency already in effect?
def initialize () {
    unschedule();		//Get rid of any existing schedule items
    log.info "Starting polling";
    schedule (pollSched, pollUrl);
}

// Called when the device settings are saved
void updated() {
    log.info '''Updated device settings:
    Description logging is: ${txtEnable == true}
    Scrape URL: ${scrapeUrl}
    Polling schedule: ${pollSched}
    Test regexp: ${testRegexp}
    Debug logging: ${debugOn}
'''
    initialize()
}

// Called when the device is first created
void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
//    refresh()
//    initialize()
}

// Required by form, but not actually necessary if the device isn't recieving messages.
void parse(String description) { log.warn "parse(String description) not implemented" }

void parse(List description) {
    description.each {
        if (it.name in ["switch"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
        }
    }
}

// As a switch, we can be set on. This is how. The "parent?" notation is a safe
// navigation thing, if parent is null then the statement aborts with a value of
// null, rather than giving an error. New to me, a Groovy thing, handy I think.
// Though...shouldn't that be the default? With explicit checking required if
// you want to *fail* if something is undefined?
void on() {
    parent?.componentOn(this.device)
}

void off() {
    parent?.componentOff(this.device)
}

// Called when the refresh button on the device page is clicked
void refresh() {
    parent?.componentRefresh(this.device) // Can't happen?
    // Force immediate poll, useful for testing
    pollUrl()
}

// Start asynch poll of our scraping URL.
void pollUrl() {
    if (debugOn) log.debug "Polling ${scrapeUrl} 30";
    if (scrapeUrl) {
	def params = [
            uri: 	scrapeUrl,
	    timeout:	30,
	    contentType: 'text/html', 
	]
	asynchttpGet(procUrl, params)
    } else {
	log.warn "URL to scrape not configured";
    }
}

// Called when the async poll of the scraping URL completes.
def procUrl (response, data) {
    if (debugOn) log.debug "Poll result ${response.status} returned data ${response.data}";
    if (response.hasError()) {
        log.warn "response received error: ${response.getErrorMessage()}"
	return
    }
    if (response.data =~ testRegexp) {
	sendEvent(name: 'on', value: 0, descriptionText: "Switch set on")
	log.info "Switch set on";
    } else {
	sendEvent(name: 'off', value: 0, descriptionText: "Switch set off")
	log.info "Switch set off";
    }
}
