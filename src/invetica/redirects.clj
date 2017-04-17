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

(s/fdef canonical-uri-str
  :args (s/cat :request :ring/request :site ::site :options ::options)
  :ret ::uri/absolute-uri-str)

(defn- canonical-uri-str
  [request {:keys [site/canonical site/https?]} options]
  (let [ssl-port (:ssl-port options)
        qs (:query-string request)]
    (str "http"
         (when https? "s")
         "://"
         canonical
         (when (and https? ssl-port) (str ":" ssl-port))
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
     (when-let [{:keys [:site/canonical :site/https?]
                 :or {https? true} :as site} (get registry server-name)]
       (when-not (= canonical server-name)
         (response/redirect
          (canonical-uri-str request site options)
          (if (get-request? request) 302 307)))))))

;; -----------------------------------------------------------------------------
;; Sites

(s/def :site/aliases (s/coll-of :ring.request/server-name :into #{}))
(s/def :site/canonical :ring.request/server-name)
(s/def :site/https? boolean?)

(s/def ::site
  (s/keys :req [:site/canonical]
          :opt [:site/aliases
                :site/https?]))

(s/def ::compiled-sites
  (s/map-of :ring.request/server-name
            (s/keys :req [:site/canonical]
                    :opt [:site/https?])))

(s/fdef compile-sites
  :args (s/cat :sites (s/coll-of ::site :into #{}))
  :ret ::compiled-sites)

(defn compile-sites
  [sites]
  (into {}
        (mapcat (fn [site]
                  (let [m* (dissoc site :site/aliases)]
                    (into []
                          (map #(vector %1 m*))
                          (:site/aliases site)))))
        sites))

;; -----------------------------------------------------------------------------
;; Deprecated registry
;;
;; Use `compile-sites` instead.

(s/def ::registry
  (s/map-of :ring.request/server-name
            (s/keys :opt-un [::aliases ::canonical ::https?])))

(s/def ::compiled-registry
  (s/map-of :ring.request/server-name
            (s/keys :opt-un [::canonical ::https?])))

(s/fdef make-registry
  :args (s/cat :registry ::registry)
  :ret ::compiled-sites)

(defn ^:deprecated make-registry
  "See `compile-sites`."
  [registry]
  (letfn [(->site [[k v]]
            {:site/aliases (into #{k} (:aliases v))
             :site/canonical (:canonical v k)
             :site/https? (:https? v true)})]
    (compile-sites (into #{} (map ->site) registry))))
