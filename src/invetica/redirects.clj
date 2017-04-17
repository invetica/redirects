(ns invetica.redirects
  "Redirect some requests to their canonical counterparts.

  To redirect a number of gTLDs for example, you can take a registry (i.e.
  hash-map) like so:

      {\"example.co.uk\" {:aliases #{\"example.co.uk\"
                                    \"example.de\"
                                    \"example.fr\"
                                    \"example-files.com\"}
                          :canonical \"example.com\"
                          :https? true}}

  ...and 'compile' it into an optimised registry for use with
  `canonical-redirect`.

  The above example would redirect any HTTP(S) request to example.co.uk, to
  https://www.example.com/ preserving any path and/or query string."
  (:require [clojure.spec :as s]
            [invetica.uri :as uri]
            [ring.core.spec]
            [ring.util.response :as response]))

;; -----------------------------------------------------------------------------
;; Specs

(s/def ::aliases (s/coll-of :ring.request/server-name :into #{}))
(s/def ::canonical :ring.request/server-name)
(s/def ::https? boolean?)

(s/def ::registry
  (s/map-of :ring.request/server-name
            (s/keys :opt-un [::aliases ::canonical ::https?])))

(s/def ::compiled-registry
  (s/map-of :ring.request/server-name
            (s/keys :opt-un [::canonical ::https?])))

;; -----------------------------------------------------------------------------
;; Utils

(s/fdef get-request?
  :args (s/cat :request :ring/request)
  :ret boolean?)

(defn- get-request?
  [{method :request-method}]
  (or (= method :head) (= method :get)))

;; -----------------------------------------------------------------------------
;; Predicate

(def ^:private redirect-statuses
  (->> response/redirect-status-codes vals (into #{})))

(def redirect?
  (comp redirect-statuses :status))

;; -----------------------------------------------------------------------------
;; Redirect

(defn- canonical-uri-str
  [request {:keys [canonical https?]}]
  (let [qs (:query-string request)]
    (str "http"
         (when https? "s")
         "://"
         canonical
         (:uri request)
         (when qs (str "?" qs)))))

(s/def ::ssl-port
  ::uri/port)

(s/def ::options
  (s/keys :opt-un [::ssl-port]))

(s/fdef canonical-redirect
  :args (s/cat :registry ::compiled-registry
               :request :ring/request
               :options (s/? ::options))
  :ret (s/nilable :ring/response))

(defn canonical-redirect
  "Given a registry (i.e. hash-map) of hosts, returns a Ring-compatible redirect
  if there's a canonical URL that should be used instead."
  ([registry request] (canonical-redirect registry request {}))
  ([registry request options]
   (let [{:keys [scheme server-name]} request]
     (when-let [{:keys [::canonical ::https?]
                 :or {https? true} :as site} (get registry server-name)]
       (when-not (= canonical server-name)
         (response/redirect
          (canonical-uri-str request site)
          (if (get-request? request) 302 307)))))))

;; -----------------------------------------------------------------------------
;; Config

(s/fdef make-registry
  :args (s/cat :registry ::registry)
  :ret ::compiled-registry)

(defn make-registry
  [registry]
  (into {}
        (mapcat (fn [[k m]]
                  (let [m* (dissoc m :aliases)]
                    (into [[k m*]]
                          (map #(vector %1 m*))
                          (:aliases m)))))
        registry))
