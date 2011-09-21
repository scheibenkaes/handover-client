(ns leiningen.jnlp  "Generate the projects jnlp file."
  (:use [leiningen.jar :only [get-default-uberjar-name]]
        [leiningen.uberjar :only [uberjar]]))

(def template
  "<?xml version='1.0' encoding='UTF-8'?>
<jnlp codebase='%s' 
        href='handover.jnlp'>
    <information>
        <title>Handover</title>
        <vendor>Benjamin Klüglein</vendor>
    </information>
    <resources>
        <jar href='handover-webstart.jar'/>
    </resources>
    <security>
      <all-permissions />
    </security>
    <application-desc main-class='%s'/>
</jnlp>")


(defn fill-template [codebase main-class]
  (format template codebase main-class))

(defn jnlp [project]
  (let [{:keys [main url uberjar-name jnlp-file]} project]
    (spit jnlp-file (fill-template url (-> main str (.replace "-" "_"))))
    (println "Created " jnlp-file)))
