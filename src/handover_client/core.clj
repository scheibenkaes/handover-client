(ns handover-client.core
  (:use [seesaw core mig])
  (:use clojure.java.io)
  (:gen-class))

(def main-frame
  (frame :title "Handover" :size [800 :by 600] :on-close :exit))

(defn show-panel-in-main-frame [p] 
  (config! main-frame :content p))

(def bold-font "ARIAL-BOLD-18")

(def receive-panel 
  (mig-panel
    :constraints ["" "[80,right][160]"]
    :items [[(label :text "Bitte geben Sie die ID ein, die Ihnen ihr Partner übermittelt hat." :font bold-font) "wrap,span 2"]
            ["#" ""] [(text) "growx,wrap"]
            [(action :name "Ok") "span 2"]]))

(def welcome-panel 
  (mig-panel
    :constraints ["" "[120]25[center][center]"]
    :items [[(label :text "Was möchten Sie tun?" :font bold-font) "span 2 1,wrap"]
            [(action :handler (fn [e] (println e)) :icon (resource "icons/go-next.png")) "growx"]["Eine Datei versenden." "wrap"]
            [(action :icon (resource "icons/go-previous.png") :handler (fn [e] (show-panel-in-main-frame receive-panel))) "growx"]["Eine Datei empfangen." "span 2"]
            [:separator "wrap,growx"]]))

(defn show-main-window [] 
  (invoke-later 
    (show-panel-in-main-frame receive-panel)
    (show! main-frame)))

(defn -main [& args] 
  (show-main-window))
