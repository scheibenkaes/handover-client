(ns handover-client.presence
  (:use [clojure.java.io :only [resource]])
  (:use [handover-client.connection :only [roster]])
  (:require [clojure.contrib.logging :as log])
  (:import [org.jivesoftware.smack RosterListener])
  (:import [org.jivesoftware.smack.packet Presence Presence$Type]))

(def available-icon (resource "icons/available.png"))

(def not-available-icon (resource "icons/not-available.png"))

(defn presence [ros user]
  (.getPresence ros user))

(defn available? 
  ([^Presence pres]
   (.isAvailable pres))

  ([con user]
   (if-let [pres (presence (roster con) user)]
     (available? pres)
     false)))

(defn watch-availability! [rost cb]
  "Adds a listener to the given roster and calls (cb new-availablity) when the state changes."
  (.addRosterListener rost
                      (proxy [RosterListener][]
                        (entriesAdded [added] (log/info (str "Added to roster " added)))
                        (entriesDeleted [deleted] (log/info (str "Deleted from roster " deleted)))
                        (entriesUpdated [updated] (log/info (str "Updated in roster " updated)))
                        (presenceChanged [presence] 
                                         (log/info (str "Presence changed " presence))
                                         (let [new-presence (if (available? presence) :available :unavailable)]
                                           (cb new-presence))))))
(defn availability->icon [con user]
  (if (available? con user)
    available-icon
    not-available-icon))

