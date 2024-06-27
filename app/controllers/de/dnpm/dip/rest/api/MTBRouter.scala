package de.dnpm.dip.rest.api


import javax.inject.Inject
import scala.util.Random
import scala.util.chaining._
import play.api.routing.sird._
import play.api.mvc.Results.Ok
import play.api.libs.json.Json.toJson
import json.Schema
import json.schema.Version._
import com.github.andyglow.jsonschema.AsPlay._
import de.dnpm.dip.mtb.query.api.{
  MTBConfig,
  MTBQueryService
}
import de.dnpm.dip.mtb.model.MTBPatientRecord
import de.dnpm.dip.mtb.model.v1
import de.dnpm.dip.mtb.gens.Generators._
import de.ekut.tbi.generators.Gen
import de.dnpm.dip.mtb.query.api.KaplanMeier
import de.dnpm.dip.rest.util.Extractor



class MTBRouter @Inject()(
  override val controller: MTBController
)
extends UseCaseRouter[MTBConfig]("mtb")
{

  private implicit val rnd: Random =
    new Random



  override val jsonSchemas =
    Map(
      APPLICATION_JSON -> {
        import de.dnpm.dip.mtb.model.v1.json.Schemas._
        Map(
          "draft-12" -> Schema[v1.MTBPatientRecord].asPlay(Draft12("MTB-Patient-Record")),
          "draft-09" -> Schema[v1.MTBPatientRecord].asPlay(Draft09("MTB-Patient-Record")),
          "draft-07" -> Schema[v1.MTBPatientRecord].asPlay(Draft07("MTB-Patient-Record")),
          "draft-04" -> Schema[v1.MTBPatientRecord].asPlay(Draft04())
        )
      },      
      s"$APPLICATION_JSON+v2" -> {
        import de.dnpm.dip.mtb.model.json.Schemas._
        Map(
          "draft-12" -> Schema[MTBPatientRecord].asPlay(Draft12("MTB-Patient-Record")),
          "draft-09" -> Schema[MTBPatientRecord].asPlay(Draft09("MTB-Patient-Record")),
          "draft-07" -> Schema[MTBPatientRecord].asPlay(Draft07("MTB-Patient-Record")),
          "draft-04" -> Schema[MTBPatientRecord].asPlay(Draft04())
        )
      }
    )



  private val SurvivalType: Extractor[String,KaplanMeier.SurvivalType.Value] =
   KaplanMeier.SurvivalType.unapply(_)

  private val Grouping: Extractor[String,KaplanMeier.Grouping.Value] =
    KaplanMeier.Grouping.unapply(_)


  override val additionalRoutes = {

    case GET(p"/kaplan-meier/config") =>
      controller.kaplanMeierConfig


    case GET(p"/queries/${QueryId(id)}/survival-statistics"
      ? q"type=${SurvivalType(typ)}"
      & q"grouping=${Grouping(grp)}") =>
      controller.survivalStatistics(id,Some(typ),Some(grp))

    case GET(p"/queries/${QueryId(id)}/survival-statistics") =>
      controller.survivalStatistics(id,None,None)


    case GET(p"/fake/data/patient-record") =>
      controller.Action {
        Gen.of[MTBPatientRecord].next
          .pipe(toJson(_))
          .pipe(Ok(_))
      }

  }

}
