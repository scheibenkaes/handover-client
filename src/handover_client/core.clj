(ns handover-client.core
  (:use [seesaw core mig])
  (:use clojure.java.io)
  (:gen-class))

(def welcome-panel 
  (mig-panel
    :constraints ["" "[120]25[center][center]"]
    :items [[(label :text "Was möchten Sie tun?" :font "ARIAL-BOLD-18") "span 2 1,wrap"]
            [(action :handler (fn [e] (println e)) :icon (resource "icons/go-next.png")) "growx"]["Eine Datei versenden." "wrap"]
            [(action :icon (resource "icons/go-previous.png")) "growx"]["Eine Datei empfangen." "span 2"]
            [:separator "wrap,growx"]]))

(def main-frame 
  (frame :title "Handover" :size [800 :by 600] :on-close :exit :content welcome-panel))

(defn show-main-window [] 
  (invoke-later (show! main-frame)))

(defn -main [& args] 
  (show-main-window))
