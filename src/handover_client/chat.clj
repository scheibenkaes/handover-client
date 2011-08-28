(ns handover-client.chat)

(def chat-manager (atom nil))

(def chat (atom nil))

(def messages (ref []))

(defrecord Message [from at text orig])

(defn append-message [message]
  (dosync
    (alter messages conj message)))

(def message-listener
  (proxy [org.jivesoftware.smack.MessageListener][]
    (processMessage 
      [_ msg]
      (append-message (Message. :other (System/currentTimeMillis) (.getBody msg) msg)))))

(defn send-message [^String msg]
  (dosync
    (.sendMessage @chat msg)
    (alter messages conj (Message. :me (System/currentTimeMillis) msg nil))))

(defn init! [con other-id]
  "Initialize the chatting system.
  con is the XMPPConnection to the server.
  other-id must be the fully qualified name of the other client connected to"
  (reset! chat-manager (.getChatManager con))
  (reset! chat (.createChat @chat-manager other-id message-listener)))
