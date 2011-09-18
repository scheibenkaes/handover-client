(ns handover-client.transfer
  (:use [seesaw core make-widget mig])
  (:require [handover-client.settings :as settings])
  (:use [clojure.contrib.io :only [file-str as-file]]
        [clojure.java.io :only (resource)])
  (:require [handover-client.connection :as con]
            [handover-client.state :as state])
  (:import [javax.swing JFileChooser JDialog])
  (:import [java.text NumberFormat])
  (:import [java.awt Desktop])
  (:import [org.jivesoftware.smackx.filetransfer
            FileTransferListener
            FileTransferManager
            FileTransferRequest
            FileTransfer
            FileTransfer$Status]))

(def file-transfer-manager (atom nil))

(def transfers (ref []))

(def number-format (NumberFormat/getInstance))

(def desktop (Desktop/getDesktop))

(def open-file #(.open desktop (as-file %)))

(defn done? [^FileTransfer t]
  (.isDone t))

(defn file-size->str [size]
  (let [mb (/ size 1024 1024)
        fmt (.format number-format mb)]
    (str fmt " MB")))

(defn file-transfer-req->panel [^FileTransferRequest req]
  (mig-panel
    :constraints ["" "[][][]"]
    :items [["<html><strong>Ihr Partner möchte Ihnen eine Datei übermitteln:</strong></html>" "span 3,wrap,growx"]
            ["Dateiname:" "span 2"] [(.getFileName req) "wrap"]
            ["Dateigröße:" "span 2"] [(file-size->str (.getFileSize req)) "wrap"]
            [:separator "wrap"]
            [(format "Nach Abschluss der Übertragung finden Sie die Datei unter %s" @settings/download-folder) "span 3,grow,wrap"]]))

(extend-type FileTransferRequest
  MakeWidget
  (make-widget* [this]
    (file-transfer-req->panel this)))

(defn ask-for-cancellation-of-transfer [^FileTransfer tr]
  (-> (dialog :content "Wollen Sie den Transfer wirklich abbrechen?" :option-type :ok-cancel :success-fn (fn [& _] (.cancel tr))) pack! show!))

(def stop-icon
  (resource "icons/process-stop.png"))

(def open-icon
  (resource "icons/document-open.png"))

(def open-download-folder-action
  (action :tip "Öffnen Sie die das Verzeichnis, in dem alle heruntergeladenen Dateien gespeichert werden" :icon open-icon 
          :handler (fn [_] (open-file @settings/download-folder))))

(extend-type FileTransfer
  MakeWidget
  (make-widget* [this]
                  (mig-panel
                    :constraints ["insets 0 0 0 0" "[grow][shrink]"]
                    :items [[(.getFileName this) ""]
                            [(label :text "Übertragung läuft" :id :status-label) "wrap"]
                            [(progress-bar :id :progress-bar :paint-string? true) "growx"]
                            [(button :action (action :tip "Übertragung abbrechen" :icon stop-icon :handler (fn [_] (ask-for-cancellation-of-transfer this)))) "growx"]])))

(defn- status->text [^FileTransfer$Status status]
  (condp = status
    FileTransfer$Status/cancelled "Abgebrochen"
    FileTransfer$Status/refused "Abgelehnt"
    FileTransfer$Status/complete "Abgeschlossen"
    FileTransfer$Status/error "Fehler"
    "Übertragung läuft"))

(defn update-transfer-widgets [r]
  (doseq [t @r]
    (let [{:keys [widget ^FileTransfer transfer]} t]
      (text! (select widget [:#status-label]) (status->text (.getStatus transfer)))
      (config! (select widget [:#progress-bar]) :value (* 100.0 (.getProgress transfer)))
      (when (done? transfer)
        (do
          (config! (select widget [:*]) :enabled? false)))))
  (Thread/sleep 1000)
  (recur r))

(defn request-transfer [file user description]
  (dosync
    (let [out (.createOutgoingFileTransfer @file-transfer-manager (str user "/handover"))]
      (.sendFile out file description)
      (alter transfers conj out))))

(defn save-incoming-transfer [^FileTransferRequest req]
  (dosync
    (let [transfer (.accept req)
          file-name (.getFileName req)
          file (file-str @settings/download-folder "/" file-name)]
      (.recieveFile transfer file)
      (alter transfers conj transfer))))

(defn incoming-file-transfer-request [^FileTransferRequest req]
  (try
    (let [panel (make-widget* req)
          func (fn [& _] (save-incoming-transfer req))]
      (-> (dialog :resizable? false :content panel :option-type :ok-cancel :success-fn func :cancel-fn (fn [& _] (.reject req))) pack! show!))
    (catch Exception e (println e))))

(defn complete-name []
  (-> @state/other :id (con/with-host-name (:server-host @state/server-configuration))))

(defn choose-transfer [parent]
  (let [chooser (JFileChooser.)
        ret (.showOpenDialog chooser parent)]
    (when (= JFileChooser/APPROVE_OPTION ret)
      (request-transfer (.getSelectedFile chooser) (complete-name) "TODO"))))

(defn transfer-file [^java.io.File file]
  (request-transfer file (complete-name) "TODO"))

(defn init! [con]
  (let [manager  (FileTransferManager. con)]
    (.addFileTransferListener 
      manager 
      (proxy [FileTransferListener][]
        (fileTransferRequest [request]
                             (incoming-file-transfer-request request))))
    (reset! file-transfer-manager manager)))
