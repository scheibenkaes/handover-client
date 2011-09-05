(ns handover-client.transfer
  (:use [seesaw core make-widget mig])
  (:use [clojure.contrib.io :only [file-str]])
  (:require [handover-client.connection :as con]
            [handover-client.state :as state])
  (:import [javax.swing JFileChooser JDialog])
  (:import [org.jivesoftware.smackx.filetransfer
            FileTransferListener
            FileTransferManager
            FileTransferRequest
            FileTransfer
            FileTransfer$Status]))

(def file-transfer-manager (atom nil))

(def transfers (ref []))

(defn file-size->str [size]
  (str (-> (/ 1024 size) float str) " MB"))

(defn file-transfer-req->panel [^FileTransferRequest req]
  (mig-panel
    :constraints ["" "[][][]"]
    :items [["<html><strong>Ihr Partner möchte Ihnen eine Datei übermitteln:</strong></html>" "span 3,wrap,growx"]
            ["Dateiname:" "span 2"] [(.getFileName req) "wrap"]
            ["Dateigröße:" "span 2"] [(file-size->str (.getFileSize req)) "wrap"]]))

(extend-type FileTransferRequest
  MakeWidget
  (make-widget* [this]
    (file-transfer-req->panel this)))

(defn ask-for-cancellation-of-transfer [^FileTransfer tr]
  (-> (dialog :content "Wollen Sie den Transfer wirklich abbrechen?" :option-type :ok-cancel :success-fn (fn [& _] (.cancel tr))) pack! show!))

(extend-type FileTransfer
  MakeWidget
  (make-widget* [this]
                  (mig-panel
                    :constraints ["insets 0 0 0 0" "[60%][40%]"]
                    :items [[(.getFileName this) ""]
                            [(label :text "Übertragung läuft" :id :status-label) "wrap"]
                            [(progress-bar :id :progress-bar :paint-string? true) "growx"]
                            [(action :name "Stoppen" :handler (fn [_] (ask-for-cancellation-of-transfer this))) "wrap"]])))

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
      (when (.isDone transfer)
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
          file (file-str "~" "/Downloads" "/" file-name)]
      (.recieveFile transfer file)
      (alter transfers conj transfer))))

(defn incoming-file-transfer-request [^FileTransferRequest req]
  (try
    (let [panel (make-widget* req)
          func (fn [& _] (save-incoming-transfer req))]
      (-> (dialog :resizable? false :content panel :option-type :ok-cancel :success-fn func :cancel-fn (fn [& _] (.reject req))) pack! show!))
    (catch Exception e (println e))))

(defn choose-transfer [parent]
  (let [chooser (JFileChooser.)
        ret (.showOpenDialog chooser parent)]
    (when (= JFileChooser/APPROVE_OPTION ret)
      (request-transfer (.getSelectedFile chooser) (-> @state/other :id (con/with-host-name (:server-host @state/server-configuration))) "TODO"))))

(defn init! [con]
  (let [manager  (FileTransferManager. con)]
    (.addFileTransferListener 
      manager 
      (proxy [FileTransferListener][]
        (fileTransferRequest [request]
                             (incoming-file-transfer-request request))))
    (reset! file-transfer-manager manager)))
