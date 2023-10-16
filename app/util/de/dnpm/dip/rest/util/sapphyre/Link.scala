package de.dnpm.dip.rest.util.sapphyre


import play.api.libs.json.{
  Json,
  Writes
}


final case class Link(href: String) extends AnyVal

object Link
{
  implicit val format: Writes[Link] =
    Json.valueWrites[Link]
}
