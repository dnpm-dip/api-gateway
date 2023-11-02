package de.dnpm.dip.rest.util


import play.api.libs.json.{
  Json,
  Writes,
  OWrites
}


final case class Collection[T: Writes]
(
  entries: Seq[T]
)


object Collection
{

  implicit def writes[T: Writes]: OWrites[Collection[T]] =
    Json.writes[Collection[T]]

}
