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
        input "thecontact", "capability.contactSensors", required: true, title: "Door?"
    }
    section("Lock this lock:") {
        input "thelock", "capability.lock", required: true, title: "Lock?"
    }
    section("After:") {
        input "thetimeout", 'number', required: true, title: "Seconds?"
    }
}

def initialize() {
    subscribe(thelock, "lock.unlock", unlockedHandler)
}

def lockTimeoutHandler() {
    didLock = ensureLockedIfClosed()
    if (!didLock) {
        runIn(thetimeout, lockTimeoutHandler) 
    }
}

def forceLockHandler() {
    if (!isLocked()) {
        sendPush("Locked due to timeout!")
        doLock()
    }
    runIn(1800, forceLock)
}

def checkActuallyLockedHandler() {
    if (!islocked()) {
        sendPush("Touble locking! Check door!")
    }
}

def unlockedHandler() {
    unschedule()
    runIn(1800, forceLockHandler) // every 30 min
    runIn(thetimeout, lockTimeoutHandler)
}

def doLock() {
    thelock.lock()
    runIn(thetimeout, checkActuallyLockedHandler)
}

def isLocked() {
    return thelock.currentState("lock") == "locked"
}

def ensureLockedIfClosed() {
    isUnlocked = !isLocked()
    isClosed = thecontact.currentState("contact") == "closed"

    if (isClosed && isUnlocked) {
        doLock()
        return true
    } 
    return false
}


def installed() {
}

def updated() {
    unsubscribe();
    initialize();
}

def uninstalled() {
    unsubscribe();
}

