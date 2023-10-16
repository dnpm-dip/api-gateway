package de.dnpm.dip.rest.util.sapphyre



import play.api.libs.json.{
  Json,
  Format,
  Writes
}


object Method extends Enumeration
{
  type Method = Value

  val DELETE, GET, PATCH, POST, PUT, OPTIONS = Value

  implicit val format: Format[Method.Value] =
    Json.formatEnum(this)
}



object MediaType
{

  val APPLICATION_JSON =
    "application/json"

  val JSON =
    APPLICATION_JSON

  val JSON_SCHEMA =
    "application/schema+json"

  val FORM_URL_ENCODED =
    "application/x-www-form-urlencoded"

  val APPLICATION_FORM_URL_ENCODED =
    FORM_URL_ENCODED

  //TODO: other MIME Types

}


case class Operation
(
  method: Method.Value,
  href: Link,
  formats: Map[String,Link] = Map.empty
)
{
  def withFormats(fs: (String,Link)*) =
    copy(formats = fs.toMap)
}


object Operation
{
  implicit val writesOperation: Writes[Operation] =
    Json.writes[Operation]
}
