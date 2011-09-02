(ns handover-client.transfer
  (:use [seesaw core make-widget mig])
  (:use [clojure.contrib.io :only [file-str]])
  (:require [handover-client.connection :as con]
            [handover-client.state :as state])
  (:import [javax.swing JFileChooser JDialog])
  (:import [org.jivesoftware.smackx.filetransfer FileTransferListener FileTransferManager FileTransferRequest FileTransfer]))

(def file-transfer-manager (atom nil))

(def transfers (ref []))

(defn file-transfer-req->panel [^FileTransferRequest req]
  (mig-panel
    :constraints ["" "[][][]"]
    :items [["Ihr Partner möchte Ihnen eine Datei übermitteln." "span 3,wrap,growx"]
            ["Dateiname:" "span 2"] [(.getFileName req) "wrap"]
            ["Beschreibung:" "span 2"][(.getDescription req) "wrap"]
            ["Dateigröße:" "span 2"] [(str (.getFileSize req)) "wrap"]]))

(extend-type FileTransferRequest
  MakeWidget
  (make-widget* [this]
    (file-transfer-req->panel this)))

(extend-type FileTransfer
  MakeWidget
  (make-widget* [this]
                  (mig-panel
                    :constraints ["" "[][][]"]
                    :items [[(.getFileName this) "span 2"][(label :text "Übertragung läuft") "wrap"]
                            [(progress-bar :id :progress-bar) "span 2,growx"]
                            [(action :name "" :handler (fn [_] (.cancel this))) "wrap"]])))

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
      (-> (dialog :resizable? false :content panel :option-type :ok-cancel :success-fn func) pack! show!))
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
