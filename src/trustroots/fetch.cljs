(ns trustroots.fetch
  (:require
    [trustroots.helpers :refer [log info debug]]
  ))


(def baseurl "https://www.trustroots.org/api")
(def url_map
  {
   :signin "auth/signin"
   })

(defn get-url [endpoint]
  (str baseurl (endpoint url_map)))


(defn- handle-fetch-promise
  [promise & {:keys [on-success on-error]}]
  (-> promise
      ; network error
      (.catch (fn [err] (on-error {:data err :type :network-error})))

      ; fecth resolves also 400 and 500 errors
      (.then (fn [res]
               (if (not (nil? res))
                 (let [ok (.-ok res)
                       status (.-status res)
                       handle-data (fn [data]
                                     (if ok
                                       (on-success {:data data :status status :ok ok})
                                       (on-error   {:data data :status status :type :http-error})))
                       ]
                   (-> (.json res)
                       (.then handle-data))))))))

(defn fetch-json [& {:keys [url endpoint headers method body on-success on-error fetch-fn]
                     :or   [fetch-fn js/fetch]
                     } ]
  "Helper for JSON rest api request"
  (let [req-url        (if (nil? endpoint)
                         url
                         (get-url endpoint))
        default-header {"Content-Type" "text/json"}
        optional-body  (if body
                         {"body" (js/JSON.stringify (clj->js body)) }
                         {})

        args (merge {
                     "method"   method
                     "credentials" "include"
                     "headers" (merge default-header headers)}
              optional-body )
        query (fetch-fn req-url (clj->js args))
        ]
    (handle-fetch-promise query :on-error on-error :on-success on-success)))
 
