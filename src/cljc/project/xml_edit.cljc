(ns project.xml-edit
  (:require [clojure.string :as str]))

(def slurped-xml (atom ""))

(def patterns [{:pattern #"ФИО:[а-яА-Я[\d][\h]]*"
                :new-val "ФИО:"}
               {:pattern #"Образование:[а-яА-Я[\d][\h]]*"
                :new-val "Образование:"}
               {:pattern #"Номер телефона:[а-яА-Я[\+][\d][\h]]*"
                :new-val "Номер телефона:"}
               {:pattern #"Возраст:[а-яА-Я[\d][\h]]*"
                :new-val "Возраст"}])

(defn edit-data [data {:keys [pattern new-val]}]
  (str/replace data pattern new-val))

(defn apply-patterns [patterns data]
  (reduce (partial edit-data) data patterns))

