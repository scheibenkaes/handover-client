(ns handover-client.state)

(def server-configuration (atom {:server-host "scheibenkaes.org"}))

(def me (atom nil))

(def other (atom nil))
