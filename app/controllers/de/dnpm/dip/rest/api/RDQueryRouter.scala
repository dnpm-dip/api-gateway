package de.dnpm.dip.rest.api


import javax.inject.Inject
import de.dnpm.dip.rd.query.api.{
  RDConfig,
  RDQueryService
}


class RDQueryRouter @Inject()(
  override val controller: RDQueryController
)
extends QueryRouter[RDConfig](
  "rd"
)

