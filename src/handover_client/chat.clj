(ns handover-client.chat)

(def chat-manager (atom nil))

(def chat (atom nil))

(def messages (ref []))

(defrecord Message [time text])

(def message-listener
  (proxy [org.jivesoftware.smack.MessageListener][]
    (processMessage 
      [this _ msg]
      (dosync
        (alter messages conj (Message. (System/currentTimeMillis) (.getBody msg)))))))

(defn send-message [^String msg]
  (.sendMessage @chat msg))

(defn init! [con other-id]
  "Initialize the chatting system.
  con is the XMPPConnection to the server.
  other-id must be the fully qualified name of the other client connected to"
  (reset! chat-manager (.getChatManager con))
  (reset! chat (.createChat @chat-manager other-id message-listener)))
