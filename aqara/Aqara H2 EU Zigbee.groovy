/**
 * Aqara H2 EU (WS-K08E) - v38.0
 * FIXED: Isolated Power (W) from Voltage (V) to prevent 2.1W overwriting 230V.
 * FIXED: Refined Tag 98 decoding for precise Line Voltage.
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
        capability "CurrentMeter"
        capability "VoltageMeasurement"
        capability "EnergyMeter"
        capability "TemperatureMeasurement"
        
        attribute "amperage", "number"
        attribute "voltage", "number"
        attribute "powerOutageCount", "number"
        
        command "setMultiClickMode", [[name: "Endpoint", type: "ENUM", constraints: ["04", "05"]], [name: "State", type: "ENUM", constraints: ["enabled", "disabled"]]]
        command "setOperationMode", [[name: "Endpoint", type: "ENUM", constraints: ["01", "02"]], [name: "Mode", type: "ENUM", constraints: ["control", "decoupled"]]]
        command "setLockRelay", [[name: "Endpoint", type: "ENUM", constraints: ["01", "02"]], [name: "State", type: "ENUM", constraints: ["unlocked", "locked"]]]
        command "setLedIndicator", [[name: "Endpoint", type: "ENUM", constraints: ["01", "02"]], [name: "State", type: "ENUM", constraints: ["off", "on"]]]

        command "createChildDevices"
        command "componentOn"
        command "componentOff"

        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0003,0004,0005,0006,0009,0702,0B04,FCC0", outClusters: "0019,000A", manufacturer: "LUMI", model: "lumi.switch.agl010"
    }
}

def parse(String description) {
    if (logEnable) log.debug "RAW: ${description}"
    def descMap = zigbee.parseDescriptionAsMap(description)
    String cluster = descMap.cluster?.toUpperCase()
    String attrId = descMap.attrId?.toUpperCase()
    
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

    // 1b. XIAOMI/AQARA BASIC STRUCTURE (OUTAGE COUNT + AUX METRICS)
    else if (cluster == "0000" && (attrId == "FF01" || attrId == "FF02")) {
        decodeXiaomiStruct(descMap.value)
    }
    
    // 2. ELECTRICAL MEASUREMENTS (VOLTAGE / CURRENT / POWER)
    else if (cluster == "0B04") {
        decodeElectricalMeasurement(descMap)
    }
    
    // 3. MULTI-CLICK BUTTONS
    else if (cluster == "0012") {
        int ep = Integer.parseInt(descMap.endpoint, 16)
        int btn = (ep == 1 || ep == 4) ? 1 : 2
        int val = Integer.parseInt(descMap.value, 16)
        if (val == 1) sendButtonEvent(btn, "pushed")
        else if (val == 2) sendButtonEvent(btn, "doubleTapped")
        else if (val == 0) sendButtonEvent(btn, "held")
    }
    
    // 4. RELAY STATUS
    else if (cluster == "0006") {
        def ep = descMap.endpoint
        def value = (descMap.value == "01" || descMap.command == "01") ? "on" : "off"
        def child = getChildDevice("${device.deviceNetworkId}-ep${ep}")
        if (child) child.parse([[name: "switch", value: value]])
        if (ep == "01") sendEvent(name: "switch", value: value)
    }
}

def decodeAqaraOutageAttr(String valueHex) {
    try {
        long raw = Long.parseLong(valueHex, 16)
        updatePowerOutageCount(Math.max(0, raw - 1))
    } catch (e) {
        log.error "Decoding 0002 error: ${e}"
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
        // INSTANT POWER FALLBACK (Tag 98)
        if (hexString.contains("9839")) {
            def sub = hexString.split("9839")[1]
            long val = Long.parseLong(swapEndianHex(sub.substring(0,8)), 16)
            float fVal = Float.intBitsToFloat(val.intValue())
            if (fVal >= 0 && fVal < 20000) {
                sendEvent(name: "power", value: fVal.round(1), unit: "W")
            }
        }
        // ENERGY (Tag 95)
        if (hexString.contains("9539")) {
            def sub = hexString.split("9539")[1]
            long val = Long.parseLong(swapEndianHex(sub.substring(0,8)), 16)
            float fVal = Float.intBitsToFloat(val.intValue())
            sendEvent(name: "energy", value: fVal.round(3), unit: "kWh")
        }
        // AMPS (Tag 97)
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

private void updatePowerOutageCount(long count) {
    if (count < 0) return
    def current = device.currentValue("powerOutageCount")
    if (!(current instanceof Number) || count >= (current as long)) {
        sendEvent(name: "powerOutageCount", value: count)
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

def refresh() {
    log.info "Refreshing all sensor data..."
    return (
        zigbee.readAttribute(0x0B04, 0x0508) +
        zigbee.readAttribute(0x0B04, 0x050B) +
        zigbee.readAttribute(0xFCC0, 0x0002, [mfgCode: "0x115F"]) +
        zigbee.readAttribute(0xFCC0, 0x00F7, [mfgCode: "0x115F"]) +
        zigbee.readAttribute(0xFCC0, 0xFFF2, [mfgCode: "0x115F"]) +
        zigbee.readAttribute(0x0000, 0xFF01, [mfgCode: "0x115F"]) +
        zigbee.readAttribute(0x0000, 0xFF02, [mfgCode: "0x115F"])
    )
}

// Commands
def setMultiClickMode(ep, state) {
    int val = (state == "enabled") ? 2 : 1
    sendHubCommand(new hubitat.device.HubMultiAction(zigbee.writeAttribute(0xFCC0, 0x0286, 0x20, val, [mfgCode: "0x115F", destEndpoint: Integer.parseInt(ep)]), hubitat.device.Protocol.ZIGBEE))
}
def setOperationMode(ep, mode) {
    int val = (mode == "decoupled") ? 0 : 1
    sendHubCommand(new hubitat.device.HubMultiAction(zigbee.writeAttribute(0xFCC0, 0x0200, 0x20, val, [mfgCode: "0x115F", destEndpoint: Integer.parseInt(ep)]), hubitat.device.Protocol.ZIGBEE))
}
def setLockRelay(ep, state) {
    int val = (state == "locked") ? 1 : 0
    sendHubCommand(new hubitat.device.HubMultiAction(zigbee.writeAttribute(0xFCC0, 0x0285, 0x20, val, [mfgCode: "0x115F", destEndpoint: Integer.parseInt(ep)]), hubitat.device.Protocol.ZIGBEE))
}
def setLedIndicator(ep, state) {
    int val = (state == "on") ? 1 : 0
    sendHubCommand(new hubitat.device.HubMultiAction(zigbee.writeAttribute(0xFCC0, 0x0203, 0x10, val, [mfgCode: "0x115F", destEndpoint: Integer.parseInt(ep)]), hubitat.device.Protocol.ZIGBEE))
}
def configure() { return ["he raw 0x${device.deviceNetworkId} 1 0x01 0x0000 {10 00 01 05 00 07 00 42 12 6C 75 6D 69 2E 73 77 69 74 63 68 2E 61 67 6C 30 31 30}"] }
def sendButtonEvent(btn, type) { sendEvent(name: type, value: btn, isStateChange: true) }
def on() { componentOn(getChildDevice("${device.deviceNetworkId}-ep01")) }
def off() { componentOff(getChildDevice("${device.deviceNetworkId}-ep01")) }
def componentOn(cd) { sendHubCommand(new hubitat.device.HubAction("he cmd 0x${device.deviceNetworkId} 0x${cd.deviceNetworkId.split("-ep")[-1]} 0x0006 1 {}", hubitat.device.Protocol.ZIGBEE)) }
def componentOff(cd) { sendHubCommand(new hubitat.device.HubAction("he cmd 0x${device.deviceNetworkId} 0x${cd.deviceNetworkId.split("-ep")[-1]} 0x0006 0 {}", hubitat.device.Protocol.ZIGBEE)) }
def createChildDevices() { [1, 2].each { ep -> String dni = "${device.deviceNetworkId}-ep0${ep}"; if (!getChildDevice(dni)) addChildDevice("hubitat", "Generic Component Switch", dni, [name: "${device.displayName} (Relay ${ep})", isComponent: true]) } }