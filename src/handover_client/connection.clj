(ns handover-client.connection
  (:use [clojure.contrib.properties :only [get-system-property]])
  (:use digest))

(defn create-id [in] 
  (->> in (digest "SHA1") (take 8) (apply str)))

(defn generate-new-id [] 
  (->> (System/nanoTime) (str (get-system-property "user.home") (rand-int 100)) create-id))
