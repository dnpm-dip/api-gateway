package de.dnpm.dip.rest.api


import java.time.LocalDateTime
import play.api.libs.json.{
  Json,
  OWrites
}


final case class MetaInfo private (
  name: String,
  version: String,
  datetime: LocalDateTime
)

object MetaInfo
{

  def instance =
    MetaInfo(
      BuildInfo.name,
      BuildInfo.version,
      LocalDateTime.now
    )

  implicit val format: OWrites[MetaInfo] =
    Json.writes[MetaInfo]
}
