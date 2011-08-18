(ns handover-client.core
  (:use [seesaw core mig])
  (:use clojure.java.io)
  (:require [handover-client.connection :as con])
  (:gen-class))


(def main-frame
  (frame :title "Handover" :size [800 :by 600] :on-close :exit))

(defn show-panel-in-main-frame [p] 
  (-> main-frame 
    (config! :content p)
    (pack!)))

(def bold-font "ARIAL-BOLD-18")

(declare welcome-panel)

(def receive-panel 
  (mig-panel
    :constraints ["" "[][][][]"]
    :items [[(label :text "Bitte geben Sie die ID ein, die Ihnen ihr Partner übermittelt hat." :font bold-font) "wrap,span 2"]
            [(text) "span 4,growx,wrap"]
            [(action :name "Zurück" :handler (fn [_] (show-panel-in-main-frame welcome-panel))) ""]
            [:separator "span 2"]
            [(action :name "Ok") ""]]))

(def send-panel 
  (mig-panel
    :constraints ["" "[center][300][center]"]
    :items [[(label :text "Teilen Sie Ihrem Partner diese ID mit." :font bold-font) "span 3,wrap,align left"]
            ["#" ""][(text :text (:generated-id connection-data) :editable? false) "growx"][(action :icon (resource "icons/edit-paste.png") :tip "Die ID in die Zwischenablage kopieren.") "wrap,growx"]
            [(action :name "Zurück" :handler (fn [_] (show-panel-in-main-frame welcome-panel))) ""]
            [:separator ""]
            [(action :name "Weiter") "growx,wrap"] ]))

(def welcome-panel 
  (mig-panel
    :constraints ["" "[120]25[][]" "[][][]15[]"]
    :items [[(label :text "Was möchten Sie tun?" :font bold-font) "span 2 1,wrap"]
            [(action :handler (fn [e] (show-panel-in-main-frame send-panel)) :icon (resource "icons/go-next.png")) "growx"]["Eine Datei versenden." "wrap"]
            [(action :icon (resource "icons/go-previous.png") :handler (fn [e] (show-panel-in-main-frame receive-panel))) "growx"]["Eine Datei empfangen." "span 2"]
            [:separator "wrap,growx"]
            [(action :icon (resource "icons/system-log-out.png") :handler (fn [_] (System/exit 0))) "growx"]["Das Programm beenden" "span 2"]]))

(defn show-main-window [] 
  (invoke-later 
    (show-panel-in-main-frame welcome-panel)
    (-> main-frame pack! show!)))

(defn -main [& args] 
  (native!)
  (show-main-window))
