(ns handover-client.presence
  (:use [handover-client.connection :only [roster]])
  (:import [org.jivesoftware.smack.packet Presence Presence$Type]))

(defn available? [con user]
  (if-let [presence (.getPresence (roster con) user)]
    (= Presence$Type/available presence)
    false))
