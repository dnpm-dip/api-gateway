package de.dnpm.dip.rest.api



import javax.inject.Inject
import scala.concurrent.{
  Future,
  ExecutionContext
}
import play.api.mvc.{
  Action,
  AnyContent,
  RequestHeader,
  ControllerComponents
}
import play.api.libs.json.{
  Json,
  Format,
  Reads,
  Writes
}
import de.dnpm.dip.rest.util._
import de.dnpm.dip.service.query.{
  PatientFilter,
  Query,
  ResultSet
}
import de.dnpm.dip.coding.Coding 
import de.dnpm.dip.mtb.query.api.{
  MTBConfig,
  MTBFilters,
  MTBQueryService,
  MTBResultSet
}


class MTBQueryController @Inject()(
  override val controllerComponents: ControllerComponents,
)(
  implicit ec: ExecutionContext,
)
extends QueryController[MTBConfig]
{

  override lazy val prefix = "mtb"


  override val service: MTBQueryService =
    MTBQueryService.getInstance.get


  override def FilterFrom(
    req: RequestHeader,
    patientFilter: PatientFilter
  ): MTBFilters = 
    MTBFilters(
      patientFilter,
    )
  
}
