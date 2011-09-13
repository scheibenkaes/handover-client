(ns handover-client.resources
  (:use [clojure.java.io :only (resource)]))

(defn icon-by-name [icon-name]
   (let [ending (if (.endsWith icon-name ".png") "" ".png")
         file (str "icons/" icon-name ending)]
     (resource file)))
