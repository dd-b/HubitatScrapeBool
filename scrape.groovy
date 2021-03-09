/*
	Scraping Switch--present switch behavior controlled by web scraping.

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

static String version()	{  return '0.1.192'  }

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
	       displayDuringSetup: true)
// ZZDO Put conditionals around debug log statement using this
	input (name: 'debugOn', type: 'bool', title: 'Enable debug logging?',
	       displayDuringSetup: true, defaultValue: true)
    }
}

// Early on, kick off the polling. Make this configurable and MUCH slower
// when it's working. Maybe use fancy schedule for MPLS case? Know by 6pm,
// don't check at all frequently when snow emergency already in effect?
def initialize () {
    log.debug "Starting polling";
    unschedule();		//Get rid of any existing schedule items
    runEvery1Minute (pollUrl); // Note singular vs. plural!
}

void updated() {
    log.info "Updated..."
    log.warn "description logging is: ${txtEnable == true}"
    initialize()
}

void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    refresh()
    initialize()
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

void refresh() {
    parent?.componentRefresh(this.device)
}

// Start asynch poll of our scraping URL.
void pollUrl() {
    log.debug "Polling ${scrapeUrl} 30";
    if (scrapeUrl) {
	def params = [
            uri: 		scrapeUrl,
	    timeout:	30
            // body: ''
	]
	asynchttpGet(procUrl, params)
    } else {
	log.warn "URL to scrape not configured";
    }
}

// Called when the async poll of the scraping URL completes.
def procUrl (response, data) {
    log.debug "Poll result ${response.status} returned data ${data}";
    if (response.hasError()) {
        log.warn "response received error: ${response.getErrorMessage()}"
	return
    }
    if (data =~ /snow-emergency: yes/) {
	sendEvent(name: 'on', value: 0, descriptionText: "Switch set on")
	log.info "Switch set on";
    } else {
	sendEvent(name: 'off', value: 0, descriptionText: "Switch set off")
	log.info "Switch set off";
    }
}
