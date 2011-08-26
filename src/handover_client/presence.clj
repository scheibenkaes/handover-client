(ns handover-client.presence
  (:use [clojure.java.io :only [resource]])
  (:use [handover-client.connection :only [roster]])
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

(defn availability->icon [con user]
  (if (available? con user)
    available-icon
    not-available-icon))

