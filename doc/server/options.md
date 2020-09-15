# Server options

Each interpreter accepts an implicit options value, which contains configuration values for:

* how to create a file (when receiving a response that is mapped to a file, or when reading a file-mapped multipart 
  part)
* how to handle decode failures (see also [error handling](errors.md))
* debug-logging of request handling

To customise the server options, define an implicit value, which will be visible when converting an endpoint or multiple
endpoints to a route/routes. For example, for `AkkaHttpServerOptions`:

```scala mdoc:compile-only
import sttp.tapir.server.akkahttp.AkkaHttpServerOptions

implicit val customServerOptions: AkkaHttpServerOptions = 
  AkkaHttpServerOptions.default.copy(decodeFailureHandler = ???)
```
