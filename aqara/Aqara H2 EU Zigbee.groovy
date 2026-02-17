/**
 * Aqara H2 EU (WS-K08E) Zigbee Driver
 * 
 * Device: Aqara H2 EU Double Switch with Power Monitoring (lumi.switch.agl010)
 * Author: Adrian Dobre
 * 
 * Features:
 * - Dual relay control with child devices
 * - Power, voltage, current, energy monitoring
 * - Multi-click button support (push, double-tap, hold) for 4 buttons
 * - Configurable modes: decoupled mode, relay lock, LED indicators
 * - Power outage counter
 * - Temperature sensor
 * 
 * Notes:
 * - Power measurement uses genAnalogInput cluster (0x000C) per zigbee-herdsman-converters
 * - Device requires manual driver selection on first pairing (Aqara limitation)
 * - Child relay devices created automatically on install/configure
 */

metadata {
    definition (name: "Aqara H2 EU", namespace: "madtek", author: "Adrian Dobre") {
        capability "Configuration"
        capability "Refresh"
        capability "PushableButton"
        capability "HoldableButton"
        capability "DoubleTapableButton"
        capability "Switch"
        capability "PowerMeter"
        capability "CurrentMeter"
        capability "VoltageMeasurement"
        capability "EnergyMeter"
        capability "TemperatureMeasurement"
        
        attribute "amperage", "number"
        attribute "voltage", "number"
        attribute "powerOutageCount", "number"
        attribute "relay1", "string"
        attribute "relay2", "string"
        attribute "powerOnBehaviorRelay1", "string"
        attribute "powerOnBehaviorRelay2", "string"
        attribute "ledMode", "string"
        attribute "multiClickButton1", "string"
        attribute "multiClickButton2", "string"
        attribute "decoupledModeRelay1", "string"
        attribute "decoupledModeRelay2", "string"
        attribute "relayLockRelay1", "string"
        attribute "relayLockRelay2", "string"
        attribute "ledIndicator", "string"
        
        command "multiClickButton1", [[name: "Enable Multi-Click Button 1", type: "ENUM", constraints: ["off", "on"]]]
        command "multiClickButton2", [[name: "Enable Multi-Click Button 2", type: "ENUM", constraints: ["off", "on"]]]
        command "decoupledModeRelay1", [[name: "Decouple Relay 1 (ON = Decoupled)", type: "ENUM", constraints: ["off", "on"]]]
        command "decoupledModeRelay2", [[name: "Decouple Relay 2 (ON = Decoupled)", type: "ENUM", constraints: ["off", "on"]]]
        command "relayLockRelay1", [[name: "Lock Relay 1", type: "ENUM", constraints: ["off", "on"]]]
        command "relayLockRelay2", [[name: "Lock Relay 2", type: "ENUM", constraints: ["off", "on"]]]
        command "ledIndicator", [[name: "LED Indicator", type: "ENUM", constraints: ["off", "on"]]]
        command "powerOnBehaviorRelay1", [[name: "Power On Behavior Relay 1", type: "ENUM", constraints: ["on", "previous", "off"]]]
        command "powerOnBehaviorRelay2", [[name: "Power On Behavior Relay 2", type: "ENUM", constraints: ["on", "previous", "off"]]]
        command "ledMode", [[name: "LED Mode", type: "ENUM", constraints: ["normal", "inverted"]]]

        command "createChildDevices"
        command "componentOn"
        command "componentOff"

        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0009,0702,0B04,FCC0", outClusters: "0019,000A", manufacturer: "LUMI", model: "lumi.switch.agl010", deviceJoinName: "Aqara H2 EU Double Switch"
        fingerprint profileId: "0104", manufacturer: "LUMI", model: "lumi.switch.agl010", deviceJoinName: "Aqara H2 EU Double Switch"
    }
    
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    }
}

// ===== Lifecycle Methods =====

def installed() {
    log.info "Aqara H2 EU installed"
    sendEvent(name: "numberOfButtons", value: 4)
    createChildDevices()
}

def updated() {
    log.info "Aqara H2 EU updated"
    sendEvent(name: "numberOfButtons", value: 4)
    createChildDevices()
    
    // Auto-disable debug logging after 30 minutes
    if (logEnable) {
        log.warn "Debug logging enabled for 30 minutes"
        runIn(1800, "logsOff")
    }
}

def logsOff() {
    log.warn "Debug logging disabled"
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

// ===== Message Parsing =====

def parse(String description) {
    if (logEnable) log.debug "RAW: ${description}"
    def descMap = zigbee.parseDescriptionAsMap(description)
    String cluster = descMap.cluster?.toUpperCase()
    String attrId = descMap.attrId?.toUpperCase()?.padLeft(4, '0')
    
    // 1. AQARA CUSTOM STRUCTURE (VOLTAGE / ENERGY / AMPS)
    if (cluster == "FCC0" && attrId == "00F7") {
        decodeAqaraStruct(descMap.value)
    }
    else if (cluster == "FCC0" && attrId == "FFF2") {
        decodeAqaraFff2(descMap.value)
    }
    else if (cluster == "FCC0" && attrId == "0002") {
        decodeAqaraOutageAttr(descMap.value)
    }
    else if (cluster == "FCC0" && ["0286", "0200", "0285", "0203", "0517", "00F0"].contains(attrId)) {
        decodeEndpointSetting(descMap)
    }

    // 1b. XIAOMI/AQARA BASIC STRUCTURE (OUTAGE COUNT + AUX METRICS)
    else if (cluster == "0000" && (attrId == "FF01" || attrId == "FF02")) {
        decodeXiaomiStruct(descMap.value)
    }
    
    // 2. POWER MEASUREMENT (PRIMARY SOURCE: genAnalogInput)
    else if (cluster == "000C" && attrId == "0055") {
        decodePowerMeasurement(descMap)
    }
    
    // 3. ELECTRICAL MEASUREMENTS (VOLTAGE / CURRENT / POWER FALLBACK)
    else if (cluster == "0B04") {
        decodeElectricalMeasurement(descMap)
    }
    
    // 4. MULTI-CLICK BUTTONS (EP1->Btn1, EP2->Btn2, EP4->Btn3, EP5->Btn4)
    else if (cluster == "0012") {
        int ep = Integer.parseInt(descMap.endpoint, 16)
        Integer btn = [1: 1, 2: 2, 4: 3, 5: 4][ep]
        if (btn != null) {
            int val = Integer.parseInt(descMap.value, 16)
            if (val == 1) sendButtonEvent(btn, "pushed")
            else if (val == 2) sendButtonEvent(btn, "doubleTapped")
            else if (val == 0) sendButtonEvent(btn, "held")
        }
    }
    
    // 5. RELAY STATUS
    else if (cluster == "0006") {
        def ep = descMap.endpoint
        def value = (descMap.value == "01" || descMap.command == "01") ? "on" : "off"
        def child = getChildDevice("${device.deviceNetworkId}-ep${ep}")
        if (child) child.parse([[name: "switch", value: value]])
        if (ep == "01") {
            sendEvent(name: "switch", value: value)
            sendEvent(name: "relay1", value: value)
        } else if (ep == "02") {
            sendEvent(name: "relay2", value: value)
        }
    }
}

// ===== Decoder Methods =====

def decodeAqaraOutageAttr(String valueHex) {
    try {
        long raw = Long.parseLong(valueHex, 16)
        updatePowerOutageCount(Math.max(0, raw - 1))
    } catch (e) {
        log.error "Decoding 0002 error: ${e}"
    }
}

def decodePowerMeasurement(Map descMap) {
    try {
        // IMPORTANT: This device uses genAnalogInput (0x000C/0x0055) for power measurement,
        // NOT haElectricalMeasurement (0x0B04/0x050B) which is only a fallback.
        // This matches the upstream zigbee-herdsman-converters implementation.
        // Value is IEEE 754 single precision float with NO scaling/divisor applied.
        def raw = descMap.value
        if (raw) {
            long bits = Long.parseLong(swapEndianHex(raw), 16)
            float watts = Float.intBitsToFloat(bits.intValue())
            if (watts >= 0 && watts < 20000) {
                sendEvent(name: "power", value: watts.round(1), unit: "W", descriptionText: "Power is ${watts.round(1)} W")
            }
        }
    } catch (e) {
        log.error "Decoding genAnalogInput power error: ${e}"
    }
}

def decodeEndpointSetting(Map descMap) {
    try {
        String attrId = descMap.attrId?.toUpperCase()?.padLeft(4, '0')
        String endpoint = descMap.endpoint?.toUpperCase()?.padLeft(2, '0')
        long raw = Long.parseLong(descMap.value ?: "0", 16)

        if (attrId == "0286") {
            if (endpoint == "04") {
                updateSettingState("multiClickButton1", raw == 2 ? "on" : "off")
            } else if (endpoint == "05") {
                updateSettingState("multiClickButton2", raw == 2 ? "on" : "off")
            }
        }
        else if (attrId == "0200") {
            if (endpoint == "01") {
                updateSettingState("decoupledModeRelay1", raw == 0 ? "on" : "off")
            } else if (endpoint == "02") {
                updateSettingState("decoupledModeRelay2", raw == 0 ? "on" : "off")
            }
        }
        else if (attrId == "0285") {
            if (endpoint == "01") {
                updateSettingState("relayLockRelay1", raw == 1 ? "on" : "off")
            } else if (endpoint == "02") {
                updateSettingState("relayLockRelay2", raw == 1 ? "on" : "off")
            }
        }
        else if (attrId == "0203") {
            def ledValue = raw == 1 ? "on" : "off"
            // LED indicator is a global setting for this device
            updateSettingState("ledIndicator", ledValue)
        }
        else if (attrId == "0517") {
            def behavior = raw == 0 ? "on" : (raw == 1 ? "previous" : (raw == 2 ? "off" : null))
            if (behavior) {
                if (endpoint == "01") {
                    updateSettingState("powerOnBehaviorRelay1", behavior, ["on", "previous", "off"])
                } else if (endpoint == "02") {
                    updateSettingState("powerOnBehaviorRelay2", behavior, ["on", "previous", "off"])
                }
            }
        }
        else if (attrId == "00F0") {
            def mode = raw == 1 ? "inverted" : "normal"
            updateSettingState("ledMode", mode, ["normal", "inverted"])
        }
    } catch (e) {
        log.error "Decoding endpoint setting error: ${e}"
    }
}

def decodeElectricalMeasurement(Map descMap) {
    try {
        long raw = Long.parseLong(descMap.value, 16)
        String attrId = descMap.attrId?.toUpperCase()?.padLeft(4, '0')

        if (attrId == "0505") {
            BigDecimal volts = (raw / 10.0).setScale(1, BigDecimal.ROUND_HALF_UP)
            if (volts > 0) sendEvent(name: "voltage", value: volts, unit: "V", descriptionText: "Voltage is ${volts} V")
        }
        else if (attrId == "0508") {
            BigDecimal amps = (raw / 1000.0).setScale(3, BigDecimal.ROUND_HALF_UP)
            if (amps >= 0) sendEvent(name: "amperage", value: amps, unit: "A", descriptionText: "Current is ${amps} A")
        }
        else if (attrId == "050B") {
            BigDecimal watts = (raw / 10.0).setScale(1, BigDecimal.ROUND_HALF_UP)
            sendEvent(name: "power", value: watts, unit: "W", descriptionText: "Power is ${watts} W")
        }
    } catch (e) {
        log.error "Decoding 0B04 error: ${e}"
    }
}

def decodeAqaraStruct(hexString) {
    try {
        // TEMPERATURE
        if (hexString.contains("0328")) {
            def sub = hexString.split("0328")[1]
            sendEvent(name: "temperature", value: Integer.parseInt(sub.substring(0,2), 16), unit: "°C")
        }
        // INSTANT POWER SECONDARY (Tag 98 / 0x98 / key 152)
        if (hexString.contains("9839")) {
            def sub = hexString.split("9839")[1]
            long val = Long.parseLong(swapEndianHex(sub.substring(0,8)), 16)
            float fVal = Float.intBitsToFloat(val.intValue())
            if (fVal >= 0 && fVal < 20000) {
                sendEvent(name: "power", value: fVal.round(1), unit: "W")
            }
        }
        // ENERGY (Tag 95 / 0x95 / key 149)
        if (hexString.contains("9539")) {
            def sub = hexString.split("9539")[1]
            long val = Long.parseLong(swapEndianHex(sub.substring(0,8)), 16)
            float fVal = Float.intBitsToFloat(val.intValue())
            sendEvent(name: "energy", value: fVal.round(3), unit: "kWh")
        }
        // CURRENT (Tag 97 / 0x97 / key 151 - scaling × 0.001)
        if (hexString.contains("9739")) {
            def sub = hexString.split("9739")[1]
            long val = Long.parseLong(swapEndianHex(sub.substring(0,8)), 16)
            float fVal = Float.intBitsToFloat(val.intValue())
            sendEvent(name: "amperage", value: (fVal / 1000.0).round(3), unit: "A")
        }
        // POWER OUTAGE COUNT (Tag 05, uint16) - Aqara convention is value - 1
        if (hexString.contains("0521")) {
            def sub = hexString.split("0521")[1]
            long rawCount = Long.parseLong(swapEndianHex(sub.substring(0,4)), 16)
            updatePowerOutageCount(Math.max(0, rawCount - 1))
        }
        // POWER OUTAGE COUNT (Tag 0A, uint16)
        if (hexString.contains("0A21")) {
            def sub = hexString.split("0A21")[1]
            int count = Integer.parseInt(swapEndianHex(sub.substring(0,4)), 16)
            updatePowerOutageCount(count)
        }
        // POWER OUTAGE COUNT (observed on some Aqara payloads as Tag 9A, uint16)
        if (hexString.contains("9A21")) {
            def sub = hexString.split("9A21")[1]
            int count = Integer.parseInt(swapEndianHex(sub.substring(0,4)), 16)
            updatePowerOutageCount(count)
        }
    } catch (e) {
        log.error "Decoding 00F7 error: ${e}"
    }
}

def decodeAqaraFff2(String hexString) {
    try {
        def matcher = (hexString =~ /84[0-9A-Fa-f]{2}([0-9A-Fa-f]{4})4100/)
        if (matcher.find()) {
            String rawHex = matcher.group(1)
            long raw = Long.parseLong(swapEndianHex(rawHex), 16)
            BigDecimal volts = (raw / 8.0).setScale(1, BigDecimal.ROUND_HALF_UP)
            if (volts >= 150 && volts <= 280) {
                sendEvent(name: "voltage", value: volts, unit: "V", descriptionText: "Voltage is ${volts} V")
            }
        }

        def outageMatcher = (hexString =~ /0523([0-9A-Fa-f]{8})/)
        if (outageMatcher.find()) {
            long count = parseUnsignedLittleEndian(outageMatcher.group(1))
            updatePowerOutageCount(Math.max(0, count))
        }
    } catch (e) {
        log.error "Decoding FFF2 error: ${e}"
    }
}

def decodeXiaomiStruct(String hexString) {
    try {
        int idx = 0
        while (idx + 4 <= hexString.length()) {
            int key = Integer.parseInt(hexString.substring(idx, idx + 2), 16)
            int dataType = Integer.parseInt(hexString.substring(idx + 2, idx + 4), 16)
            idx += 4

            int bytes = zigbeeTypeByteLength(dataType, hexString, idx)
            if (bytes <= 0 || (idx + (bytes * 2)) > hexString.length()) break

            String valueHex = hexString.substring(idx, idx + (bytes * 2))
            idx += (bytes * 2)

            if (key == 0x0A) {
                long count = parseUnsignedLittleEndian(valueHex)
                updatePowerOutageCount(count)
            }
        }
    } catch (e) {
        log.error "Decoding FF01/FF02 error: ${e}"
    }
}

// ===== Helper Methods =====

private void updatePowerOutageCount(long count) {
    if (count < 0) return
    def current = device.currentValue("powerOutageCount")
    if (!(current instanceof Number) || count >= (current as long)) {
        sendEvent(name: "powerOutageCount", value: count)
    }
}

private void updateSettingState(String attributeName, String value, List allowedValues = null) {
    if (value == null) return
    def allowed = allowedValues ?: ["on", "off"]
    if (!(value in allowed)) return
    if (device.currentValue(attributeName) != value) {
        sendEvent(name: attributeName, value: value)
    }
}

private int zigbeeTypeByteLength(int dataType, String hexString, int dataStartIdx) {
    switch (dataType) {
        case 0x10:
        case 0x18:
        case 0x20:
        case 0x28:
            return 1
        case 0x21:
        case 0x29:
            return 2
        case 0x23:
        case 0x2B:
        case 0x39:
            return 4
        case 0x25:
        case 0x2D:
            return 8
        case 0x41:
            if (dataStartIdx + 2 > hexString.length()) return 0
            return Integer.parseInt(hexString.substring(dataStartIdx, dataStartIdx + 2), 16) + 1
        default:
            return 0
    }
}

private long parseUnsignedLittleEndian(String valueHex) {
    return Long.parseLong(swapEndianHex(valueHex), 16)
}

private String swapEndianHex(String hex) {
    def reversed = ""
    for (int i = hex.length() - 2; i >= 0; i -= 2) {
        reversed += hex.substring(i, i + 2)
    }
    return reversed
}

// ===== Refresh and Configuration =====

def refresh() {
    log.info "Refreshing all sensor data..."
    return (
        // Power measurements
        zigbee.readAttribute(0x000C, 0x0055) +                                           // Power (genAnalogInput)
        zigbee.readAttribute(0x0B04, 0x0508) +                                           // Current
        
        // Aqara custom telemetry
        zigbee.readAttribute(0xFCC0, 0x0002, [mfgCode: "0x115F"]) +                      // Outage count
        zigbee.readAttribute(0xFCC0, 0x00F7, [mfgCode: "0x115F"]) +                      // Custom struct (temp, power, energy, current)
        zigbee.readAttribute(0xFCC0, 0xFFF2, [mfgCode: "0x115F"]) +                      // Voltage struct
        
        // Xiaomi structs
        zigbee.readAttribute(0x0000, 0xFF01, [mfgCode: "0x115F"]) +                      // Xiaomi struct 1
        zigbee.readAttribute(0x0000, 0xFF02, [mfgCode: "0x115F"]) +                      // Xiaomi struct 2
        
        // Device settings
        zigbee.readAttribute(0xFCC0, 0x0286, [mfgCode: "0x115F", destEndpoint: 0x04]) +  // Multi-click button 1
        zigbee.readAttribute(0xFCC0, 0x0286, [mfgCode: "0x115F", destEndpoint: 0x05]) +  // Multi-click button 2
        zigbee.readAttribute(0xFCC0, 0x0200, [mfgCode: "0x115F", destEndpoint: 0x01]) +  // Decoupled mode relay 1
        zigbee.readAttribute(0xFCC0, 0x0200, [mfgCode: "0x115F", destEndpoint: 0x02]) +  // Decoupled mode relay 2
        zigbee.readAttribute(0xFCC0, 0x0285, [mfgCode: "0x115F", destEndpoint: 0x01]) +  // Relay lock 1
        zigbee.readAttribute(0xFCC0, 0x0285, [mfgCode: "0x115F", destEndpoint: 0x02]) +  // Relay lock 2
        zigbee.readAttribute(0xFCC0, 0x0203, [mfgCode: "0x115F", destEndpoint: 0x01]) +  // LED indicator
        zigbee.readAttribute(0xFCC0, 0x0517, [mfgCode: "0x115F", destEndpoint: 0x01]) +  // Power on behavior relay 1
        zigbee.readAttribute(0xFCC0, 0x0517, [mfgCode: "0x115F", destEndpoint: 0x02]) +  // Power on behavior relay 2
        zigbee.readAttribute(0xFCC0, 0x00F0, [mfgCode: "0x115F", destEndpoint: 0x01])    // LED mode (normal/inverted)
    )
}

// ===== Device Commands =====

def multiClickButton1(state) { setMultiClickMode("04", state) }
def multiClickButton2(state) { setMultiClickMode("05", state) }
def decoupledModeRelay1(state) { setOperationMode("01", state) }
def decoupledModeRelay2(state) { setOperationMode("02", state) }
def relayLockRelay1(state) { setLockRelay("01", state) }
def relayLockRelay2(state) { setLockRelay("02", state) }
def ledIndicator(state) { setLedIndicator(state) }
def powerOnBehaviorRelay1(state) { setPowerOnBehavior("01", state) }
def powerOnBehaviorRelay2(state) { setPowerOnBehavior("02", state) }
def ledMode(state) { setLedMode(state) }

def setMultiClickMode(ep, state) {
    int endpoint = Integer.parseInt(ep, 16)
    boolean enabled = state in ["enabled", "on", true]
    int val = enabled ? 2 : 1
    sendHubCommand(new hubitat.device.HubMultiAction(zigbee.writeAttribute(0xFCC0, 0x0286, 0x20, val, [mfgCode: "0x115F", destEndpoint: endpoint]), hubitat.device.Protocol.ZIGBEE))
    if (endpoint == 4) {
        updateSettingState("multiClickButton1", enabled ? "on" : "off")
    } else if (endpoint == 5) {
        updateSettingState("multiClickButton2", enabled ? "on" : "off")
    }
}
def setOperationMode(ep, mode) {
    int endpoint = Integer.parseInt(ep, 16)
    boolean decoupled = mode in ["decoupled", "on", true]
    int val = decoupled ? 0 : 1
    sendHubCommand(new hubitat.device.HubMultiAction(zigbee.writeAttribute(0xFCC0, 0x0200, 0x20, val, [mfgCode: "0x115F", destEndpoint: endpoint]), hubitat.device.Protocol.ZIGBEE))
    if (endpoint == 1) {
        updateSettingState("decoupledModeRelay1", decoupled ? "on" : "off")
    } else if (endpoint == 2) {
        updateSettingState("decoupledModeRelay2", decoupled ? "on" : "off")
    }
}
def setLockRelay(ep, state) {
    int endpoint = Integer.parseInt(ep, 16)
    boolean locked = state in ["locked", "on", true]
    int val = locked ? 1 : 0
    sendHubCommand(new hubitat.device.HubMultiAction(zigbee.writeAttribute(0xFCC0, 0x0285, 0x20, val, [mfgCode: "0x115F", destEndpoint: endpoint]), hubitat.device.Protocol.ZIGBEE))
    if (endpoint == 1) {
        updateSettingState("relayLockRelay1", locked ? "on" : "off")
    } else if (endpoint == 2) {
        updateSettingState("relayLockRelay2", locked ? "on" : "off")
    }
}
def setLedIndicator(state) {
    int endpoint = 1
    boolean enabled = state in ["on", true]
    int val = enabled ? 1 : 0
    sendHubCommand(new hubitat.device.HubMultiAction(zigbee.writeAttribute(0xFCC0, 0x0203, 0x10, val, [mfgCode: "0x115F", destEndpoint: endpoint]), hubitat.device.Protocol.ZIGBEE))
    updateSettingState("ledIndicator", enabled ? "on" : "off")
}

def setPowerOnBehavior(ep, state) {
    int endpoint = Integer.parseInt(ep, 16)
    def behaviorMap = [on: 0, previous: 1, off: 2]
    if (!behaviorMap.containsKey(state)) return
    int val = behaviorMap[state]
    sendHubCommand(new hubitat.device.HubMultiAction(zigbee.writeAttribute(0xFCC0, 0x0517, 0x20, val, [mfgCode: "0x115F", destEndpoint: endpoint]), hubitat.device.Protocol.ZIGBEE))
    if (endpoint == 1) {
        updateSettingState("powerOnBehaviorRelay1", state, ["on", "previous", "off"])
    } else if (endpoint == 2) {
        updateSettingState("powerOnBehaviorRelay2", state, ["on", "previous", "off"])
    }
}

def setLedMode(state) {
    def modeMap = [normal: 0, inverted: 1]
    if (!modeMap.containsKey(state)) return
    int val = modeMap[state]
    sendHubCommand(new hubitat.device.HubMultiAction(zigbee.writeAttribute(0xFCC0, 0x00F0, 0x20, val, [mfgCode: "0x115F", destEndpoint: 0x01]), hubitat.device.Protocol.ZIGBEE))
    updateSettingState("ledMode", state, ["normal", "inverted"])
}
def configure() {
    log.info "Configuring Aqara H2 EU..."
    createChildDevices()
    return ["he raw 0x${device.deviceNetworkId} 1 0x01 0x0000 {10 00 01 05 00 07 00 42 12 6C 75 6D 69 2E 73 77 69 74 63 68 2E 61 67 6C 30 31 30}"]
}
def getInfo() {
    log.info "Getting device info..."
    return zigbee.readAttribute(0x0000, 0x0004) + zigbee.readAttribute(0x0000, 0x0005)
}

// ===== Component Device Methods =====

def sendButtonEvent(btn, type) { sendEvent(name: type, value: btn, isStateChange: true) }
def on() { componentOn(getChildDevice("${device.deviceNetworkId}-ep01")) }
def off() { componentOff(getChildDevice("${device.deviceNetworkId}-ep01")) }
def componentOn(cd) { sendHubCommand(new hubitat.device.HubAction("he cmd 0x${device.deviceNetworkId} 0x${cd.deviceNetworkId.split("-ep")[-1]} 0x0006 1 {}", hubitat.device.Protocol.ZIGBEE)) }
def componentOff(cd) { sendHubCommand(new hubitat.device.HubAction("he cmd 0x${device.deviceNetworkId} 0x${cd.deviceNetworkId.split("-ep")[-1]} 0x0006 0 {}", hubitat.device.Protocol.ZIGBEE)) }
def componentRefresh(cd) { refresh() }
def createChildDevices() {
    [1, 2].each { ep ->
        String dni = "${device.deviceNetworkId}-ep0${ep}"
        if (!getChildDevice(dni)) {
            log.info "Creating child device for Relay ${ep}"
            addChildDevice("hubitat", "Generic Component Switch", dni, [name: "${device.displayName} (Relay ${ep})", isComponent: true])
        } else {
            log.debug "Child device for Relay ${ep} already exists"
        }
    }
}