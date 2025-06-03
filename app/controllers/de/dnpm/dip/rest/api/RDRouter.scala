package de.dnpm.dip.rest.api


import javax.inject.Inject
import scala.util.chaining._
import play.api.routing.sird._
import play.api.mvc.Results.Ok
import play.api.libs.json.Json.toJson
import json.Schema
import json.schema.Version._
import com.github.andyglow.jsonschema.AsPlay._
import de.dnpm.dip.rd.query.api.RDConfig
import de.dnpm.dip.rd.model.RDPatientRecord
import de.dnpm.dip.rd.model.json.Schemas._
import de.dnpm.dip.rd.gens.Generators._
import de.ekut.tbi.generators.Gen
import de.dnpm.dip.service.DataUpload


class RDRouter @Inject()(
  override val controller: RDController
)
extends UseCaseRouter[RDConfig]("rd")
with FakeDataGen[RDPatientRecord]
{

  import DataUpload.Schemas._

  override val jsonSchemas =
    Map(
      APPLICATION_JSON -> Map(
        "draft-12" -> Schema[DataUpload[RDPatientRecord]].asPlay(Draft12("RD-Patient-Record")),
        "draft-09" -> Schema[DataUpload[RDPatientRecord]].asPlay(Draft09("RD-Patient-Record")),
        "draft-07" -> Schema[DataUpload[RDPatientRecord]].asPlay(Draft07("RD-Patient-Record")),
        "draft-04" -> Schema[DataUpload[RDPatientRecord]].asPlay(Draft04())
      )
    )

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
