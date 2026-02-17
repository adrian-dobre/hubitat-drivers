import groovy.transform.Field

metadata {
    definition (name: "BTicino K4001C", namespace: "madtek", author: "Adrian Dobre") {
        capability "Configuration"
        capability "Refresh"
        capability "Switch"
        capability "Actuator"

        command "toggle"
        
        // LED Indicator Controls
        attribute "ledInDark", "enum", ["on", "off"]
        attribute "ledIfOn", "enum", ["on", "off"]
        command "setLedInDark", [[name:"Status*", type: "ENUM", constraints: ["on", "off"]]]
        command "setLedIfOn", [[name:"Status*", type: "ENUM", constraints: ["on", "off"]]]

        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0003,0004,0005,000F,FC01,0006", outClusters: "0000,FC01,0005,0019,0006", manufacturer: " Legrand", model: " Light switch with neutral"
    }
    
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

@Field static Integer CLUSTER_ON_OFF = 0x0006
@Field static Integer CLUSTER_LEGRAND = 0xFC01 

def parse(String description) {
    if (logEnable) log.debug "parse: ${description}"
    Map descMap = zigbee.parseDescriptionAsMap(description)
    
    // 1. Handle On/Off State (Now that the bulb is connected, this should work!)
    if (descMap.clusterInt == CLUSTER_ON_OFF) {
        if (descMap.attrInt == 0x0000 || descMap.commandInt == 0x01) {
            def value = (descMap.value == "01") ? "on" : "off"
            sendEvent(name: "switch", value: value)
        }
        else if (descMap.commandInt == 0x0B) { // Confirmation of command
             // We can refresh or optimistically update here if needed
             if (logEnable) log.debug "Command confirmation received"
        }
    }
    // 2. Handle LED Attribute Reports
    else if (descMap.clusterInt == CLUSTER_LEGRAND) {
        def value = (descMap.value == "01") ? "on" : "off"
        if (descMap.attrInt == 0x0001) sendEvent(name: "ledInDark", value: value)
        if (descMap.attrInt == 0x0002) sendEvent(name: "ledIfOn", value: value)
    }
}

def on() {
    if (logEnable) log.debug "on()"
    return zigbee.on()
}

def off() {
    if (logEnable) log.debug "off()"
    return zigbee.off()
}

def toggle() {
    if (logEnable) log.debug "toggle()"
    return "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x02 {}"
}

def setLedInDark(String status) {
    def val = (status == "on") ? 0x01 : 0x00
    return zigbee.writeAttribute(0xFC01, 0x0001, 0x10, val, [mfgCode: 0x1021])
}

def setLedIfOn(String status) {
    def val = (status == "on") ? 0x01 : 0x00
    return zigbee.writeAttribute(0xFC01, 0x0002, 0x10, val, [mfgCode: 0x1021])
}

def refresh() {
    if (logEnable) log.debug "refreshing..."
    return zigbee.readAttribute(CLUSTER_ON_OFF, 0x0000) +
           zigbee.readAttribute(CLUSTER_LEGRAND, 0x0001, [mfgCode: 0x1021]) +
           zigbee.readAttribute(CLUSTER_LEGRAND, 0x0002, [mfgCode: 0x1021])
}

def configure() {
    log.info "Configuring reporting for K4001C..."
    return zigbee.onOffConfig() + 
           zigbee.configureReporting(CLUSTER_ON_OFF, 0x0000, 0x10, 0, 3600, 0x01) +
           refresh()
}