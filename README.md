# paulbutcher/ring-lambda-adapter

[![Clojars Project](https://img.shields.io/clojars/v/com.paulbutcher/ring-lambda-adapter.svg)](https://clojars.org/com.paulbutcher/ring-lambda-adapter)

Clojure Ring adapter for AWS Lambda.

## Usage

See [Quick and Easy Clojure on AWS Lambda in 2025](https://paulbutcher.com/lambda1.html).

Example usage:

```clojure
(ns my-application.lambda
  (:gen-class :implements
              [com.amazonaws.services.lambda.runtime.RequestStreamHandler])
  (:require [paulbutcher.ring-lambda-adapter :refer [handle-request]]))
(defn -handleRequest [_ is os _context] (handle-request my-application/app is os))
```

## License

Copyright © 2025 Paulbutcher

Distributed under the Eclipse Public License version 1.0.
