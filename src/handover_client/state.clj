(ns handover-client.state
  (:require [handover-client.connection :as con]))

(def server-configuration (atom {:server-host (con/localhost)}))

(def me (atom nil))

(def other (atom nil))
