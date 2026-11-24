package de.dnpm.dip.rest.api


import javax.inject.Inject
import scala.util.chaining._
import play.api.routing.sird._
import play.api.mvc.Results.Ok
import play.api.libs.json.Json.toJson
import de.dnpm.dip.rd.query.api.RDConfig
import de.dnpm.dip.rd.model.RDPatientRecord
import de.dnpm.dip.rd.gens.Generators._
import de.ekut.tbi.generators.Gen
import de.dnpm.dip.service.DataUpload


class RDRouter @Inject()(
  override val controller: RDController
)
extends UseCaseRouter[RDConfig]
with FakeDataGen[RDPatientRecord]
{

  override val additionalRoutes = {

    case GET(p"/queries/${QueryId(id)}/diagnostics") =>
      controller.diagnostics(id)

    case GET(p"/fake/data/patient-record") =>
      controller.Action {
        Gen.of[RDPatientRecord].next
          .pipe(toJson(_))
          .pipe(Ok(_))
      }

    case GET(p"/fake/data/mvh-submission") =>
      controller.Action {
        Gen.of[DataUpload[RDPatientRecord]].next
          .pipe(toJson(_))
          .pipe(Ok(_))
      }

  }

}
