(defproject handover-client "1.0.0-SNAPSHOT"
  :description "Handing over files. DAU approved."
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [digest "1.2.1"]
                 [seesaw "1.0.10"]]
  :dev-dependencies [[lein-vim "1.0.2-SNAPSHOT"]
                     [org.clojars.autre/lein-vimclojure "1.0.0"]]
  :main handover-client.core)
