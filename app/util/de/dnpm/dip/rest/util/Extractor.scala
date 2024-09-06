package de.dnpm.dip.rest.util


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
import cats.syntax.traverse._
import shapeless.Coproduct


abstract class Extractor[S,T]
{
  def unapply(s: S): Option[T]
}

object Extractor
{

  import scala.language.implicitConversions

  implicit def unlift[S,T](f: S => Option[T]): Extractor[S,T] =
    new Extractor[S,T]{
      override def unapply(s: S): Option[T] = f(s)
    }


  def apply[S,T](
    f: S => T
  ): Extractor[S,T] =
    new Extractor[S,T]{
      override def unapply(s: S): Option[T] =
        Some(f(s))
    }


  def optional[S,T](f: S => T): Extractor[Option[S],Option[T]] =
    apply((opt: Option[S]) => opt.map(f))


  def csvSet[T](
    implicit ext: Extractor[String,T]
  ): Extractor[String,Set[T]] =
    Extractor(
      _.split(",")
       .toList
       .map { case ext(t) => t }
       .toSet
    )

  def seq[T](
    implicit ext: Extractor[String,T]
  ): Extractor[Seq[String],Seq[T]] =
    Extractor(
      _.map { case ext(t) => t }
    )

}



object CodingExtractors
{

  implicit def fixedSystem[T](
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
 

  implicit def systemCoproduct[T <: Coproduct](
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
              new Exception(s"Invalid 'system', expected one of {${uris.values.mkString(",")}}")
            )
        }
        .get
    }
    
}
