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
        q => q.addLinks(
          "kaplan-meier-config" -> Link(s"$BASE_URI/kaplan-meier/config"),
          "kaplan-meier-stats"  -> Link(s"${Uri(q.data)}/survival-statistics[?type={type}&grouping={grouping}]"),
          "tumor-diagnostics"   -> Link(s"${Uri(q.data)}/tumor-diagnostics"),
          "medication"          -> Link(s"${Uri(q.data)}/medication"),
          "therapy-responses"   -> Link(s"${Uri(q.data)}/therapy-responses")
        )
      )

}
