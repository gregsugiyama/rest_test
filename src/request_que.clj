(ns request-que
  (:require
   [taoensso.timbre        :as timbre]
   [clj-http.client        :as http]
   [clojure.core.async     :as a]
   [clojure.edn            :as edn]
   [clojure.data.json      :as json]
   [camel-snake-kebab.core :as csk]))

(timbre/set-level! :info)

;; ** Helper Functions

(defn remove-vals
  "Remove keys whose value matches a given `pred`icate."
  [pred m]
  (reduce (fn [m' [k v]]
            (if (pred v)
              m'
              (assoc m' k v)))
          {}
          m))

(defn empty-or-nil?
  "True for values that are an empty collection, the empty string,
  the empty sequence, or `nil`. False for all other values."
  [v]
  (or (and (seqable? v)
           (empty? v))
      (nil? v)))

(def remove-empty-or-nil-values
  (partial remove-vals empty-or-nil?))

(defn parse-amount
  "Converts to a Double rounded to the nearest 100th"
  [n]
  (java.lang.Double/parseDouble (format "%.2f" n)))

(defn make-url
  "Returns a url for 'n'.json"
  [n]
  (str "https://resttest.bench.co/transactions/" n ".json"))

(defn make-request
  "Creates a http 'get' request for the provided 'n' page of transaction records.
   Throws an exception for any non 200 OK status."
  [n]
  (let [url (make-url n)]
    (timbre/info "Sending Request To: " url)
    (try
      (http/get url)
      (catch Exception e (timbre/debug "Bad Request" e)))))

(defn get-tally
  "Makes a 'get' request to the Bench API for a specific page of tallies. 
   When 200 OK, returns the response body in the form of a clojure map."
  [page-number]
  (let [initial-request (make-request page-number)]
    (when (= 200 (:status initial-request))
      (let [response-body (json/read-str (:body initial-request)
                                         :key-fn csk/->kebab-case-keyword)]
        response-body))))

(defn make-tally
  "Sorts tansactions by date & sums the transaction costs. 
   Returns a map of {date transaction-total}"
  [state]
  (let [transactions (->>
                      (remove-empty-or-nil-values state) ;; Remove empty or nil values
                      (map (fn [[_ v]]
                             (:transactions v)))         ;; Grab the transactions
                      (flatten)                          ;; Create a flat collection
                      (group-by #(:date %))) ;; Group transactions by date
        tally        (into {} (map (fn [[k v]]
                                     (let [sum (reduce (fn [acc m]
                                                         (let [amount (edn/read-string (:amount m))]
                                                           (+ acc amount))) 0 v)]
                                       {k (parse-amount sum)})) transactions))] ;; return a map of {date tally-sum}
    (timbre/info "Totals: " tally)
    tally))

;; -------------------------------------------------------------------------------------- ;;

;; ** In-memory data store

(defonce *state (atom {}))

;; -------------------------------------------------------------------------------------- ;;

;; ** Async Request Worker 

(defn request-worker
  "Creates an async job que for processing http-requests to the Bench API.
   Spins up a non-blocking process (channel) to run concurrently on a seperate thread. Once a http-response
   has been received, it is pulled off the request-channel & stored in memory. The channel
   will close after all requests have been processed."
  []

;; Currently, I'm incrementing the page request after a succesfull response. 
;; Once the que receives a 404, it will stop sending requests & tally the stored responses.

;; One potential drawback is in the url generation logic. What if pages 3 & 6 exist, but not 5? Unlikley, but ultimatly possible. 
;; That could result in missing data.

  (let [req-chan (a/chan)]
    (a/go (while true
            (let [response (a/<! req-chan)]
              (swap! *state assoc (:page response) response))))
    (loop [page 1]                     ;; start at page 1
      (let [request (get-tally page)]  ;; make a request
        (if request
          (do (a/>!! req-chan request) ;; on success, recur & increment the page number for the next request. Puts the pending request on the request channel
              (recur (inc page)))
          (do (make-tally @*state) ;; on failure, tally the current in-memory store of tally responses & close the request que.
              (a/close! req-chan)))))))

(request-worker)