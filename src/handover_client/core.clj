(ns handover-client.core
  (:use [seesaw core mig make-widget])
  (:use clojure.java.io)
  (:use clojure.contrib.command-line)
  (:use [handover-client.clipboard :only [get-str put-str]])
  (:require [clojure.contrib.logging :as logging])
  (:use [clojure.string :only [blank?]]
        [clojure.set :only [difference]])
  (:require [handover-client.connection :as con]
            [handover-client.presence :as presence]
            [handover-client.state :as state]
            [handover-client.transfer :as transfer]
            [handover-client.error :as error]
            [handover-client.chat :as chat])
  (:gen-class))

(defn center! [f]
  (doto f (.setLocationRelativeTo nil)))

(def main-frame
  (frame :title "" :size [800 :by 600] :on-close :exit :resizable? false))

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
      (error/display-error "Beim Verbindungsaufbau mit dem Server ist ein Fehler aufgetreten:" e))
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

(def presence-indicators
  {:available ["Ihr Partner ist verfügbar." (:available presence/presence-icons)]
   :unavailable ["<html>Ihr Partner ist <strong>nicht</strong> verfügbar.</html>" (:unavailable presence/presence-icons)]})

(defn update-presence-indicator [pres]
  (let [[lbl icon] (pres presence-indicators)
        ui-elem (select transfer-panel [:#presence-label])]
    (config! ui-elem :icon icon)
    (text! ui-elem lbl)))

(declare send-action send-action-dialogs)

(defn partner-went-online []
  (config! send-action :handler (:available send-action-dialogs))
  (update-presence-indicator :available)
  (let [ui (select transfer-panel [:#chat-panel])]
    (config! (select ui [:*]) :enabled? true)))

(defn partner-went-offline []
  (config! send-action :handler (:unavailable send-action-dialogs))
  (update-presence-indicator :unavailable)
  (let [ui (select transfer-panel [:#chat-panel])]
    (config! (select ui [:*]) :enabled? false)))

(defn on-partner-presence-changed [pres]
  (if (= :available pres)
    (partner-went-online)
    (partner-went-offline)))

(defn check-presence [connection id]
  (let [pres (presence/available? connection id)]
    (if pres
      (partner-went-online)
      (partner-went-offline))))

(defn message-markup [{:keys [from text]}]
  (if (= from :me)
    (str "Sie> " text)
    (str "Ihr Partner> " text)))

(defn messages->markup [messages]
  (let [lines (map message-markup messages)]
    (apply str (interpose \newline lines))))

(defn on-chat-message-appended [_ _ _ messages]
  (let [ui (select transfer-panel [:#text-chat])
        markup (messages->markup messages)]
    (text! ui markup)))

(def transfer-widgets (ref []))

(defn on-transfers-changed [_ _ old-state new-state]
  (let [diff (difference (set new-state) (set old-state))
        k-vs (for [n diff] {:widget (make-widget* n) :transfer n})
        panel (select transfer-panel [:#transfer-panel])]
    (dosync
      (apply alter transfer-widgets conj k-vs))
    (config! panel :items (map :widget @transfer-widgets))))

(defn start-transfer-ui-updater! []
  (let [t (Thread. #(transfer/update-transfer-widgets transfer-widgets))]
    (.start t)))

(defn periodically-check-for-partners-presence! [con partner]
  (let [f (fn []  (check-presence con partner)(Thread/sleep 1500)(recur))]
    (.start (Thread. f))))

(defn cancel-and-exit [& _]
  (doseq [t @transfer/transfers :when (not (transfer/done? t))]
    (.cancel t))
  (System/exit 0))

(defn ask-before-shutdown []
  (if-not (every? transfer/done? @transfer/transfers)
    (-> (dialog :content "Sie sind im Begriff das Programm zu beenden. Laufende Übertragungen werden in diesem Fall abgebrochen. Möchten Sie wirklich beenden?" :option-type :yes-no :success-fn cancel-and-exit) pack! show! center!)
    (System/exit 0)))

(def window-listener
  (proxy [java.awt.event.WindowListener][]
    (windowClosing [e] (ask-before-shutdown))
    (windowActivated [_])
    (windowClosed [_])
    (windowDeactivated [_])
    (windowDeiconified [_])
    (windowIconified [_])
    (windowOpened [_])))

(defn user-wants-to-transfer [me other server]
  (try
    (let [c (con/connect-and-login server (-> me :id (con/with-host-name server)) (:password me))
          other-with-host (-> other :id (con/with-host-name server))]
      (transfer/init! c)
      (reset! state/me c)
      (reset! state/other other)
      (presence/watch-availability! (con/roster c) on-partner-presence-changed)
      (chat/init! c other-with-host)
      (add-watch chat/messages ::main-window on-chat-message-appended)
      (add-watch transfer/transfers ::transfers on-transfers-changed)
      (start-transfer-ui-updater!)
      (periodically-check-for-partners-presence! c other-with-host)
      (show-panel-in-main-frame transfer-panel))
      (config! main-frame :size [800 :by 600])
      (config! main-frame :on-close :nothing)
      (.addWindowListener main-frame window-listener)
      (center! main-frame)
    (catch Exception e (error/display-error "Fehler beim Verbinden: " e))))

(def exit-action
  (action :icon (resource "icons/system-log-out.png") :handler (fn [_] (ask-before-shutdown))))

(def zip-action
  (action :enabled? false :icon (resource "icons/package.png") :tip "Übertragen Sie mehrere Dateien, in dem Sie sie in ein Archiv verpacken."))

(def send-action-dialogs
   {:available (fn [_] (transfer/choose-transfer main-frame))
    :unavailable (fn [_] (alert "Ihr Partner ist derzeit leider nicht online, Sie können ihm daher keine Dateien schicken."))})

(def send-action
  (action :icon (resource "icons/go-next.png") :tip "Eine einzelne Datei übermitteln."
          :handler (:unavailable send-action-dialogs)))

(defn user-wants-to-send-chat-message []
  (when-let [txt (-> (select transfer-panel [:#msg-field]) text)]
    (when-not (blank? txt)
      (try
        (chat/send-message txt)
        (text! (select transfer-panel [:#msg-field]) "")
        (catch Exception e (error/display-error "Fehler beim Senden der Nachricht." e))))))

(def transfer-panel
  (mig-panel 
    :constraints ["insets 0 0 0 0" "[75%][25%]" "[][grow]"]
    :items [
            [(toolbar :items [send-action zip-action :separator exit-action]) ""][(label :id :presence-label) "wrap,align center"]
            [(scrollable (vertical-panel :items [] :id :transfer-panel) :hscroll :never) "span 1 10,growx,growy"]
            [(mig-panel :constraints ["insets 0 0 0 0" "[150][][]" "[grow][shrink]"] 
                        :items [[(scrollable (editor-pane :text "" :editable? false :id :text-chat)) "span 3,grow,wrap"]
                                [(text :text "" :id :msg-field) "span 2,growx"]
                                [(action :name "Senden" :handler (fn [_] 
                                                                   (user-wants-to-send-chat-message))) ""]]
                        :id :chat-panel) "grow"]
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
            [(action :icon (resource "icons/edit-copy.png") :tip "Die ID in die Zwischenablage kopieren." 
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

(defn show-main-window [] 
  (invoke-later 
    (show-panel-in-main-frame welcome-panel)
    (-> main-frame center! pack! show!)))

(defn -main [& args]
  (with-command-line
    args
    "Benutzung: handover [-d]"
    [[debug? d? "Debug modus" false]
     remaining]
    (when debug? 
      (reset! state/server-configuration {:server-host "xmpp" }))
    (config! main-frame :title (str "Handover" " - " (:server-host @state/server-configuration)))
    (native!)
    (show-main-window)))

