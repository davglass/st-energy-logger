/**
 *  Aeon HEM - My Logger
 *
 *  Copyright 2015 Dav Glass
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License  for the specific language governing permissions and limitations under the License.
 *
 *
 */


// Automatically generated. Make future change here.
definition (
                name: "Aeon HEM - My Logger",
                namespace: "davglass",
                author: "Dav Glass",
                description: "Aeon HEM - My Logger",
                category: "My Apps",
                iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Electronics.electronics13-icn?displaySize",
                iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Electronics.electronics13-icn?displaySize=2x")

preferences {
    section("Log devices...") {
        input "energymeters", "capability.EnergyMeter", title: "Energy Meter", required: false, multiple: true
    }
    section("Outside Temp.") {
        input "outside", "capability.temperatureMeasurement", title: "Outside Temp Meter", required: false, multiple: true
    }
    section("Inside Temp.") {
        input "inside", "capability.temperatureMeasurement", title: "Inside Temp Meter", required: false, multiple: true
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()

    initialize()
}

def initialize() {
    state.clear()
        unschedule(checkSensors)
        schedule("0 */5 * * * ?", "checkSensors")
        subscribe(app, appTouch)
        schedule("0 0 23 L * ?", "resetCounter")
}

def resetCounter() {
    energymeters.each {
        sendNotificationEvent("Energy Meter Counter was reset")
        it.reset()
    }
}

def appTouch(evt) {
    log.debug "appTouch: $evt"
    checkSensors()
}



def checkSensors() {

    def logitems = []

    for (t in settings.energymeters) {
        logitems.add(["power", t.latestValue("power")] )
        state[t.displayName + ".power"] = t.latestValue("power")
        logitems.add(["energy", t.latestValue("energy")] )
        state[t.displayName + ".energy"] = t.latestValue("energy")
    }
    
    def temp = []
    inside.each {
        temp.add(it.currentTemperature)
    }
    log.debug "Temps Inside: ${temp}"
    def average = temp.sum() / temp.size()
    log.debug "Average Inside: ${average}"
    logitems.add(["inside", average])
    
    def tempO = []
    outside.each {
        tempO.add(it.currentTemperature)
    }
    log.debug "Temps Outside: ${tempO}"
    def averageO = tempO.sum() / tempO.size()
    log.debug "Average Outside: ${averageO}"
    logitems.add(["outside", averageO])
    
    logField(logitems)

}

private logField(logItems) {
    def fieldvalues = ""
    log.debug logItems


    def jsonS = "["
    logItems.each {
        jsonS += "{\"name\":\"${it[0]}\",\"value\": ${it[1]} },"
    }
    jsonS += "{}]" //This is a hack so that I don't have to count the items
    log.debug jsonS
    def uri = "https://davglass-house.herokuapp.com/-/api/feed/"
    def json = "${jsonS}"

    def headers = [
        "content-type": "application/json"
    ]

    def params = [
        uri: uri,
        headers: headers,
        body: json
    ]
    log.debug params.body
    httpPutJson(params) {response -> parseHttpResponse(response)}
}

def parseHttpResponse(response) {
    log.debug "HTTP Response: ${response}"
}
