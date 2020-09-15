# Forms

## URL-encoded forms

An URL-encoded form input/output can be specified in two ways. First, it is possible to map all form fields as a
`Seq[(String, String)]`, or `Map[String, String]` (which is more convenient if fields can't have multiple values):

```scala mdoc:compile-only
import sttp.tapir._

formBody[Seq[(String, String)]]: EndpointIO.Body[String, Seq[(String, String)]]
formBody[Map[String, String]]: EndpointIO.Body[String, Map[String, String]]
```

Second, form data can be mapped to a case class. The codec for the case class is automatically derived using a macro at 
compile-time. The fields of the case class should have types, for which there is a plain text codec. For example:

```scala mdoc:compile-only
import sttp.tapir._

case class RegistrationForm(name: String, age: Int, news: Boolean, city: Option[String])

formBody[RegistrationForm]: EndpointIO.Body[String, RegistrationForm]
```

Each form-field is named the same as the case-class-field. The names can be transformed to snake or kebab case by 
providing an implicit `tapir.generic.Configuraton`. 

## Multipart forms

Similarly as above, multipart form input/outputs can be specified in two ways. To map to all parts of a multipart body,
use:

```scala mdoc:compile-only
import sttp.tapir._
import sttp.model.Part

multipartBody: EndpointIO.Body[Seq[RawPart], Seq[Part[Array[Byte]]]]
```

`Part` is a case class containing the `name` of the part, disposition parameters, headers, and the body. The bodies 
will be mapped as byte arrays (`Array[Byte]`). Custom multipart codecs can be defined with the `Codec.multipartCodec`
method, and then used with `multipartBody[T]`.

As with URL-encoded forms, multipart bodies can be mapped directly to case classes, however without the restriction
on codecs for individual fields. Given a field of type `T`, first a plain text codec is looked up, and if one isn't
found, any codec for any media type (e.g. JSON) is searched for.

Each part is named the same as the case-class-field. The names can be transformed to snake or kebab case by 
providing an implicit `sttp.tapir.generic.Configuraton`.
 
Additionally, the case class to which the multipart body is mapped can contain both normal fields, and fields of type 
`Part[T]`. This is useful, if part metadata (e.g. the filename) is relevant. 

For example:

```scala mdoc:compile-only
import sttp.tapir._
import sttp.model.Part
import java.io.File

case class RegistrationForm(userData: User, photo: Part[File], news: Boolean)
case class User(email: String)

multipartBody[RegistrationForm]: EndpointIO.Body[Seq[RawPart], RegistrationForm]
```

## Next

Read on about [authentication](auth.md).
