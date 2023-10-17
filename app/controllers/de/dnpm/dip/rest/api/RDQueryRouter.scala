package de.dnpm.dip.rest.api


import javax.inject.Inject
import scala.util.Random
import scala.util.chaining._
import play.api.routing.sird._
import play.api.mvc.Results.Ok
import play.api.libs.json.Json.toJson
import de.dnpm.dip.rd.query.api.{
  RDConfig,
  RDQueryService
}
import de.dnpm.dip.rd.model.RDPatientRecord
import de.dnpm.dip.rd.gens.Generators._
import de.ekut.tbi.generators.Gen



class RDQueryRouter @Inject()(
  override val controller: RDQueryController
)
extends QueryRouter[RDConfig](
  "rd"
)
{

  private implicit val rnd: Random =
    new Random

  override val additionalRoutes = {

    case GET(p"/fake/data/patient-record") =>
      controller.Action {
        Gen.of[RDPatientRecord].next
          .pipe(toJson(_))
          .pipe(Ok(_))
      }
  }

}

