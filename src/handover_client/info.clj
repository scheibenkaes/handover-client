(ns handover-client.info
  (:use handover-client.resources)
  (:use [seesaw core mig]))

(def info-icon (icon-by-name "help-browser"))

(def desktop (java.awt.Desktop/getDesktop))

(defn open-url [u]
  (-> desktop (.browse (java.net.URI. u))))

(defn url-button [text url]
  (action :name text :handler (fn [_] (open-url url))))

(defn action-button [text act]
  (action :name text :handler (fn [_] (act))))

(def uri (java.net.URI. "mailto:scheibenkaes+handover@googlemail.com"))

(def info-panel
  (mig-panel :constraints ["" "[][][]"]
       :items [["<html><strong>Handover</strong></html>" "span 3,growx,wrap"]
               ["Copyright 2011 Benjamin Klüglein" "span 3,growx,wrap"]
               [:separator "wrap"]
               [(url-button "Handover im Internet" "http://scheibenkaes.org/software") "growx,wrap"]
               [(url-button "Lizenz (GPLv3)" "http://www.gnu.de/documents/gpl.de.html") "growx,wrap"]
               [(action-button "Einen Fehler melden" #(.mail desktop uri)) "growx,wrap"]]))

(defn show-info-dialog []
  (-> (dialog :type :info :content info-panel :title "Informationen zu Handover" :resizable? false) pack! show!))

(def show-info-action
  (action :icon info-icon :handler (fn [_] (show-info-dialog))))
