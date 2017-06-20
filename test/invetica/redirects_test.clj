(ns invetica.redirects-test
  (:require [clojure.test :refer :all]
            [invetica.redirects :as sut]
            [invetica.test.spec :as test.spec]
            [ring.mock.request :as mock]
            [ring.util.response :as response]))

;; -----------------------------------------------------------------------------
;; Specs

(use-fixtures :once test.spec/instrument)

(deftest t-specs
  (test.spec/is-well-specified 'invetica.redirects))

;; -----------------------------------------------------------------------------
;; Utils

(defn- location
  [response]
  (response/get-header response "location"))

(defn- maybe-redirect
  [sites request]
  (sut/canonical-redirect (sut/compile-sites sites) request))

;; -----------------------------------------------------------------------------
;; Sites

(def ^:private sites
  #{{:site/aliases #{"invetica.co.uk"
                     "invetica.pro"
                     "invetika.com"}
     :site/canonical "www.invetica.co.uk"
     :site/https? true}
    {:site/aliases #{"example.co.uk"
                     "example.com"}
     :site/canonical "www.example.com"}})

(deftest t-deprecated-config
  (is (= (sut/compile-sites sites)
         (sut/make-registry
          {"invetica.co.uk" {:aliases #{"invetica.pro"
                                        "invetika.com"}
                             :canonical "www.invetica.co.uk"
                             :https? true}
           "example.co.uk" {:aliases #{"example.com"}
                            :canonical "www.example.com"
                            :https? false}}))))

(deftest t-compile-sites
  (let [registry (sut/compile-sites sites)]
    (is (= ["example.co.uk"
            "example.com"
            "invetica.co.uk"
            "invetica.pro"
            "invetika.com"
            "www.example.com"
            "www.invetica.co.uk"]
           (sort (keys registry))))))

;; -----------------------------------------------------------------------------
;; Redirects

(deftest t-redirect-not-required
  (let [response (maybe-redirect
                  #{{:site/canonical "www.example.com"}}
                  (mock/request :get "http://www.example.com/"))]
    (is (nil? response))))

(deftest t-redirect-not-required-unknown-site
  (let [response (maybe-redirect
                  #{{:site/canonical "www.example.com"}}
                  (mock/request :get "http://some.example.com/"))]
    (is (nil? response))))

(deftest t-redirect-get-requests
  (let [response (maybe-redirect
                  #{{:site/aliases #{"example.com"}
                     :site/canonical "www.example.com"}}
                  (-> (mock/request :get "http://example.com/")
                      (mock/query-string {"a" "b" "c" "d"})))]
    (is (= 302 (:status response)))
    (is (= "http://www.example.com/?a=b&c=d" (location response)))))

(deftest t-redirect-post-requests
  (let [response (maybe-redirect
                  #{{:site/aliases #{"example.com"}
                     :site/canonical "www.example.com"}}
                  (-> (mock/request :post "http://example.com/")
                      (mock/query-string {"a" "b" "c" "d"})))]
    (is (= 307 (:status response)))
    (is (= "http://www.example.com/?a=b&c=d" (location response)))))

(deftest t-redirect-https
  (let [registry (sut/compile-sites #{{:site/aliases #{"example.com"}
                                       :site/https? true
                                       :site/canonical "www.example.com"}})]
    (testing "alias host"
      (let [response (sut/canonical-redirect
                      registry
                      (-> (mock/request :get "http://example.com/")
                          (mock/query-string {"a" "b" "c" "d"})))]
        (is (= 302 (:status response)))
        (is (= "https://www.example.com/?a=b&c=d" (location response)))))
    (testing "canonical host"
      (let [response (sut/canonical-redirect
                      registry
                      (-> (mock/request :get "http://www.example.com/")
                          (mock/query-string {"a" "b" "c" "d"})))]
        (is (= 302 (:status response)))
        (is (= "https://www.example.com/?a=b&c=d" (location response)))))))
