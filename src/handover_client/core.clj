(ns handover-client.core
  (:use [seesaw core mig])
  (:use clojure.java.io)
  (:use [handover-client.clipboard :only [get-str put-str]])
  (:require [clojure.contrib.logging :as logging])
  (:require [handover-client.connection :as con]
            [handover-client.presence :as presence]
            [handover-client.state :as state]
            [handover-client.chat :as chat])
  (:gen-class))

(defn display-error [msg exc]
  (logging/debug msg exc)
  (alert (str msg " " (.getMessage exc))))

(def main-frame
  (frame :title "Handover" :size [800 :by 600] :on-close :exit :resizable? false))

(defn show-panel-in-main-frame [p] 
  (-> main-frame 
    (config! :content p)
    (pack!)))

(def bold-font "ARIAL-BOLD-18")

(declare welcome-panel send-panel receive-panel transfer-panel)

(defn display-send-invitation-view [con-data]
  (config! (select send-panel [:#other-id]) :text (-> con-data :other :id))
  (config! (select send-panel [:#proceed]) :enabled? true :listen [:mouse-clicked println])
  (show-panel-in-main-frame send-panel))

(defn create-accounts []
  (try
    (let [connection-data (con/create-tmp-connection! (:server-host @state/server-configuration))]
      (display-send-invitation-view connection-data))
    (catch Exception e
      (display-error "Beim Verbindungsaufbau mit dem Server ist ein Fehler aufgetreten:" e))
    (finally
      (config! (select welcome-panel [:*]) :enabled? true)
      (config! (select welcome-panel [:#spinner]) :visible? false))))

(defn get-id-from-clipboard []
  (when-let [cp-content (get-str)]
    (when (.startsWith cp-content "temp-") cp-content)))

(defn user-wants-to-receive []
  (config! (select receive-panel [:#rec-other]) :text (get-id-from-clipboard))
  (show-panel-in-main-frame receive-panel))

(defn user-wants-to-send [] 
  (invoke-now
    (config! (select welcome-panel [:*]) :enabled? false)
    (config! (select welcome-panel [:#spinner]) :visible? true :enabled? true)
    (.start (Thread. create-accounts))))

(defn user-wants-to-transfer [me other server]
  (try
    (let [con (con/connect-and-login server (-> me :id (con/with-host-name server)) (:password me))]
      (logging/debug (str "User wants to transfer: " me other server))
      (chat/init! con (:id other))
      (show-panel-in-main-frame transfer-panel))
    (catch Exception e (display-error "Fehler beim Verbinden: " e))))

(def exit-action
  (action :icon (resource "icons/system-log-out.png") :handler (fn [_] (System/exit 0))))

(def zip-action
  (action :icon (resource "icons/package.png") :tip "Übertragen Sie mehrere Dateien, in dem Sie sie in ein Archiv verpacken."))

(def send-action
  (action :icon (resource "icons/go-next.png") :tip "Eine einzelne Datei übermitteln."))

(defn send-chat-message []
  (-> (select transfer-panel [:#msg-field]) text chat/send-message))

(def transfer-panel
  (mig-panel 
    :constraints ["insets 0 0 0 0" "[][][][][]"]
    :items [
            [(toolbar :items [send-action zip-action :separator exit-action]) "span 3"][(label :icon presence/available-icon) "wrap,align center"]
            [(mig-panel :constraints ["insets 5 5 5 5" "[350][][]" "[][]"] 
                        :items [[(progress-bar :value 75) "span 2,growx"][(button :icon (resource "icons/process-stop.png")) "wrap,span 1 2,growx,growy"]
                                ["File: foo" "span 3,growx"]]) "span 3 10,growx,growy"]
            [(mig-panel :constraints ["insets 0 5 5 5" "[150][][]" "[400][][]"] 
                        :items [[(editor-pane :text "" :editable? false) "span 3,growx,wrap,growy"][(text :text "" :id :msg-field) "span 2,growx"]
                                [(action :name "Senden" :handler (fn [_] 
                                                                   (send-chat-message)
                                                                   (text! (select transfer-panel [:#msg-field]) ""))) ""]]) "span 1 3"]
            ]))

(def receive-panel 
  (mig-panel
    :constraints ["" "[][][][]"]
    :items [[(label :text "Bitte geben Sie die ID ein, die Ihnen ihr Partner übermittelt hat." :font bold-font) "wrap,span 2"]
            [(text :id :rec-other) "span 4,growx,wrap"]
            [(action :name "Zurück" :handler (fn [_] (show-panel-in-main-frame welcome-panel))) ""]
            [:separator "span 2"]
            [(action :name "Ok" 
                     :handler (fn [_] 
                                (let [me (-> (select receive-panel [:#rec-other]) text)]
                                  (user-wants-to-transfer 
                                    {:id me :password me}
                                    {:id (str me "-1")}
                                    (:server-host @state/server-configuration))))) ""]]))

(def send-panel
  (mig-panel
    :constraints ["" "[center][300][center]" "[][]25[][]"]
    :items [[(label :text "Teilen Sie Ihrem Partner diese ID mit." :font bold-font) "span 3,wrap,align left"]
            ["#" ""][(text :text "TODO" :editable? false :id :other-id) "growx"]
            [(action :icon (resource "icons/edit-paste.png") :tip "Die ID in die Zwischenablage kopieren." 
                     :handler (fn [_] (let [id (-> (select send-panel [:#other-id]) text)](put-str id)))) "wrap,growx"]
            [(action :name "Zurück" :handler (fn [_] (show-panel-in-main-frame welcome-panel))) ""]
            [:separator ""]
            [(action :name "Weiter" :handler (fn [_] (let [other-id (-> (select send-panel [:#other-id]) text)]
                                                       (user-wants-to-transfer 
                                                         {:id (str other-id "-1") :password (str other-id "-1")}
                                                         {:id other-id}
                                                         (:server-host @state/server-configuration))))) "growx,wrap"]]))

(def not-implemented-handler
  (fn [& _] (alert "Diese Funktionalität steht leider noch nicht zur Verfügung.")))

(def welcome-panel 
  (mig-panel
    :constraints ["" "[120]25[][]" "[][][]15[][]"]
    :items [[(label :text "Was möchten Sie tun?" :font bold-font) "span 2 1"][(action :icon (resource "icons/applications-system.png") :tip "Passen Sie die Einstellungen des Programms an." :handler not-implemented-handler) "wrap"]
            [(action :handler (fn [_] (user-wants-to-send)) :icon (resource "icons/go-next.png") :tip "Laden Sie eine Person ein, um mit ihr Dateien auszutauschen.") "growx"]["<html>Eine Datei versenden.<br/><small>Verschicken Sie dazu eine Einladung an den gewünschten Partner</small></html>" ""][(label :icon (resource "icons/spinner.gif") :visible? false :id :spinner) "wrap,align center"]
            [(action :icon (resource "icons/go-previous.png") :handler (fn [e] (user-wants-to-receive))
                     :tip "Klicken Sie hier, wenn Sie eine Einladung zu Austauschen von Daten erhalten haben.") "growx"]
            ["<html>Eine Datei empfangen.<br/><small>Akzeptieren Sie eine Einladung zum Datenaustausch.</small></html>" "span 2"]
            [:separator "wrap,growx"]
            [exit-action "growx"]["Das Programm beenden" "span 2"]]))

(defn center! [f]
  (doto f (.setLocationRelativeTo nil)))

(defn show-main-window [] 
  (invoke-later 
    (show-panel-in-main-frame welcome-panel)
    (-> main-frame center! pack! show!)))

(defn -main [& args]
  (native!)
  (show-main-window))

