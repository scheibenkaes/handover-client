(ns handover-client.state)

(def server-configuration (atom {:server-host "xmpp"}))

(def me (atom nil))

(def other (atom nil))
