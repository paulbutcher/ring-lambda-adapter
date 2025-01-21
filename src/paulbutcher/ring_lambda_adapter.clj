(ns paulbutcher.ring-lambda-adapter
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- get-body
  [request]
  (when-let [body (get request "body")]
    (-> (if (get request "isBase64Encoded")
            (String. (.decode (java.util.Base64/getDecoder) body))
            body)
        (.getBytes)
        (java.io.ByteArrayInputStream.))))

(defn lambda-request->ring-request
  "Given a Lambda function URL request, returns a Ring request map.

   Function URL request format:
   https://docs.aws.amazon.com/lambda/latest/dg/urls-invocation.html#urls-request-payload

   Ring request spec:
   https://github.com/ring-clojure/ring/blob/master/SPEC.md#14-request-maps"
  [request]
  (let [headers (-> (get request "headers")
                    (update-keys str/lower-case))
        http (get-in request ["requestContext" "http"])]
    {:headers headers
     :body (get-body request)
     :protocol (get http "protocol")
     :query-string (get request "rawQueryString")
     :remote-addr (get http "sourceIp")
     :request-method (-> (get http "method")
                         str/lower-case
                         keyword)
     :scheme (-> headers
                 (get "x-forwarded-proto")
                 keyword)
     :server-name (get-in request ["requestContext" "domainName"])
     :server-port (get headers "x-forwarded-port")
     :uri (get http "path")}))

(defmulti ^:private ^String body-string class)
(defmethod body-string nil [_] nil)
(defmethod body-string String [body] body)
(defmethod body-string clojure.lang.ISeq [body] (str/join body))
(defmethod body-string java.io.File [body] (slurp body))
(defmethod body-string java.io.InputStream [body] (slurp body))

(defn ring-response->lambda-response
  "Given a Ring response map, returns a Lambda function URL response.
   
   Function URL response format:
   https://docs.aws.amazon.com/lambda/latest/dg/urls-invocation.html#urls-response-payload
   Ring response spec:
   https://github.com/ring-clojure/ring/blob/master/SPEC.md#15-response-maps"
  [response]
  {:statusCode (:status response)
   :headers (:headers response)
   :isBase64Encoded false
   :body (-> response
             :body
             body-string)})

(defn ^:export handle-request
  "Example usage:

   ```clojure
   (ns my-application.lambda
     (:gen-class :implements
                 [com.amazonaws.services.lambda.runtime.RequestStreamHandler])
     (:require [paulbutcher.ring-lambda-adapter :refer [handle-request]]))

   (defn -handleRequest [_ is os _context] (handle-request my-application/app is os))
   ```"
  [app is os]
  (with-open [r (io/reader is)
              w (io/writer os)]
    (-> (json/read r)
        lambda-request->ring-request
        app
        ring-response->lambda-response
        (json/write w))))
