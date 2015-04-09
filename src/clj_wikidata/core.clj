;; Anwendung zum Zugriff auf Wikidata
;;
;;Author Arnold Andiek Email:arnold.andiek@mni.thm.de
;;Author Dieudonne Timma Meyatchie Email:dieudonne.timma.meyatchie@mni.thm.de
;;
(ns clj-wikidata.core
   (:require [clj-http.client :as client]
             [clojure.data.json :as json]))

(import '(javax.swing JFrame  JPanel JButton JTextField JLabel 
                       JScrollPane JSeparator JTable 
                      JTextArea JList ListSelectionModel DefaultListModel ))
(import 'javax.swing.JOptionPane)
(import '(javax.swing.event ListSelectionEvent ListSelectionListener))
(import '(java.awt GridBagLayout Dimension Font))
(import 'java.awt.event.ActionListener)
(import 'java.awt.GridBagConstraints)
(import '(java.awt GridBagLayout Insets))


;
; Die Funktion pare-input ersetzt die leer-zeichen durch plus-zeichen.
(defn parse-input
  "Replaces all instance of blank with plus sign in input."
  [input]
  (clojure.string/replace input #" " "+"))

;Die Funktion search-entites sendet ein Get-Request zur der Wiki API Methode wbserachentities dabei wird die Eingabe als Parameter mitgegeben. 
;Der Zurückgabe Wert dieser Funktion ist der Body-Teil des Response als JSON Objekt.
(defn search-entities
  "Gets the data for multiple Wikibase entities"
  [search-string]
  (let [entities (client/get (str "https://www.wikidata.org/w/api.php?action=wbsearchentities&search=" (parse-input search-string) "&format=json&language=en") {:as :json})
        get-body (get entities :body)
        entities-json (json/read-str get-body)]
    entities-json))

;Die Funktion get-info-search-result holt die Informationen zu Bezeichung, Id und Beschreibung aus Entitäten heraus.
(defn get-info-search-result
  "Return the value mapped to key"
  [result-vector key]
  (get result-vector key))

;Die Funktion do-print-result gibt die Bezeichnungen, Id und Beschreibungen der möglichen Entitäten aus.
(defn do-print-result
  "Print the info 'label( id ): description' of result to the output stream"
  [result]
  (println (get-info-search-result result "label") "("(get-info-search-result result "id")") :"(get-info-search-result result "description")))
     
;Die Funktion get-entity sucht eine Entität nach deren ID mit Hilfe der WIKI API Methode webgetentities.
(defn get-entity
  "Get entity as json object from search-id (entity-Id or property-Id)"
  [search-id]
  (let [entity (client/get (str "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=" search-id "&format=json&language=en"))
        body (get entity :body)
        entity-json (json/read-str body)
        get-entity (get-in entity-json ["entities" (str search-id) "claims"])]
    get-entity ))

;Die Funktion get-value-by-property liefert den Wert zur Eigenschaft.
(defn get-value-by-property
  "Get the value by the property"
  [property-id, entity-claims]
  (let [property (get-in entity-claims [property-id])
        value (get-in (first property) ["mainsnak" "datavalue" "value"])]
    value))

;Die Funktion get-type-by-property liefert den Typ der Eigenschaft zurück.
(defn get-type-by-property
 "Get the type of the property received by the id in entity-claims"
  [property-id, entity-claims]
  (let [property (get-in entity-claims [property-id])
        value (get-in (first property) ["mainsnak" "datavalue" "type"])]
    value))

;Die Funktion get-property-value liefert den Wert der Eigenschaft. Also die Bezeichnung der Eigenschaft in der Standard Sprache Englisch.
(defn get-property-value
  "get the value of property to property-id"
  [property-id]
  (let [entity (client/get (str "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=" property-id "&format=json&language=en"))
        body (get entity :body)
        entity-json (json/read-str body)
        get-property-value-by-aliases (get-in entity-json ["entities" (str property-id) "aliases" "en" 1 "value"])
        get-property-value-by-labels (get-in entity-json ["entities" (str property-id) "labels" "en" "value"])
        value? (if (nil? get-property-value-by-aliases) true false)]
    (if value? get-property-value-by-labels get-property-value-by-aliases )
    ))

;Die Funktion get-time Transformiert ein WIKIDATA Datum in ein Einfaches Datumsformat.
(defn get-time
  "Transform a wikidata date in a Simpledateformat"
  [time]
  (let [df (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss")
        date (.format (java.text.SimpleDateFormat. "MM/dd/yyyy") (.parse df (subs (get time "time") 8)))]
    date))

;Die Funktion detail-view zeigt die Entitäten der Eingabe. Sowohl die Bezeichnung der Eigenschaften als auch deren Werte
(defn do-detail-view
  "Show all values of the property of the input"
  [input]
  (let [entity (get-entity input)]
    (doseq [[k] entity 
        :let [value (get-value-by-property k entity)
              type (get-type-by-property k entity)
              propertyValue (get-property-value k)]] 
  (case type
    "globe-coordinate" (println (format "%-6s %-5.5s %-25.25s :: %s" k type propertyValue value))
    "quantity" (println (format "%-6s %-5.5s %-25.25s :: %s" k type propertyValue (get value "amount")))
    "string" (println (format "%-6s %-5.5s %-25.25s :: %s" k type propertyValue value))
    "time" (println (format "%-6s %-5.5s %-25.25s :: %s" k type propertyValue (get-time value)))
    "wikibase-entityid" (println (format "%-6s %-5.5s %-25.25s :: (%s) %s" k type propertyValue (str "Q" (get value "numeric-id")) (get-property-value (str "Q" (get value "numeric-id")))))
    "monolingualtext" (println (format "%-6s %-5.5s %-25.25s :: %s" k type propertyValue (get value "text")))
    "default"))))

;Die Funktion list-or-detail-view entscheidet, 
;welche folge Funktion aufgerufen wird. 
;Entweder die Funktion detail-view oder die Funktion do-print-result. 
;Über die Funktion search-entities wird nach der Eingabe gesucht. 
;Gibt es zur Eingabe nur einen Eintrag wird die Funktion detail-view mit dem Eintrag aufgerufen,
;sonst werden alle Einträge ausgegeben.
(defn list-or-detail-view
  "Shows a list of entities or a entity in detail"
  [input]
  (let [entities (search-entities input)
        search-vector (get entities "search")
        search-size (get entities "search-continue" 0)]
   ( if (== search-size 0) 
     (do-detail-view(get (first search-vector) "id")) 
     (doseq [item search-vector] (do-print-result item)))
    ))

;Die Funktion validate-input öffnet ein Dialog Fenster worüber man eine Eingabe tätigen kann. 
;Diese Funktion liefert nil zurück, wenn die Eingabe leer ist.
(defn validate-input
  "return nil else input"
  []
  (let [input-show-feld (JOptionPane/showInputDialog
                          nil 
                          "Please write the word or ID to search:" "WikiData"
                          JOptionPane/INFORMATION_MESSAGE)
        input-nil? (compare input-show-feld "")  
        ]
    (if (== input-nil? 0) nil input-show-feld)))

;---------------MAIN-ALPHA------------------------------------------------------------
;Die main Funktion als Startpunkt der Anwendung. 
;Diese Funktion besteht aus einer while-Schleife deren Bedingung solange 1(true) ist bis die Eingabe nil(leer) ist. 
;Die Eingabe wird über die Funktion input-swing durchgeführt. 
;Ist die Eingabe nicht nil wird diese der Funktion list-or-details-view übergeben.

(defn main
  "main function"
  []
  (let [a (atom 1)]
  (while (pos? @a) (let [input (validate-input)
                       ]
                   (if (nil? input) (swap! a dec) (list-or-detail-view input))))))


;-------------SWING-BETA------------------------------------------------------
;
;

;Die Funktion list-view gibt das Suchergebnis in einem Array zurück
(defn list-view
  "Return a Array of Searchresult as String 'label( id ): description' of result to the output stream"
  [entities]
  (let [ 
        search-vector (get entities "search") 
        temp-data (for [item search-vector] (list (str (get-info-search-result item "label") 
                                                 "( "(get-info-search-result item "id")") : "
                                                 (get-info-search-result item "description")
                                                   )))
        ]
            (to-array (map #(clojure.string/join " " %)temp-data))
            ))
;Die Funktion list-view-id gibt das Suchergebnis in einem Array zurück
(defn list-view-id
  "Return a Array of Searchresult as String ID of result to the output stream"
  [entities]
  (let [ 
        search-vector (get entities "search") 
        temp-data (for [item search-vector] (list (get-info-search-result item "id")
                                                 ))
        ]
    (to-array (map #(clojure.string/join " " %)temp-data))
    )
  )


;Die Funktion detail-view-data-string gibt die Entität der Eingabe als String zurück.
;Sowohl die Bezeichnung der Eigenschaften als auch deren Werte.
(defn detail-view-data-string
 
  [input]
  (let [entity (get-entity input)
        dataList (for [[k] entity 
        :let [value (get-value-by-property k entity)
              type (get-type-by-property k entity)
              property-value (get-property-value k)]](list 
  (case type
    "globe-coordinate" (format "%-25.25s :: %s" property-value value)
    "quantity" (format "%-25.25s :: %s" property-value (get value "amount"))
    "string" (format "%-25.25s :: %s" property-value value)
    "time" (format "%-25.25s :: %s" property-value (get-time value))
    "wikibase-entityid" (format "%-25.25s :: %s" property-value (get-property-value (str "Q" (get value "numeric-id"))))
    "monolingualtext" (format "%-25.25s :: MONO" property-value)
    "default" ))
  
  )]
    (clojure.string/join "\n" (map #(clojure.string/join %)dataList))
    ))

;Speichern eine Instanz von der Klasse GridBagConstraints
;;quelle: http://stuartsierra.com/2010/01/05/taming-the-gridbaglayout
(def c (GridBagConstraints.))
(set! (. c gridx) 1)
(set! (. c gridy) GridBagConstraints/RELATIVE)

;;Der Macro set-grid positionniert ein Element im einem Gridlayout
;;quelle: http://stuartsierra.com/2010/01/05/taming-the-gridbaglayout

(defmacro set-grid! [constraints field value]
  `(set! (. ~constraints ~(symbol (name field)))
         ~(if (keyword? value)
            `(. java.awt.GridBagConstraints
                ~(symbol (name value)))
            value)))

;;Der Macro Grid-bag-layout bekommt ein Panel und mehreren Elementen und positionniert sie im Gridlayout
;;params container Jpanel
;;params body ein oder mehrere GUI-Elemente
;;quelle: http://stuartsierra.com/2010/01/05/taming-the-gridbaglayout

(defmacro grid-bag-layout [container & body]
  (let [c (gensym "c")
        cntr (gensym "cntr")]
    `(let [~c (new java.awt.GridBagConstraints)
           ~cntr ~container]
       ~@(loop [result '() body body]
           (if (empty? body)
             (reverse result)
             (let [expr (first body)]
               (if (keyword? expr)
                 (recur (cons `(set-grid! ~c ~expr
                                          ~(second body))
                              result)
                        (next (next body)))
                 (recur (cons `(.add ~cntr ~expr ~c)
                              result)
                        (next body)))))))))

;;Der Macro on-action implementiert den Interface Actionlistener,bekommt ein Event und eine oder mehreren Funktion 
;;Params event ActionListener
;;params body eine oder mehrere Funktion
(defmacro on-action [component event & body]
  `(. ~component addActionListener
      (proxy [java.awt.event.ActionListener] []
        (actionPerformed [~event] ~body))))

;;Der Macro on-action implementiert den Interface ListSelectionListener,bekommt ein Event und eine oder mehreren Funktion  
;;Params event ListSelectionListener
;;params body eine oder mehrere Funktion
(defmacro on-action-list [component event & body]
  `(. ~component addListSelectionListener
      (proxy [ListSelectionListener] []
        (valueChanged [~event] ~body ))))

;;Die Funktion search-list-event ist die aktion ,wenn man auf einem Element der liste klickt
;;sie aktualisiert die GUI-Oberfläche
;;params entities eine Liste von Identifier mit Label und Beschreibung
;;params list-feld Jlist
;;params item-feld JTextArea
;;params event ListSelectionListener
;;params call-back-event eine funktion
;;
(defn search-list-event [entities list-feld item-feld event call-back-event]
  (let[
       
       id-list (list-view-id entities)
        source-list(.getSource event)
       item-index(first (.getSelectedIndices source-list))
       selected-item(get id-list item-index)
    
    ]
   (doto item-feld 
         (.setVisible false)
         (.setText (detail-view-data-string selected-item))
         (.setVisible true))
    ))

;;Die Funktion search-button-event ist die aktion ,wenn man auf  dem Knopf druckt
;;sie aktualisiert die GUI-Oberfläche
;;params textfeld JtextField
;;params list-feld Jlist
;;params item-feld JTextArea
;;params isFromMain Boolean
;;
(defn search-button-event [textfeld list-feld item-feld isfromMain]
  (let[
      to-search-text ( if(= isfromMain true)(str (.getText textfeld))textfeld)
      entities (search-entities to-search-text)
        search-vector (get entities "search")
        search-size (count  search-vector)
       list-data-temps (list-view entities)
       list_show (doto (JList. list-data-temps)
                      (.setSelectionMode ListSelectionModel/SINGLE_INTERVAL_SELECTION);
                    (.setVisibleRowCount -1)
                      (.setLayoutOrientation JList/VERTICAL)
                     (on-action-list evnt  search-list-event entities list-feld item-feld evnt search-button-event )
                      )
   ]
    ( if (== search-size 1)  
      (doto item-feld 
        (.setVisible false)
        (.setText (detail-view-data-string to-search-text))
        (.setVisible true))
       
      (doto list-feld 
        (.setVisible false)
         (.setViewportView list_show)
        (.setVisible true)
     )
   )))


;;Die Funktion panel_app initialisiert die gesamte GUI-Oberfläche
;;return ein GUI-Fenster
(defn panel_app[]
  (let
    [
     search_feld (doto (JTextField. "search feld" 15))
     list_data_cell (to-array [""])
     item_text_feld (doto (JTextArea. )
                           (.setFont (Font. "Arial Black" Font/BOLD 12) )
                           (.setLineWrap true)
                           (.setWrapStyleWord true)
                           (.setEditable false)
                           (.setColumns 30))
     list_show (doto (JList. list_data_cell)
                      (.setSelectionMode ListSelectionModel/SINGLE_INTERVAL_SELECTION);
                      (.setVisibleRowCount -1)
                      (.setLayoutOrientation JList/VERTICAL)
                 )
     item_scroll_panel(doto (JScrollPane. item_text_feld)
                        (.setPreferredSize (Dimension. 800 200))
                        )
     list_panel(doto (JScrollPane. list_show)
                 (.setPreferredSize (Dimension. 150 100))
                 )
     search_button (doto(JButton. "search" ) 
                     (.setSize (Dimension. 2 2))
                     (on-action evnt search-button-event search_feld  list_panel  item_text_feld true ))
     search_panel (doto (JPanel.) (.add search_feld)(.add search_button))
     mainPanel (doto (JPanel. (GridBagLayout.))
       (grid-bag-layout
        :fill :BOTH, :insets (Insets. 5 5 5 5)
        :gridx 0, :gridy 0
         search_panel
         :gridx 0,:gridy 1
         list_panel
        :gridx 0, :gridy 2, :gridheight 2
        item_scroll_panel
        )
       )      
     ]
     (doto (JFrame. "Wiki Data")
      (.setContentPane mainPanel)
       (.setSize 900 600)
      (.pack)
      (.setVisible true))
    ))
;; View Speichert ,welche Bedienungsoberfläche ausgewählt wurde
(def view (JOptionPane/showInputDialog nil (str "Please choose the Output view")
                                       (str "Wikidata") JOptionPane/QUESTION_MESSAGE nil
                                        (to-array(vector "Console" "GUI")) (str "Console")))
;;Startet das Programm
(if(==(compare view "Console") 0)(main) (panel_app))

