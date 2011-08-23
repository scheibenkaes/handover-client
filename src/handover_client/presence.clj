(ns handover-client.presence
  (:use [clojure.java.io :only [resource]])
  (:use [handover-client.connection :only [roster]])
  (:import [org.jivesoftware.smack.packet Presence Presence$Type]))

(def available-icon (resource "icons/available.png"))

(def not-available-icon (resource "icons/not-available.png"))

(defn available? [con user]
  (if-let [presence (.getPresence (roster con) user)]
    (= Presence$Type/available presence)
    false))

(defn availability->icon [con user]
  (if (available? con user)
    available-icon
    not-available-icon))

