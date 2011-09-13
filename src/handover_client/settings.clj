(ns handover-client.settings
  (:use [clojure.contrib.io :only [file-str]]
        [clojure.java.io :only [as-file]])
  (:use [seesaw core]))

(def download-folder
  (atom (file-str "~/Downloads")))

(defn create-needed-folders! []
  (.mkdir (as-file @download-folder)))

