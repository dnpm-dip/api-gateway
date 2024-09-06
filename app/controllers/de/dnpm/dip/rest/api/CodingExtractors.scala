package de.dnpm.dip.rest.api


import java.net.URI
import scala.util.{
  Failure,
  Try
}
import de.dnpm.dip.coding.{
  Code,
  Coding,
  CodeSystem
}
import de.dnpm.dip.coding.atc.ATC
import de.dnpm.dip.coding.UnregisteredMedication
import de.dnpm.dip.model.Medications
import shapeless.Coproduct
import de.dnpm.dip.rest.util.Extractor


object CodingExtractors
{

  implicit def fixed[T](
    implicit cs: Coding.System[T]
  ): Extractor[String,Coding[T]] =
    Extractor {
      s =>
        val csv = s split "\\|"

        Coding[T](
          Code(csv(0)),
          None,
          cs.uri,
          Try(csv(2)).toOption
        )
    }


  implicit def systems[T <: Coproduct](
    implicit uris: Coding.System.UriSet[T]
  ): Extractor[String,Coding[T]] =
    Extractor {
      s =>
        val csv = s split "\\|"

        {
          for {
            code <- Try(csv(0))
            uri  <- Try(csv(1)).map(URI.create)
            if (uris.values contains uri)
            version = Try(csv(2)).toOption
          } yield Coding[T](
            Code(code),
            None,
            uri,
            version
          )
        }
        .recoverWith {
          case t =>
            Failure(
              new IllegalArgumentException(s"Invalid or missing 'system', expected one of {${uris.values.mkString(",")}}")
            )
        }
        .get
    }


  import scala.util.matching.Regex
  private val atc = "(?i)(atc)".r.unanchored

  implicit val MedicationCoding: Extractor[String,Coding[Medications]] = {
    param =>
      val csv = param split "\\|"

      {
        for {
        code <- Try(csv(0))
        system =
          Try(csv(1))
            .collect { case atc(_) => Coding.System[ATC].uri }
            .getOrElse(Coding.System[UnregisteredMedication].uri)
        version = Try(csv(2)).toOption
      } yield
        Coding[Medications](
          Code(code),
          None,
          system,
          version
        )
      }
      .toOption
  }
    
}
