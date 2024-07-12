package de.dnpm.dip.rest.api


import de.dnpm.dip.rest.util.sapphyre.{
  Hyper,
  Link
}
import de.dnpm.dip.mtb.query.api.MTBConfig


trait MTBHypermedia extends UseCaseHypermedia[MTBConfig]
{

  override implicit def HyperQuery: Hyper.Mapper[QueryType] =
    super.HyperQuery
      .andThen(
        h => h.addLinks(
          "kaplan-meier-config" -> Link(s"$BASE_URI/kaplan-meier/config"),
          "kaplan-meier-stats"  -> Link(s"${Uri(h.data)}/survival-statistics[?type={type}&grouping={grouping}]"),
          "therapy-responses"   -> Link(s"${Uri(h.data)}/therapy-responses")
        )
      )

}
