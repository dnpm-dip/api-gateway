package de.dnpm.dip.rest.util


import java.net.URI
import scala.util.Try
import de.dnpm.dip.coding.{
  Code,
  Coding,
  CodeSystem
}
import cats.syntax.traverse._
import shapeless.{
  Coproduct,
  <:!<
}


abstract class Extractor[S,T]
{
  def unapply(s: S): Option[T]
}

object Extractor
{

  def unlift[S,T](f: S => Option[T]): Extractor[S,T] =
    new Extractor[S,T]{
      override def unapply(s: S): Option[T] =
        f(s)
    }

  def apply[S,T](
    f: S => T
  ): Extractor[S,T] =
    new Extractor[S,T]{
      override def unapply(s: S): Option[T] =
        Some(f(s))
    }


  def AsCoding[T: CodeSystem]: Extractor[String,Coding[T]] =
    unlift[String,Coding[T]](
      CodeSystem[T].codingWithCode(_)
    )
  
  def AsCodingsOf[T: Coding.System]: Extractor[Seq[String],Set[Coding[T]]] =
    Extractor[Seq[String],Set[Coding[T]]](
      _.toSet[String].map(Coding[T](_))
    )


  def AsCodings[T: CodeSystem]: Extractor[Seq[String],Set[Coding[T]]] =
    Extractor[Seq[String],Set[Coding[T]]](
      _.toSet.flatMap(CodeSystem[T].codingWithCode(_))
    )


  def Codings[C <: Coproduct](
    implicit uris: Coding.System.UriSet[C]
  ): Extractor[Seq[String],Set[Coding[C]]] =
    Extractor.unlift[Seq[String],Set[Coding[C]]](
      _.map(_ split "\\|")
       .map {
         csv =>
           for {
             code <- Try(csv(0))
             uri  <- Try(csv(1)).map(URI.create)
             if (uris.values contains uri)
             version = Try(csv(2)).toOption
           } yield
             Coding[C](
               Code(code),
               None,
               uri,
               version
             )
       }
       .sequence
       .toOption
       .map(_.toSet)
   )

}
