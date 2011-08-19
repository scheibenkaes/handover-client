(ns handover-client.core
  (:use [seesaw core mig])
  (:use clojure.java.io)
  (:require [clojure.contrib.logging :as logging])
  (:require [handover-client.connection :as con])
  (:gen-class))

(defn display-error [msg exc]
  (logging/debug msg exc)
  (alert (str msg " " (.getMessage exc))))

(def
  ^{:doc "All required user information about me and my companion."}
  users-information
  (ref
    nil
    :error-handler (fn [_ e] (display-error "Ein Fehler ist aufgetreten" e))))

(def other-client (atom nil))

(defn users-information-update-handler [_ _ _ n]
  (when-let [id (-> n :other :id)]
    (reset! other-client id)))

(def
  ^{:doc "Information regarding the targeted server. :server-host - host to connect to"} 
  server-configuration
  (agent 
    {:server-host (con/localhost)} 
    :error-handler (fn [_ e] (display-error "Ein Fehler ist aufgetreten: " e))))

(def main-frame
  (frame :title "Handover" :size [800 :by 600] :on-close :exit :resizable? false))

(defn show-panel-in-main-frame [p] 
  (-> main-frame 
    (config! :content p)
    (pack!)))

(def bold-font "ARIAL-BOLD-18")

(declare welcome-panel send-panel receive-panel)

(defn display-send-invitation-view [con-data]
  (config! (select send-panel [:#other-id]) :text (-> con-data :other :id))
  (config! (select send-panel [:#proceed]) :enabled? true :listen [:mouse-clicked println])
  (show-panel-in-main-frame send-panel))

(defn create-accounts []
  (try
    (let [connection-data (con/create-tmp-connection! (:server-host @server-configuration))]
      (display-send-invitation-view connection-data))
    (catch Exception e
      (display-error "Beim Verbindungsaufbau mit dem Server ist ein Fehler aufgetreten:" e))
    (finally
      (config! (select welcome-panel [:*]) :enabled? true)
      (config! (select welcome-panel [:#spinner]) :visible? false))))

(defn user-wants-to-send [] 
  (invoke-now
    (config! (select welcome-panel [:*]) :enabled? false)
    (config! (select welcome-panel [:#spinner]) :visible? true :enabled? true)
    (.start (Thread. create-accounts))))

(def exit-action
  (action :icon (resource "icons/system-log-out.png")))

(def zip-action
  (action :icon (resource "icons/package.png")))

(def send-action
  (action :icon (resource "icons/go-next.png")))

(def transfer-panel
  (mig-panel 
    :constraints ["insets 0 0 0 0" "[][][][][]"]
    :items [
            [(toolbar :items [send-action zip-action :separator exit-action]) "span 3"]["Verbunden" "wrap,align right"]
            [(mig-panel :constraints ["insets 5 5 5 5" "[350][][]" "[][]"] 
                        :items [[(progress-bar :value 75) "span 2,growx"][(button :icon (resource "icons/process-stop.png")) "wrap,span 1 2,growx,growy"]
                                ["File: foo" "span 3,growx"]]) "span 3 10,growx,growy"]
            [(mig-panel :constraints ["insets 0 5 5 5" "[150][][]" "[400][][]"] 
                        :items [[(editor-pane :text "" :editable? false) "span 3,growx,wrap,growy"][(text :text "") "span 2,growx"] [(button :text "Senden") ""]]) "span 1 3"]
            ]))

(def receive-panel 
  (mig-panel
    :constraints ["" "[][][][]"]
    :items [[(label :text "Bitte geben Sie die ID ein, die Ihnen ihr Partner übermittelt hat." :font bold-font) "wrap,span 2"]
            [(text) "span 4,growx,wrap"]
            [(action :name "Zurück" :handler (fn [_] (show-panel-in-main-frame welcome-panel))) ""]
            [:separator "span 2"]
            [(action :name "Ok" :enabled? false) ""]]))

(def send-panel
  (mig-panel
    :constraints ["" "[center][300][center]" "[][]25[][]"]
    :items [[(label :text "Teilen Sie Ihrem Partner diese ID mit." :font bold-font) "span 3,wrap,align left"]
            ["#" ""][(text :text "TODO" :editable? false :id :other-id) "growx"][(action :icon (resource "icons/edit-paste.png") :tip "Die ID in die Zwischenablage kopieren.") "wrap,growx"]
            [(action :name "Zurück" :handler (fn [_] (show-panel-in-main-frame welcome-panel))) ""]
            [:separator ""]
            [(button :text "Weiter" :enabled? false :id :proceed) "growx,wrap"] ]))

(def welcome-panel 
  (mig-panel
    :constraints ["" "[120]25[][]" "[][][]15[][]"]
    :items [[(label :text "Was möchten Sie tun?" :font bold-font) "span 2 1"][(action :icon (resource "icons/applications-system.png") :tip "Passen Sie die Einstellungen des Programms an." :handler (fn [& _] (alert "Diese Funktionalität steht noch nicht zur Verfügung."))) "wrap"]
            [(action :handler (fn [_] (user-wants-to-send)) :icon (resource "icons/go-next.png") :tip "Laden Sie eine Person ein, um mit ihr Dateien auszutauschen.") "growx"]["<html>Eine Datei versenden.<br/><small>Verschicken Sie dazu eine Einladung an den gewünschten Partner</small></html>" ""][(label :icon (resource "icons/spinner.gif") :visible? false :id :spinner) "wrap"]
            [(action :icon (resource "icons/go-previous.png") :handler (fn [e] (show-panel-in-main-frame receive-panel))
                     :tip "Klicken Sie hier, wenn Sie eine Einladung zu Austauschen von Daten erhalten haben.") "growx"]
            ["<html>Eine Datei empfangen.<br/><small>Akzeptieren Sie eine Einladung zum Datenaustausch.</small></html>" "span 2"]
            [:separator "wrap,growx"]
            [(action :icon (resource "icons/system-log-out.png") :handler (fn [_] (System/exit 0))) "growx"]["Das Programm beenden" "span 2"]]))

(defn center! [f]
  (doto f (.setLocationRelativeTo nil)))

(defn show-main-window [] 
  (invoke-later 
    (show-panel-in-main-frame transfer-panel)
    (-> main-frame center! pack! show!)))

(defn -main [& args]
  (native!)
  (add-watch users-information ::user-id users-information-update-handler)
  (show-main-window))


