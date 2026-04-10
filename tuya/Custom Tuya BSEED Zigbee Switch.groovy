/**
 * Custom Tuya BSEED Zigbee Switch
 *
 * Hubitat Elevate C8 Pro driver for custom Tuya Zigbee switches with romasku firmware
 * (https://github.com/romasku/tuya-zigbee-switch)
 *
 * Supported models: BSLR1 (1-gang), BSLR2 (2-gang)
 *
 * Author: Adrian Dobre
 */

import groovy.transform.Field
import hubitat.device.HubAction
import hubitat.device.HubMultiAction
import hubitat.device.Protocol

metadata {
    definition(name: "Custom Tuya BSEED Zigbee Switch", namespace: "madtek", author: "Adrian Dobre") {
        capability "Configuration"
        capability "Refresh"
        capability "Switch"
        capability "Actuator"
        capability "PushableButton"

        // Global config attributes
        attribute "device_config", "string"
        attribute "multi_press_reset_count", "number"
        attribute "network_led", "enum", ["ON", "OFF"]

        // Switch attributes (generic - used by BSLR1 single switch)
        attribute "switch_press_action", "enum", ["released", "press", "long_press", "position_on", "position_off"]
        attribute "switch_mode", "enum", ["toggle", "momentary", "momentary_nc"]
        attribute "switch_action_mode", "enum", ["on_off", "off_on", "toggle_simple", "toggle_smart_sync", "toggle_smart_opposite"]
        attribute "switch_relay_mode", "enum", ["detached", "press_start", "short_press", "long_press"]
        attribute "switch_relay_index", "enum", ["relay_1", "relay_2"]
        attribute "switch_binded_mode", "enum", ["press_start", "short_press", "long_press"]
        attribute "switch_long_press_duration", "number"
        attribute "switch_level_move_rate", "number"

        // Switch left attributes (used by BSLR2)
        attribute "switch_left_press_action", "enum", ["released", "press", "long_press", "position_on", "position_off"]
        attribute "switch_left_mode", "enum", ["toggle", "momentary", "momentary_nc"]
        attribute "switch_left_action_mode", "enum", ["on_off", "off_on", "toggle_simple", "toggle_smart_sync", "toggle_smart_opposite"]
        attribute "switch_left_relay_mode", "enum", ["detached", "press_start", "short_press", "long_press"]
        attribute "switch_left_relay_index", "enum", ["relay_1", "relay_2"]
        attribute "switch_left_binded_mode", "enum", ["press_start", "short_press", "long_press"]
        attribute "switch_left_long_press_duration", "number"
        attribute "switch_left_level_move_rate", "number"

        // Switch right attributes (used by BSLR2)
        attribute "switch_right_press_action", "enum", ["released", "press", "long_press", "position_on", "position_off"]
        attribute "switch_right_mode", "enum", ["toggle", "momentary", "momentary_nc"]
        attribute "switch_right_action_mode", "enum", ["on_off", "off_on", "toggle_simple", "toggle_smart_sync", "toggle_smart_opposite"]
        attribute "switch_right_relay_mode", "enum", ["detached", "press_start", "short_press", "long_press"]
        attribute "switch_right_relay_index", "enum", ["relay_1", "relay_2"]
        attribute "switch_right_binded_mode", "enum", ["press_start", "short_press", "long_press"]
        attribute "switch_right_long_press_duration", "number"
        attribute "switch_right_level_move_rate", "number"

        // Relay indicator attributes (BSLR1)
        attribute "relay_indicator_mode", "enum", ["same", "opposite", "manual"]
        attribute "relay_indicator", "enum", ["ON", "OFF"]

        // Relay indicator attributes (BSLR2)
        attribute "relay_left_indicator_mode", "enum", ["same", "opposite", "manual"]
        attribute "relay_left_indicator", "enum", ["ON", "OFF"]
        attribute "relay_right_indicator_mode", "enum", ["same", "opposite", "manual"]
        attribute "relay_right_indicator", "enum", ["ON", "OFF"]

        // Power-on behavior attributes (BSLR1)
        attribute "power_on_behavior", "enum", ["off", "on", "toggle", "previous"]

        // Power-on behavior attributes (BSLR2)
        attribute "power_on_behavior_relay_left", "enum", ["off", "on", "toggle", "previous"]
        attribute "power_on_behavior_relay_right", "enum", ["off", "on", "toggle", "previous"]

        // Relay state attributes
        attribute "relay_left", "string"
        attribute "relay_right", "string"

        // Global config commands
        command "setDeviceConfig", [[name: "device_config*", type: "STRING",
            description: "Current configuration of the device"]]
        command "setMultiPressResetCount", [[name: "multi_press_reset_count*", type: "NUMBER",
            description: "Number of consecutive presses to trigger factory reset (0 = disabled)", constraints: ["NUMBER"]]]
        command "setNetworkLed", [[name: "network_led*", type: "ENUM", constraints: ["ON", "OFF"],
            description: "State of the network indicator LED"]]

        // Switch config commands (BSLR1)
        command "setSwitchMode", [[name: "switch_mode*", type: "ENUM", constraints: ["toggle", "momentary", "momentary_nc"],
            description: "Select the type of switch connected to the device"]]
        command "setSwitchActionMode", [[name: "switch_action_mode*", type: "ENUM", constraints: ["on_off", "off_on", "toggle_simple", "toggle_smart_sync", "toggle_smart_opposite"],
            description: "Select how switch should work:\n- on_off: When switch physically moved to position 1 it always generates ON command, and when moved to position 2 it generates OFF command\n- off_on: Same as on_off, but positions are swapped\n- toggle_simple: Any press of physical switch will TOGGLE the relay and send TOGGLE command to binds\n- toggle_smart_sync: Any press of physical switch will TOGGLE the relay and send corresponding ON/OFF command to keep binds in sync with relay\n- toggle_smart_opposite: Any press of physical switch: TOGGLE the relay and send corresponding ON/OFF command to keep binds in the state opposite to the relay"]]
        command "setSwitchRelayMode", [[name: "switch_relay_mode*", type: "ENUM", constraints: ["detached", "press_start", "short_press", "long_press"],
            description: "When to turn on/off internal relay"]]
        command "setSwitchRelayIndex", [[name: "switch_relay_index*", type: "ENUM", constraints: ["relay_1", "relay_2"],
            description: "Which internal relay it should trigger"]]
        command "setSwitchBindedMode", [[name: "switch_binded_mode*", type: "ENUM", constraints: ["press_start", "short_press", "long_press"],
            description: "When turn on/off binded device"]]
        command "setSwitchLongPressDuration", [[name: "switch_long_press_duration*", type: "NUMBER",
            description: "What duration is considerd to be long press", constraints: ["NUMBER"]]]
        command "setSwitchLevelMoveRate", [[name: "switch_level_move_rate*", type: "NUMBER",
            description: "Level (dim) move rate in steps per ms", constraints: ["NUMBER"]]]

        // Switch left config commands (BSLR2)
        command "setSwitchLeftMode", [[name: "switch_left_mode*", type: "ENUM", constraints: ["toggle", "momentary", "momentary_nc"],
            description: "Select the type of switch connected to the device"]]
        command "setSwitchLeftActionMode", [[name: "switch_left_action_mode*", type: "ENUM", constraints: ["on_off", "off_on", "toggle_simple", "toggle_smart_sync", "toggle_smart_opposite"],
            description: "Select how switch should work:\n- on_off: When switch physically moved to position 1 it always generates ON command, and when moved to position 2 it generates OFF command\n- off_on: Same as on_off, but positions are swapped\n- toggle_simple: Any press of physical switch will TOGGLE the relay and send TOGGLE command to binds\n- toggle_smart_sync: Any press of physical switch will TOGGLE the relay and send corresponding ON/OFF command to keep binds in sync with relay\n- toggle_smart_opposite: Any press of physical switch: TOGGLE the relay and send corresponding ON/OFF command to keep binds in the state opposite to the relay"]]
        command "setSwitchLeftRelayMode", [[name: "switch_left_relay_mode*", type: "ENUM", constraints: ["detached", "press_start", "short_press", "long_press"],
            description: "When to turn on/off internal relay"]]
        command "setSwitchLeftRelayIndex", [[name: "switch_left_relay_index*", type: "ENUM", constraints: ["relay_1", "relay_2"],
            description: "Which internal relay it should trigger"]]
        command "setSwitchLeftBindedMode", [[name: "switch_left_binded_mode*", type: "ENUM", constraints: ["press_start", "short_press", "long_press"],
            description: "When turn on/off binded device"]]
        command "setSwitchLeftLongPressDuration", [[name: "switch_left_long_press_duration*", type: "NUMBER",
            description: "What duration is considerd to be long press", constraints: ["NUMBER"]]]
        command "setSwitchLeftLevelMoveRate", [[name: "switch_left_level_move_rate*", type: "NUMBER",
            description: "Level (dim) move rate in steps per ms", constraints: ["NUMBER"]]]

        // Switch right config commands (BSLR2)
        command "setSwitchRightMode", [[name: "switch_right_mode*", type: "ENUM", constraints: ["toggle", "momentary", "momentary_nc"],
            description: "Select the type of switch connected to the device"]]
        command "setSwitchRightActionMode", [[name: "switch_right_action_mode*", type: "ENUM", constraints: ["on_off", "off_on", "toggle_simple", "toggle_smart_sync", "toggle_smart_opposite"],
            description: "Select how switch should work:\n- on_off: When switch physically moved to position 1 it always generates ON command, and when moved to position 2 it generates OFF command\n- off_on: Same as on_off, but positions are swapped\n- toggle_simple: Any press of physical switch will TOGGLE the relay and send TOGGLE command to binds\n- toggle_smart_sync: Any press of physical switch will TOGGLE the relay and send corresponding ON/OFF command to keep binds in sync with relay\n- toggle_smart_opposite: Any press of physical switch: TOGGLE the relay and send corresponding ON/OFF command to keep binds in the state opposite to the relay"]]
        command "setSwitchRightRelayMode", [[name: "switch_right_relay_mode*", type: "ENUM", constraints: ["detached", "press_start", "short_press", "long_press"],
            description: "When to turn on/off internal relay"]]
        command "setSwitchRightRelayIndex", [[name: "switch_right_relay_index*", type: "ENUM", constraints: ["relay_1", "relay_2"],
            description: "Which internal relay it should trigger"]]
        command "setSwitchRightBindedMode", [[name: "switch_right_binded_mode*", type: "ENUM", constraints: ["press_start", "short_press", "long_press"],
            description: "When turn on/off binded device"]]
        command "setSwitchRightLongPressDuration", [[name: "switch_right_long_press_duration*", type: "NUMBER",
            description: "What duration is considerd to be long press", constraints: ["NUMBER"]]]
        command "setSwitchRightLevelMoveRate", [[name: "switch_right_level_move_rate*", type: "NUMBER",
            description: "Level (dim) move rate in steps per ms", constraints: ["NUMBER"]]]

        // Power-on behavior commands (BSLR1)
        command "setPowerOnBehavior", [[name: "power_on_behavior*", type: "ENUM", constraints: ["off", "on", "toggle", "previous"],
            description: "Controls the behavior when the device is powered on after power loss"]]

        // Power-on behavior commands (BSLR2)
        command "setPowerOnBehaviorRelayLeft", [[name: "power_on_behavior*", type: "ENUM", constraints: ["off", "on", "toggle", "previous"],
            description: "Controls the behavior when the device is powered on after power loss"]]
        command "setPowerOnBehaviorRelayRight", [[name: "power_on_behavior*", type: "ENUM", constraints: ["off", "on", "toggle", "previous"],
            description: "Controls the behavior when the device is powered on after power loss"]]

        // Relay indicator commands (BSLR1)
        command "setRelayIndicatorMode", [[name: "relay_indicator_mode*", type: "ENUM", constraints: ["same", "opposite", "manual"],
            description: "Mode for the relay indicator LED"]]
        command "setRelayIndicator", [[name: "relay_indicator*", type: "ENUM", constraints: ["ON", "OFF"],
            description: "State of the relay indicator LED"]]

        // Relay indicator commands (BSLR2)
        command "setRelayLeftIndicatorMode", [[name: "relay_left_indicator_mode*", type: "ENUM", constraints: ["same", "opposite", "manual"],
            description: "Mode for the relay indicator LED"]]
        command "setRelayLeftIndicator", [[name: "relay_left_indicator*", type: "ENUM", constraints: ["ON", "OFF"],
            description: "State of the relay indicator LED"]]
        command "setRelayRightIndicatorMode", [[name: "relay_right_indicator_mode*", type: "ENUM", constraints: ["same", "opposite", "manual"],
            description: "Mode for the relay indicator LED"]]
        command "setRelayRightIndicator", [[name: "relay_right_indicator*", type: "ENUM", constraints: ["ON", "OFF"],
            description: "State of the relay indicator LED"]]

        command "createChildDevices"
        command "componentOn"
        command "componentOff"
        command "componentRefresh"

        // BSLR1 - 1 gang
        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0007,0012", outClusters: "0019,0006,0008", manufacturer: "xk5udnd6", model: "BSLR1", deviceJoinName: "Custom Tuya BSEED Zigbee Switch 1-Gang"
        // BSLR2 - 2 gang
        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0007,0012", outClusters: "0019,0006,0008", manufacturer: "xk5udnd6", model: "BSLR2", deviceJoinName: "Custom Tuya BSEED Zigbee Switch 2-Gang"
        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0007,0012", outClusters: "0019,0006,0008", manufacturer: "xk5udnd6", model: "Bseed-2-gang-2", deviceJoinName: "Custom Tuya BSEED Zigbee Switch 2-Gang"
        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0007,0012", outClusters: "0019,0006,0008", manufacturer: "xk5udnd6", model: "Bseed-2-gang-2-ED", deviceJoinName: "Custom Tuya BSEED Zigbee Switch 2-Gang"
    }

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    }
}

// ===== Cluster and Attribute Constants =====

@Field static final int CLUSTER_ON_OFF = 0x0006
@Field static final int CLUSTER_ON_OFF_SWITCH_CFG = 0x0007
@Field static final int CLUSTER_MULTISTATE_INPUT = 0x0012
@Field static final int CLUSTER_BASIC = 0x0000

// genOnOffSwitchCfg attributes
@Field static final int ATTR_SWITCH_ACTION = 0x0010       // Enum8 - switchAction
@Field static final int ATTR_SWITCH_MODE = 0xFF00          // Enum8 - switchMode
@Field static final int ATTR_RELAY_MODE = 0xFF01           // Enum8 - relayMode
@Field static final int ATTR_RELAY_INDEX = 0xFF02          // uint8 - relayIndex
@Field static final int ATTR_LONG_PRESS_DURATION = 0xFF03  // uint16 - longPressDuration
@Field static final int ATTR_LEVEL_MOVE_RATE = 0xFF04      // uint8 - levelMoveRate
@Field static final int ATTR_BINDED_MODE = 0xFF05          // Enum8 - bindedMode

// genOnOff attributes
@Field static final int ATTR_START_UP_ON_OFF = 0x4003      // Enum8 - startUpOnOff (power-on behavior)
@Field static final int ATTR_RELAY_INDICATOR_MODE = 0xFF01 // Enum8
@Field static final int ATTR_RELAY_INDICATOR = 0xFF02      // Boolean

// genBasic attributes
@Field static final int ATTR_DEVICE_CONFIG = 0xFF00        // long string
@Field static final int ATTR_NETWORK_INDICATOR = 0xFF01    // Boolean
@Field static final int ATTR_MULTI_PRESS_RESET = 0xFF02    // uint8

// genMultistateInput attributes
@Field static final int ATTR_PRESENT_VALUE = 0x0055        // uint16

// Data type constants
@Field static final int DATA_TYPE_BOOLEAN = 0x10
@Field static final int DATA_TYPE_UINT8 = 0x20
@Field static final int DATA_TYPE_UINT16 = 0x21
@Field static final int DATA_TYPE_ENUM8 = 0x30
@Field static final int DATA_TYPE_LONG_OCTET_STRING = 0x44

// Lookup maps
@Field static final Map SWITCH_ACTION_MAP = [0: "on_off", 1: "off_on", 2: "toggle_simple", 3: "toggle_smart_sync", 4: "toggle_smart_opposite"]
@Field static final Map SWITCH_ACTION_REVERSE = ["on_off": 0, "off_on": 1, "toggle_simple": 2, "toggle_smart_sync": 3, "toggle_smart_opposite": 4]

@Field static final Map SWITCH_MODE_MAP = [0: "toggle", 1: "momentary", 2: "momentary_nc"]
@Field static final Map SWITCH_MODE_REVERSE = ["toggle": 0, "momentary": 1, "momentary_nc": 2]

@Field static final Map RELAY_MODE_MAP = [0: "detached", 1: "press_start", 3: "short_press", 2: "long_press"]
@Field static final Map RELAY_MODE_REVERSE = ["detached": 0, "press_start": 1, "short_press": 3, "long_press": 2]

@Field static final Map RELAY_INDEX_MAP = [1: "relay_1", 2: "relay_2"]
@Field static final Map RELAY_INDEX_REVERSE = ["relay_1": 1, "relay_2": 2]

@Field static final Map BINDED_MODE_MAP = [1: "press_start", 3: "short_press", 2: "long_press"]
@Field static final Map BINDED_MODE_REVERSE = ["press_start": 1, "short_press": 3, "long_press": 2]

@Field static final Map PRESS_ACTION_MAP = [0: "released", 1: "press", 2: "long_press", 3: "position_on", 4: "position_off"]

@Field static final Map RELAY_INDICATOR_MODE_MAP = [0: "same", 1: "opposite", 2: "manual"]
@Field static final Map RELAY_INDICATOR_MODE_REVERSE = ["same": 0, "opposite": 1, "manual": 2]

@Field static final Map POWER_ON_BEHAVIOR_MAP = [0: "off", 1: "on", 2: "toggle", 255: "previous"]
@Field static final Map POWER_ON_BEHAVIOR_REVERSE = ["off": 0, "on": 1, "toggle": 2, "previous": 255]

// Button event mapping from press action
@Field static final Map PRESS_ACTION_BUTTON_EVENT = ["press": "pushed", "long_press": "held"]

// ===== Lifecycle =====

def installed() {
    log.info "Custom Tuya BSEED Zigbee Switch installed"
    sendEvent(name: "numberOfButtons", value: getModel() == "BSLR2" ? 2 : 1)
    createChildDevices()
}

def updated() {
    log.info "Custom Tuya BSEED Zigbee Switch updated"
    sendEvent(name: "numberOfButtons", value: getModel() == "BSLR2" ? 2 : 1)
    createChildDevices()
    if (logEnable) {
        log.warn "Debug logging enabled for 30 minutes"
        runIn(1800, "logsOff")
    }
}

def logsOff() {
    log.warn "Debug logging disabled"
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

// ===== Model Detection =====

private String getModel() {
    String model = device.getDataValue("model") ?: ""
    if (model in ["BSLR2", "Bseed-2-gang-2", "Bseed-2-gang-2-ED"]) return "BSLR2"
    return "BSLR1"
}

private boolean isBSLR2() {
    return getModel() == "BSLR2"
}

// ===== Endpoint Mapping =====
// BSLR1: switch=1, relay=2
// BSLR2: switch_left=1, switch_right=2, relay_left=3, relay_right=4

private int getSwitchEndpoint(String side = null) {
    if (isBSLR2()) {
        return (side == "right") ? 2 : 1
    }
    return 1
}

private int getRelayEndpoint(String side = null) {
    if (isBSLR2()) {
        return (side == "right") ? 4 : 3
    }
    return 2
}

// ===== Parse =====

def parse(String description) {
    // Skip ZDP messages (profile 0000) — bind responses, active endpoints, etc.
    if (description.startsWith("catchall: 0000")) return

    Map descMap
    try {
        descMap = zigbee.parseDescriptionAsMap(description)
    } catch (e) {
        if (logEnable) log.debug "Could not parse description: ${e.message}"
        return
    }

    // Skip protocol overhead: 04=write attr response, 07=configure reporting response
    if (descMap.command in ["04", "07"]) return

    // Default response (0B): log status if error, skip otherwise
    if (descMap.command == "0B") {
        if (descMap.data?.size() >= 2 && descMap.data[1] != "00") {
            log.warn "Command ${descMap.data[0]} failed on cluster 0x${descMap.clusterId} with status 0x${descMap.data[1]}"
        }
        return
    }

    if (logEnable) log.debug "parse: ${description}"

    int cluster = descMap.clusterInt ?: 0
    int attrId = descMap.attrInt ?: 0
    int endpoint = 0
    try {
        endpoint = descMap.endpoint ? Integer.parseInt(descMap.endpoint, 16) : 0
    } catch (e) {
        if (logEnable) log.debug "Could not parse endpoint '${descMap.endpoint}', skipping"
        return
    }

    try {
        if (cluster == CLUSTER_ON_OFF) {
            parseOnOff(descMap, endpoint)
        } else if (cluster == CLUSTER_ON_OFF_SWITCH_CFG) {
            parseSwitchConfig(descMap, endpoint, attrId)
        } else if (cluster == CLUSTER_MULTISTATE_INPUT) {
            parsePressAction(descMap, endpoint, attrId)
        } else if (cluster == CLUSTER_BASIC) {
            parseBasic(descMap, endpoint, attrId)
        }
    } catch (e) {
        if (logEnable) log.debug "Error processing message: ${e.message}"
    }
}

private void parseOnOff(Map descMap, int endpoint) {
    // Relay on/off state
    if (descMap.attrInt == 0x0000 || descMap.command in ["01", "00", "0B"]) {
        String value
        if (descMap.command == "0B") return // default response, ignore
        if (descMap.value != null) {
            value = (descMap.value == "01") ? "on" : "off"
        } else if (descMap.command == "01") {
            value = "on"
        } else if (descMap.command == "00") {
            value = "off"
        } else {
            return
        }

        if (isBSLR2()) {
            if (endpoint == 3) {
                sendEvent(name: "switch", value: value)
                sendEvent(name: "relay_left", value: value)
                def child = getChildDevice("${device.deviceNetworkId}-relay_left")
                if (child) child.parse([[name: "switch", value: value]])
            } else if (endpoint == 4) {
                sendEvent(name: "relay_right", value: value)
                def child = getChildDevice("${device.deviceNetworkId}-relay_right")
                if (child) child.parse([[name: "switch", value: value]])
            }
        } else {
            if (endpoint == 2) {
                sendEvent(name: "switch", value: value)
            }
        }
    }

    // Power-on behavior (genOnOff 0x4003)
    if (descMap.attrInt == ATTR_START_UP_ON_OFF) {
        int rawVal = Integer.parseInt(descMap.value, 16)
        String behavior = POWER_ON_BEHAVIOR_MAP[rawVal]
        if (behavior) {
            String attrName = getPowerOnBehaviorAttrName(endpoint)
            if (attrName) sendEvent(name: attrName, value: behavior)
        }
    }

    // Relay indicator (genOnOff 0xFF01 and 0xFF02)
    if (descMap.attrInt == ATTR_RELAY_INDICATOR_MODE) {
        String mode = RELAY_INDICATOR_MODE_MAP[Integer.parseInt(descMap.value, 16)]
        if (mode) {
            String attrName = getRelayIndicatorModeAttrName(endpoint)
            if (attrName) sendEvent(name: attrName, value: mode)
        }
    } else if (descMap.attrInt == ATTR_RELAY_INDICATOR) {
        String val = (descMap.value == "01") ? "ON" : "OFF"
        String attrName = getRelayIndicatorAttrName(endpoint)
        if (attrName) sendEvent(name: attrName, value: val)
    }
}

private String getPowerOnBehaviorAttrName(int endpoint) {
    if (isBSLR2()) {
        if (endpoint == 3) return "power_on_behavior_relay_left"
        if (endpoint == 4) return "power_on_behavior_relay_right"
    } else {
        if (endpoint == 2) return "power_on_behavior"
    }
    return null
}

private String getRelayIndicatorModeAttrName(int endpoint) {
    if (isBSLR2()) {
        if (endpoint == 3) return "relay_left_indicator_mode"
        if (endpoint == 4) return "relay_right_indicator_mode"
    } else {
        if (endpoint == 2) return "relay_indicator_mode"
    }
    return null
}

private String getRelayIndicatorAttrName(int endpoint) {
    if (isBSLR2()) {
        if (endpoint == 3) return "relay_left_indicator"
        if (endpoint == 4) return "relay_right_indicator"
    } else {
        if (endpoint == 2) return "relay_indicator"
    }
    return null
}

private void parseSwitchConfig(Map descMap, int endpoint, int attrId) {
    String prefix = getSwitchPrefix(endpoint)
    if (!prefix) return

    int rawValue = Integer.parseInt(descMap.value, 16)

    switch (attrId) {
        case ATTR_SWITCH_ACTION:
            String val = SWITCH_ACTION_MAP[rawValue]
            if (val) sendEvent(name: "${prefix}_action_mode", value: val)
            break
        case ATTR_SWITCH_MODE:
            String val = SWITCH_MODE_MAP[rawValue]
            if (val) sendEvent(name: "${prefix}_mode", value: val)
            break
        case ATTR_RELAY_MODE:
            String val = RELAY_MODE_MAP[rawValue]
            if (val) sendEvent(name: "${prefix}_relay_mode", value: val)
            break
        case ATTR_RELAY_INDEX:
            String val = RELAY_INDEX_MAP[rawValue]
            if (val) sendEvent(name: "${prefix}_relay_index", value: val)
            break
        case ATTR_LONG_PRESS_DURATION:
            sendEvent(name: "${prefix}_long_press_duration", value: rawValue)
            break
        case ATTR_LEVEL_MOVE_RATE:
            sendEvent(name: "${prefix}_level_move_rate", value: rawValue)
            break
        case ATTR_BINDED_MODE:
            String val = BINDED_MODE_MAP[rawValue]
            if (val) sendEvent(name: "${prefix}_binded_mode", value: val)
            break
    }
}

private void parsePressAction(Map descMap, int endpoint, int attrId) {
    if (attrId != ATTR_PRESENT_VALUE) return
    int rawValue = Integer.parseInt(descMap.value, 16)
    String action = PRESS_ACTION_MAP[rawValue]
    if (!action) return

    String prefix = getSwitchPrefix(endpoint)
    if (!prefix) return

    sendEvent(name: "${prefix}_press_action", value: action)

    // Generate button events
    String buttonEvent = PRESS_ACTION_BUTTON_EVENT[action]
    if (buttonEvent) {
        int buttonNumber = isBSLR2() ? endpoint : 1
        sendEvent(name: buttonEvent, value: buttonNumber, isStateChange: true)
    }
}

private void parseBasic(Map descMap, int endpoint, int attrId) {
    switch (attrId) {
        case ATTR_DEVICE_CONFIG:
            String config = hexToString(descMap.value)
            sendEvent(name: "device_config", value: config)
            break
        case ATTR_NETWORK_INDICATOR:
            String val = (descMap.value == "01") ? "ON" : "OFF"
            sendEvent(name: "network_led", value: val)
            break
        case ATTR_MULTI_PRESS_RESET:
            int val = Integer.parseInt(descMap.value, 16)
            sendEvent(name: "multi_press_reset_count", value: val)
            break
    }
}

private String getSwitchPrefix(int endpoint) {
    if (isBSLR2()) {
        if (endpoint == 1) return "switch_left"
        if (endpoint == 2) return "switch_right"
    } else {
        if (endpoint == 1) return "switch"
    }
    return null
}

// ===== On/Off Commands =====

def on() {
    if (logEnable) log.debug "on()"
    if (isBSLR2()) {
        sendZigbeeCommands([
            "he cmd 0x${device.deviceNetworkId} 0x${intToHexStr(3)} 0x${intToHexStr(CLUSTER_ON_OFF, 2)} 1 {}",
            "he cmd 0x${device.deviceNetworkId} 0x${intToHexStr(4)} 0x${intToHexStr(CLUSTER_ON_OFF, 2)} 1 {}"
        ], 200)
    } else {
        sendZigbeeCmd("he cmd 0x${device.deviceNetworkId} 0x${intToHexStr(2)} 0x${intToHexStr(CLUSTER_ON_OFF, 2)} 1 {}")
    }
}

def off() {
    if (logEnable) log.debug "off()"
    if (isBSLR2()) {
        sendZigbeeCommands([
            "he cmd 0x${device.deviceNetworkId} 0x${intToHexStr(3)} 0x${intToHexStr(CLUSTER_ON_OFF, 2)} 0 {}",
            "he cmd 0x${device.deviceNetworkId} 0x${intToHexStr(4)} 0x${intToHexStr(CLUSTER_ON_OFF, 2)} 0 {}"
        ], 200)
    } else {
        sendZigbeeCmd("he cmd 0x${device.deviceNetworkId} 0x${intToHexStr(2)} 0x${intToHexStr(CLUSTER_ON_OFF, 2)} 0 {}")
    }
}

// ===== Child Device Commands =====

def componentOn(cd) {
    if (logEnable) log.debug "componentOn(${cd.deviceNetworkId})"
    int ep = childEndpoint(cd)
    sendZigbeeCmd("he cmd 0x${device.deviceNetworkId} 0x${intToHexStr(ep)} 0x${intToHexStr(CLUSTER_ON_OFF, 2)} 1 {}")
}

def componentOff(cd) {
    if (logEnable) log.debug "componentOff(${cd.deviceNetworkId})"
    int ep = childEndpoint(cd)
    sendZigbeeCmd("he cmd 0x${device.deviceNetworkId} 0x${intToHexStr(ep)} 0x${intToHexStr(CLUSTER_ON_OFF, 2)} 0 {}")
}

def componentRefresh(cd) {
    refresh()
}

private int childEndpoint(cd) {
    String dni = cd.deviceNetworkId
    if (dni.endsWith("-relay_left")) return 3
    if (dni.endsWith("-relay_right")) return 4
    return 2 // BSLR1 relay
}

// ===== Global Config Commands =====

def setDeviceConfig(String config) {
    if (logEnable) log.debug "setDeviceConfig(${config})"
    int ep = isBSLR2() ? 1 : 1  // switch_left or switch endpoint
    String hexConfig = stringToHex(config)
    int len = config.length()
    // Long octet string: 2-byte length (little-endian) + data
    String lenHex = zigbee.convertToHexString(len, 4)
    String payload = zigbee.swapOctets(lenHex) + hexConfig
    sendZigbeeCommands(zigbee.writeAttribute(CLUSTER_BASIC, ATTR_DEVICE_CONFIG, DATA_TYPE_LONG_OCTET_STRING, payload, [destEndpoint: ep]))
}

def setMultiPressResetCount(BigDecimal count) {
    if (logEnable) log.debug "setMultiPressResetCount(${count})"
    int val = count.intValue()
    if (val < 0 || val > 255) { log.error "multi_press_reset_count must be 0-255"; return }
    int ep = isBSLR2() ? 1 : 1
    sendZigbeeCommands(zigbee.writeAttribute(CLUSTER_BASIC, ATTR_MULTI_PRESS_RESET, DATA_TYPE_UINT8, val, [destEndpoint: ep]))
}

def setNetworkLed(String state) {
    if (logEnable) log.debug "setNetworkLed(${state})"
    int val = (state == "ON") ? 1 : 0
    int ep = isBSLR2() ? 1 : 1
    sendZigbeeCommands(zigbee.writeAttribute(CLUSTER_BASIC, ATTR_NETWORK_INDICATOR, DATA_TYPE_BOOLEAN, val, [destEndpoint: ep]))
}

// ===== Switch Config Command Helpers =====

private void writeSwitchAction(int endpoint, String value) {
    Integer val = SWITCH_ACTION_REVERSE[value]
    if (val == null) { log.error "Invalid switch_action_mode: ${value}"; return }
    sendZigbeeCommands(zigbee.writeAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_SWITCH_ACTION, DATA_TYPE_ENUM8, val, [destEndpoint: endpoint]))
}

private void writeSwitchMode(int endpoint, String value) {
    Integer val = SWITCH_MODE_REVERSE[value]
    if (val == null) { log.error "Invalid switch_mode: ${value}"; return }
    sendZigbeeCommands(zigbee.writeAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_SWITCH_MODE, DATA_TYPE_ENUM8, val, [destEndpoint: endpoint]))
}

private void writeRelayMode(int endpoint, String value) {
    Integer val = RELAY_MODE_REVERSE[value]
    if (val == null) { log.error "Invalid relay_mode: ${value}"; return }
    sendZigbeeCommands(zigbee.writeAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_RELAY_MODE, DATA_TYPE_ENUM8, val, [destEndpoint: endpoint]))
}

private void writeRelayIndex(int endpoint, String value) {
    Integer val = RELAY_INDEX_REVERSE[value]
    if (val == null) { log.error "Invalid relay_index: ${value}"; return }
    sendZigbeeCommands(zigbee.writeAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_RELAY_INDEX, DATA_TYPE_UINT8, val, [destEndpoint: endpoint]))
}

private void writeBindedMode(int endpoint, String value) {
    Integer val = BINDED_MODE_REVERSE[value]
    if (val == null) { log.error "Invalid binded_mode: ${value}"; return }
    sendZigbeeCommands(zigbee.writeAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_BINDED_MODE, DATA_TYPE_ENUM8, val, [destEndpoint: endpoint]))
}

private void writeLongPressDuration(int endpoint, int value) {
    if (value < 0 || value > 5000) { log.error "long_press_duration must be 0-5000"; return }
    sendZigbeeCommands(zigbee.writeAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_LONG_PRESS_DURATION, DATA_TYPE_UINT16, value, [destEndpoint: endpoint]))
}

private void writeLevelMoveRate(int endpoint, int value) {
    if (value < 1 || value > 255) { log.error "level_move_rate must be 1-255"; return }
    sendZigbeeCommands(zigbee.writeAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_LEVEL_MOVE_RATE, DATA_TYPE_UINT8, value, [destEndpoint: endpoint]))
}

private void writePowerOnBehavior(int endpoint, String value) {
    Integer val = POWER_ON_BEHAVIOR_REVERSE[value]
    if (val == null) { log.error "Invalid power_on_behavior: ${value}"; return }
    sendZigbeeCommands(zigbee.writeAttribute(CLUSTER_ON_OFF, ATTR_START_UP_ON_OFF, DATA_TYPE_ENUM8, val, [destEndpoint: endpoint]))
}

private void writeRelayIndicatorMode(int endpoint, String value) {
    Integer val = RELAY_INDICATOR_MODE_REVERSE[value]
    if (val == null) { log.error "Invalid relay_indicator_mode: ${value}"; return }
    sendZigbeeCommands(zigbee.writeAttribute(CLUSTER_ON_OFF, ATTR_RELAY_INDICATOR_MODE, DATA_TYPE_ENUM8, val, [destEndpoint: endpoint]))
}

private void writeRelayIndicator(int endpoint, String value) {
    int val = (value == "ON") ? 1 : 0
    sendZigbeeCommands(zigbee.writeAttribute(CLUSTER_ON_OFF, ATTR_RELAY_INDICATOR, DATA_TYPE_BOOLEAN, val, [destEndpoint: endpoint]))
}

// ===== BSLR1 Switch Config Commands =====

def setSwitchMode(String value) { writeSwitchMode(1, value) }
def setSwitchActionMode(String value) { writeSwitchAction(1, value) }
def setSwitchRelayMode(String value) { writeRelayMode(1, value) }
def setSwitchRelayIndex(String value) { writeRelayIndex(1, value) }
def setSwitchBindedMode(String value) { writeBindedMode(1, value) }
def setSwitchLongPressDuration(BigDecimal value) { writeLongPressDuration(1, value.intValue()) }
def setSwitchLevelMoveRate(BigDecimal value) { writeLevelMoveRate(1, value.intValue()) }

// ===== BSLR2 Switch Left Config Commands =====

def setSwitchLeftMode(String value) { writeSwitchMode(1, value) }
def setSwitchLeftActionMode(String value) { writeSwitchAction(1, value) }
def setSwitchLeftRelayMode(String value) { writeRelayMode(1, value) }
def setSwitchLeftRelayIndex(String value) { writeRelayIndex(1, value) }
def setSwitchLeftBindedMode(String value) { writeBindedMode(1, value) }
def setSwitchLeftLongPressDuration(BigDecimal value) { writeLongPressDuration(1, value.intValue()) }
def setSwitchLeftLevelMoveRate(BigDecimal value) { writeLevelMoveRate(1, value.intValue()) }

// ===== BSLR2 Switch Right Config Commands =====

def setSwitchRightMode(String value) { writeSwitchMode(2, value) }
def setSwitchRightActionMode(String value) { writeSwitchAction(2, value) }
def setSwitchRightRelayMode(String value) { writeRelayMode(2, value) }
def setSwitchRightRelayIndex(String value) { writeRelayIndex(2, value) }
def setSwitchRightBindedMode(String value) { writeBindedMode(2, value) }
def setSwitchRightLongPressDuration(BigDecimal value) { writeLongPressDuration(2, value.intValue()) }
def setSwitchRightLevelMoveRate(BigDecimal value) { writeLevelMoveRate(2, value.intValue()) }

// ===== Relay Indicator Commands =====

// ===== Power-on Behavior Commands =====

// BSLR1
def setPowerOnBehavior(String value) { writePowerOnBehavior(2, value) }

// BSLR2
def setPowerOnBehaviorRelayLeft(String value) { writePowerOnBehavior(3, value) }
def setPowerOnBehaviorRelayRight(String value) { writePowerOnBehavior(4, value) }

// BSLR1
def setRelayIndicatorMode(String value) { writeRelayIndicatorMode(2, value) }
def setRelayIndicator(String value) { writeRelayIndicator(2, value) }

// BSLR2
def setRelayLeftIndicatorMode(String value) { writeRelayIndicatorMode(3, value) }
def setRelayLeftIndicator(String value) { writeRelayIndicator(3, value) }
def setRelayRightIndicatorMode(String value) { writeRelayIndicatorMode(4, value) }
def setRelayRightIndicator(String value) { writeRelayIndicator(4, value) }

// ===== Refresh =====

def refresh() {
    if (logEnable) log.debug "refreshing..."
    List<String> cmds = []

    if (isBSLR2()) {
        // Read switch_left config (endpoint 1)
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_SWITCH_ACTION, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_SWITCH_MODE, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_RELAY_MODE, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_RELAY_INDEX, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_BINDED_MODE, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_LONG_PRESS_DURATION, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_LEVEL_MOVE_RATE, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_MULTISTATE_INPUT, ATTR_PRESENT_VALUE, [destEndpoint: 1])

        // Read switch_right config (endpoint 2)
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_SWITCH_ACTION, [destEndpoint: 2])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_SWITCH_MODE, [destEndpoint: 2])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_RELAY_MODE, [destEndpoint: 2])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_RELAY_INDEX, [destEndpoint: 2])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_BINDED_MODE, [destEndpoint: 2])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_LONG_PRESS_DURATION, [destEndpoint: 2])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_LEVEL_MOVE_RATE, [destEndpoint: 2])
        cmds += zigbee.readAttribute(CLUSTER_MULTISTATE_INPUT, ATTR_PRESENT_VALUE, [destEndpoint: 2])

        // Read relay states (endpoints 3, 4)
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF, 0x0000, [destEndpoint: 3])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF, 0x0000, [destEndpoint: 4])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF, ATTR_RELAY_INDICATOR_MODE, [destEndpoint: 3])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF, ATTR_RELAY_INDICATOR, [destEndpoint: 3])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF, ATTR_RELAY_INDICATOR_MODE, [destEndpoint: 4])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF, ATTR_RELAY_INDICATOR, [destEndpoint: 4])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF, ATTR_START_UP_ON_OFF, [destEndpoint: 3])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF, ATTR_START_UP_ON_OFF, [destEndpoint: 4])

        // Global config on switch_left endpoint (1)
        cmds += zigbee.readAttribute(CLUSTER_BASIC, ATTR_DEVICE_CONFIG, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_BASIC, ATTR_NETWORK_INDICATOR, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_BASIC, ATTR_MULTI_PRESS_RESET, [destEndpoint: 1])
    } else {
        // BSLR1 - Read switch config (endpoint 1)
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_SWITCH_ACTION, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_SWITCH_MODE, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_RELAY_MODE, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_RELAY_INDEX, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_BINDED_MODE, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_LONG_PRESS_DURATION, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF_SWITCH_CFG, ATTR_LEVEL_MOVE_RATE, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_MULTISTATE_INPUT, ATTR_PRESENT_VALUE, [destEndpoint: 1])

        // Read relay state (endpoint 2)
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF, 0x0000, [destEndpoint: 2])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF, ATTR_RELAY_INDICATOR_MODE, [destEndpoint: 2])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF, ATTR_RELAY_INDICATOR, [destEndpoint: 2])
        cmds += zigbee.readAttribute(CLUSTER_ON_OFF, ATTR_START_UP_ON_OFF, [destEndpoint: 2])

        // Global config on switch endpoint (1)
        cmds += zigbee.readAttribute(CLUSTER_BASIC, ATTR_DEVICE_CONFIG, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_BASIC, ATTR_NETWORK_INDICATOR, [destEndpoint: 1])
        cmds += zigbee.readAttribute(CLUSTER_BASIC, ATTR_MULTI_PRESS_RESET, [destEndpoint: 1])
    }

    sendZigbeeCommands(cmds, 200)
}

// ===== Configure =====

def configure() {
    log.info "Configuring Custom Tuya BSEED Zigbee Switch..."
    createChildDevices()

    List<String> cmds = []

    if (isBSLR2()) {
        // Bind and configure reporting for switch_left (endpoint 1) press action
        cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x${intToHexStr(CLUSTER_MULTISTATE_INPUT, 2)} {${device.zigbeeId}} {}"
        cmds += zigbee.configureReporting(CLUSTER_MULTISTATE_INPUT, ATTR_PRESENT_VALUE, DATA_TYPE_UINT16, 0, 3600, 1, [destEndpoint: 1])

        // Bind and configure reporting for switch_right (endpoint 2) press action
        cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x${intToHexStr(CLUSTER_MULTISTATE_INPUT, 2)} {${device.zigbeeId}} {}"
        cmds += zigbee.configureReporting(CLUSTER_MULTISTATE_INPUT, ATTR_PRESENT_VALUE, DATA_TYPE_UINT16, 0, 3600, 1, [destEndpoint: 2])

        // Configure on/off reporting for relay_left (endpoint 3)
        cmds += "zdo bind 0x${device.deviceNetworkId} 0x03 0x01 0x${intToHexStr(CLUSTER_ON_OFF, 2)} {${device.zigbeeId}} {}"
        cmds += zigbee.configureReporting(CLUSTER_ON_OFF, 0x0000, DATA_TYPE_BOOLEAN, 0, 3600, 1, [destEndpoint: 3])

        // Configure on/off reporting for relay_right (endpoint 4)
        cmds += "zdo bind 0x${device.deviceNetworkId} 0x04 0x01 0x${intToHexStr(CLUSTER_ON_OFF, 2)} {${device.zigbeeId}} {}"
        cmds += zigbee.configureReporting(CLUSTER_ON_OFF, 0x0000, DATA_TYPE_BOOLEAN, 0, 3600, 1, [destEndpoint: 4])

        // Configure relay indicator reporting for relay_left (endpoint 3)
        cmds += zigbee.configureReporting(CLUSTER_ON_OFF, ATTR_RELAY_INDICATOR, DATA_TYPE_BOOLEAN, 0, 3600, 1, [destEndpoint: 3])
        // Configure relay indicator reporting for relay_right (endpoint 4)
        cmds += zigbee.configureReporting(CLUSTER_ON_OFF, ATTR_RELAY_INDICATOR, DATA_TYPE_BOOLEAN, 0, 3600, 1, [destEndpoint: 4])
    } else {
        // BSLR1: Bind and configure reporting for switch (endpoint 1) press action
        cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x${intToHexStr(CLUSTER_MULTISTATE_INPUT, 2)} {${device.zigbeeId}} {}"
        cmds += zigbee.configureReporting(CLUSTER_MULTISTATE_INPUT, ATTR_PRESENT_VALUE, DATA_TYPE_UINT16, 0, 3600, 1, [destEndpoint: 1])

        // Configure on/off reporting for relay (endpoint 2)
        cmds += "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x${intToHexStr(CLUSTER_ON_OFF, 2)} {${device.zigbeeId}} {}"
        cmds += zigbee.configureReporting(CLUSTER_ON_OFF, 0x0000, DATA_TYPE_BOOLEAN, 0, 3600, 1, [destEndpoint: 2])

        // Configure relay indicator reporting (endpoint 2)
        cmds += zigbee.configureReporting(CLUSTER_ON_OFF, ATTR_RELAY_INDICATOR, DATA_TYPE_BOOLEAN, 0, 3600, 1, [destEndpoint: 2])
    }

    sendZigbeeCommands(cmds, 500)
    runIn(10, "refresh")
}

// ===== Child Devices =====

def createChildDevices() {
    if (isBSLR2()) {
        createChild("relay_left", "Relay Left")
        createChild("relay_right", "Relay Right")
    } else {
        // BSLR1 single relay - no child needed, parent handles it
    }
}

private void createChild(String suffix, String label) {
    String dni = "${device.deviceNetworkId}-${suffix}"
    if (!getChildDevice(dni)) {
        log.info "Creating child device: ${label}"
        addChildDevice("hubitat", "Generic Component Switch", dni,
            [name: "${device.displayName} (${label})", isComponent: true])
    }
}

// ===== Utility Methods =====

private void sendZigbeeCmd(String cmd) {
    sendHubCommand(new HubAction(cmd, Protocol.ZIGBEE))
}

private void sendZigbeeCommands(List<String> cmds, int interCmdDelay = 0) {
    if (!cmds) return
    List<String> toSend = interCmdDelay > 0 ? delayBetween(cmds, interCmdDelay) : cmds
    sendHubCommand(new HubMultiAction(toSend, Protocol.ZIGBEE))
}

private String intToHexStr(int value, int width = 1) {
    return zigbee.convertToHexString(value, width * 2)
}

private String hexToString(String hex) {
    if (!hex) return ""
    StringBuilder sb = new StringBuilder()
    for (int i = 0; i < hex.length(); i += 2) {
        sb.append((char) Integer.parseInt(hex.substring(i, i + 2), 16))
    }
    return sb.toString()
}

private String stringToHex(String str) {
    if (!str) return ""
    return str.bytes.collect { String.format("%02X", it) }.join()
}
