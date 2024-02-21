package de.dnpm.dip.rest.util



import play.api.libs.json.{
  Json,
  JsPath,
  JsonValidationError,
  Writes,
  OWrites,
}
import cats.data.{
  Ior,
  IorNel,
  NonEmptyList,
}



final case class Outcome private (
  issues: List[Outcome.Issue]
)


object Outcome
{

  final case class Issue
  (
    severity: Issue.Severity.Value,
    details: String
  )

  object Issue
  {

    object Severity extends Enumeration
    {
      val Fatal       = Value("fatal")
      val Error       = Value("error")
      val Warning     = Value("warning")
      val Information = Value("information")

      implicit val format: Writes[Severity.Value] =
        Json.formatEnum(this)
    }


    def Error(details: String) =
      Issue(Severity.Error,details)

    def Warning(details: String) =
      Issue(Severity.Warning,details)

    def Info(details: String) =
      Issue(Severity.Information,details)


    implicit val writes: OWrites[Issue] =
      Json.writes[Issue]

  }


//  implicit val writes: Writes[Outcome] =
//    Json.writes[Outcome]

  // Define Custom Writes so that field "issues" be called "_issues",
  // in order to maintain convention that field "_issues" will also occur
  // on resources in case of partial success
  implicit val writes: OWrites[Outcome] =
    OWrites(
      out =>
        Json.obj("_issues" -> Json.toJson(out.issues))
    )



  def apply(
    errors: Iterable[(JsPath, Iterable[JsonValidationError])]
  ): Outcome = {
    Outcome(
      errors.flatMap {
        case (path,errs) =>
          errs.map(e => Issue.Error(s"${path.toString}: ${e.message}"))
      }
      .toList
    )

  }

  def apply(
    errors: NonEmptyList[String]
  ): Outcome = 
    Outcome(errors.toList.map(Issue.Error))


  def apply(
    error: String
  ): Outcome = 
    Outcome(List(error).map(Issue.Error))

}
