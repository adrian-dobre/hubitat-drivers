/**
 * Aqara H2 EU (WS-K08E) - v21.0
 * FIXED: Removed 0x0201 to stop 0x86 errors.
 * FIXED: Enforced Operation Mode on EP 01/02 with Z2M correct types.
 */

metadata {
    definition (name: "Aqara H2 EU Z2M Pro", namespace: "custom", author: "Gemini") {
        capability "Configuration"
        capability "Refresh"
        capability "PushableButton"
        capability "HoldableButton"
        capability "DoubleTapableButton"
        capability "Switch"
        capability "PowerMeter"
        capability "EnergyMeter"
        capability "TemperatureMeasurement"
        
        attribute "amperage", "number"
        
        command "setOperationMode", [[name: "Endpoint", type: "ENUM", constraints: ["01", "02"]], [name: "Mode", type: "ENUM", constraints: ["control", "decoupled"]]]
        command "setLockRelay", [[name: "Endpoint", type: "ENUM", constraints: ["01", "02"]], [name: "State", type: "ENUM", constraints: ["unlocked", "locked"]]]
        command "setLedIndicator", [[name: "Endpoint", type: "ENUM", constraints: ["01", "02"]], [name: "State", type: "ENUM", constraints: ["off", "on"]]]
        command "setLedMode", [[name: "Mode", type: "ENUM", constraints: ["linkToRelay", "inverted", "alwaysOff"]]]

        command "createChildDevices"
        command "componentOn"
        command "componentOff"

        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0003,0004,0005,0006,0009,0702,0B04,FCC0", outClusters: "0019,000A", manufacturer: "LUMI", model: "lumi.switch.agl010"
    }
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

// --- OPERATION MODE (DECOUPLED) ---
def setOperationMode(ep, mode) {
    int val = (mode == "decoupled") ? 0 : 1
    int endP = Integer.parseInt(ep)
    log.info "Setting EP ${ep} Operation Mode to ${mode} (Attr 0x0200)"
    
    // Z2M ModernExtend for H2 uses Attr 0x0200, Type 0x20 (Uint8)
    def cmds = zigbee.writeAttribute(0xFCC0, 0x0200, 0x20, val, [mfgCode: "0x115F", destEndpoint: endP])
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

// --- RELAY LOCK ---
def setLockRelay(ep, state) {
    int val = (state == "locked") ? 1 : 0
    int endP = Integer.parseInt(ep)
    log.info "Setting Relay ${ep} Lock to ${state} (Attr 0x0285)"
    
    // Z2M: Attr 0x0285, Type 0x20
    def cmds = zigbee.writeAttribute(0xFCC0, 0x0285, 0x20, val, [mfgCode: "0x115F", destEndpoint: endP])
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

// --- LED CONTROL ---
def setLedIndicator(ep, state) {
    int val = (state == "on") ? 1 : 0
    int endP = Integer.parseInt(ep)
    log.info "Setting LED EP ${ep} to ${state} (Attr 0x0203)"
    
    // Z2M: Attr 0x0203, Type 0x10 (Boolean)
    def cmds = zigbee.writeAttribute(0xFCC0, 0x0203, 0x10, val, [mfgCode: "0x115F", destEndpoint: endP])
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

def setLedMode(mode) {
    int val = (mode == "linkToRelay") ? 0 : (mode == "inverted") ? 1 : 2
    log.info "Setting Master LED Mode to ${mode} (Attr 0x00F0)"
    def cmds = zigbee.writeAttribute(0xFCC0, 0x00F0, 0x20, val, [mfgCode: "0x115F", destEndpoint: 1])
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

// --- PARSING ---

def parse(String description) {
    if (logEnable) log.debug "RAW: ${description}"
    def descMap = zigbee.parseDescriptionAsMap(description)
    
    if (descMap.cluster == "0006") {
        def ep = descMap.endpoint
        def value = (descMap.value == "01" || descMap.command == "01") ? "on" : "off"
        def child = getChildDevice("${device.deviceNetworkId}-ep${ep}")
        if (child) child.parse([[name: "switch", value: value]])
        if (ep == "01") sendEvent(name: "switch", value: value)
    } else if (descMap.cluster == "0B04") {
        long val = Long.parseLong(descMap.value, 16)
        if (descMap.attrId == "050B") sendEvent(name: "power", value: (val / 10.0), unit: "W")
    } else if (descMap.cluster == "0012") {
        int ep = Integer.parseInt(descMap.endpoint, 16)
        int btn = (ep == 1 || ep == 4) ? 1 : 2
        sendButtonEvent(btn, "pushed")
    }
}

def configure() {
    log.info "Configuring..."
    createChildDevices()
    return ["he raw 0x${device.deviceNetworkId} 1 0x01 0x0000 {10 00 01 05 00 07 00 42 12 6C 75 6D 69 2E 73 77 69 74 63 68 2E 61 67 6C 30 31 30}"]
}

def sendButtonEvent(btn, type) { sendEvent(name: type, value: btn, isStateChange: true) }
def on() { componentOn(getChildDevice("${device.deviceNetworkId}-ep01")) }
def off() { componentOff(getChildDevice("${device.deviceNetworkId}-ep01")) }
def componentOn(cd) { sendHubCommand(new hubitat.device.HubAction("he cmd 0x${device.deviceNetworkId} 0x${cd.deviceNetworkId.split("-ep")[-1]} 0x0006 1 {}", hubitat.device.Protocol.ZIGBEE)) }
def componentOff(cd) { sendHubCommand(new hubitat.device.HubAction("he cmd 0x${device.deviceNetworkId} 0x${cd.deviceNetworkId.split("-ep")[-1]} 0x0006 0 {}", hubitat.device.Protocol.ZIGBEE)) }
def createChildDevices() { [1, 2].each { ep -> String dni = "${device.deviceNetworkId}-ep0${ep}"; if (!getChildDevice(dni)) addChildDevice("hubitat", "Generic Component Switch", dni, [name: "${device.displayName} (Relay ${ep})", isComponent: true]) } }