package de.dnpm.dip.rest.api


import java.time.LocalDateTime
import de.dnpm.dip.coding.Coding
import de.dnpm.dip.model.Site
import play.api.libs.json.{
  Json,
  OWrites
}


final case class MetaInfo private (
  name: String,
  version: String,
  site: Coding[Site],
  datetime: LocalDateTime
)

object MetaInfo
{

  def instance =
    MetaInfo(
      BuildInfo.name,
      BuildInfo.version,
      Site.local,
      LocalDateTime.now
    )

  implicit val format: OWrites[MetaInfo] =
    Json.writes[MetaInfo]
}
