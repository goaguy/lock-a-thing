definition(
    name: "Lock a Thing",
    namespace: "goaguy",
    author: "Goa Guy",
    description: "Locks a thing when a thing happens and not when another thing happens",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    section("Use this Door:") {
        input "thecontact", "capability.contactSensor", required: true, title: "Door?"
    }
    section("Lock this lock:") {
        input "thelock", "capability.lock", required: true, title: "Lock?"
    }
    section("After:") {
        input "thetimeout", 'number', required: true, title: "Seconds?"
    }
}

def initialize() {
    log.debug "initialize"
    if (!isLocked()) {
      unlockedHandler()
    }
    subscribe(thelock, "lock.unlocked", unlockedHandler)
}

def lockTimeoutHandler(evt) {
    log.debug "lockTimeoutHandler"
    
    def isClosed = thecontact.currentState("contact").value == "closed"
    def isUnlocked = !isLocked()
    if (isClosed && isUnlocked) {
        log.debug "lockTimeoutHandler: locking"
        doLock()
    } else {
        log.debug "lockTimeoutHandler: not lockead setting runIn"
        runIn(thetimeout, lockTimeoutHandler)
    }    
}

def forceLockHandler() {
    log.debug "forceLockHandler"
    if (!isLocked()) {
        log.debug "forceLockHandler: sendPush"
        sendPush("Locked due to timeout!")
        doLock()
    }
    runIn(1800, forceLock)
}

def checkActuallyLockedHandler() {
    log.debug "checkActuallyLockedHandler"
    if (!isLocked()) {
        log.debug "checkActuallyLockedHandler: sendPush"
        sendPush("Touble locking! Check door!")
    }
}

def unlockedHandler(evt) {
    log.debug "unlockedHandler: timeout - " + thetimeout
    unschedule(forceLockHandler)
    unschedule(lockTimeoutHandler)
    log.debug "unlockedHandler scheduling"
    runIn(1800, forceLockHandler) // every 30 min
    runIn(thetimeout, lockTimeoutHandler)
}

def doLock() {
    log.debug "doLock: timeout - " + thetimeout
    // Have to send it twice I guess...
    thelock.lock()
    thelock.lock()
    runIn(10, checkActuallyLockedHandler)
}

def isLocked() {
    def locked = thelock.lockState.value == "locked"
    log.debug "isLocked: " + thelock.lockState.value
    return locked
}

def installed() {
  initialize();
}

def updated() {
  unschedule();
    unsubscribe();
    initialize();
}

def uninstalled() {
  unschedule();
    unsubscribe();
}
