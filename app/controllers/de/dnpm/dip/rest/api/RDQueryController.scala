package de.dnpm.dip.rest.api



import javax.inject.Inject
import scala.concurrent.{
  Future,
  ExecutionContext
}
import play.api.mvc.{
  Request,
  ControllerComponents
}
import play.api.libs.json.{
  Json,
  Format,
  Reads,
  Writes
}
import de.dnpm.dip.rest.util._
import de.dnpm.dip.service.query.PatientFilter
import de.dnpm.dip.coding.Coding 
import de.dnpm.dip.rd.model.{ 
  HPO, Orphanet
}
import de.dnpm.dip.rd.query.api.{
  RDConfig,
  RDFilters,
  HPOFilter,
  DiagnosisFilter,
  RDQueryService
}


class RDQueryController @Inject()(
  override val controllerComponents: ControllerComponents,
)(
  implicit ec: ExecutionContext,
)
extends QueryController[RDConfig]
{

  override val service: RDQueryService =
    RDQueryService.getInstance.get

  import scala.util.chaining._

  private val HPOTerms =
    Extractor.AsCodingsOf[HPO]

  private val Categories =
    Extractor.AsCodingsOf[Orphanet]


  override def FilterFrom[T](
    req: Request[T],
    patientFilter: PatientFilter
  ): RDFilters = {

    val hpos =
      req.queryString.get("hpo[value]") match {
        case Some(HPOTerms(hpos)) if hpos.nonEmpty => Some(hpos)
        case _ => None
      }

    val categories =
      req.queryString.get("diagnosis[category]") match {
        case Some(Categories(orphas)) if orphas.nonEmpty => Some(orphas)
        case _  => None
      }


    RDFilters(
      patientFilter,
      HPOFilter(hpos),
      DiagnosisFilter(categories)
    )
  }

}
