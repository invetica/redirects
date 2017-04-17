(ns invetica.test.util
  (:require [clojure.pprint :refer [pprint]]))

(defn pprint-str
  [x]
  (with-out-str (pprint x)))
