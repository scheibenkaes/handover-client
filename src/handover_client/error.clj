(ns handover-client.error
  (:require [clojure.contrib.logging :as logging])
  (:use [seesaw core]))

(defn display-error [msg exc]
  (logging/debug msg exc)
  (alert (str msg " " (.getMessage exc))))
