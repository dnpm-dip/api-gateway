package de.dnpm.dip.rest.util


import java.time.{
  LocalDate,
  LocalDateTime
}
import java.time.format.DateTimeFormatter.{
  ISO_LOCAL_DATE,
  ISO_LOCAL_DATE_TIME
}


abstract class Extractor[S,T]
{
  def unapply(s: S): Option[T]
}

object Extractor
{

  import scala.language.implicitConversions


  def apply[S,T](
    f: S => T
  ): Extractor[S,T] =
    new Extractor[S,T]{
      override def unapply(s: S): Option[T] =
        Some(f(s))
    }


  implicit def unlift[S,T](f: S => Option[T]): Extractor[S,T] =
    new Extractor[S,T]{
      override def unapply(s: S): Option[T] = f(s)
    }


  implicit lazy val isoDate: Extractor[String,LocalDate] =
    Extractor(
      LocalDate.parse(_,ISO_LOCAL_DATE)
    )

  implicit lazy val isoDateTime: Extractor[String,LocalDateTime] =
    Extractor(
      LocalDateTime.parse(_,ISO_LOCAL_DATE_TIME)
    )


  def optional[T](
    implicit ext: Extractor[String,T]
  ): Extractor[Option[String],Option[T]] =
    Extractor(_.map { case ext(t) => t })


  def csvSet[T](
    implicit ext: Extractor[String,T]
  ): Extractor[String,Set[T]] =
    Extractor(
      _.split(",")
       .toSet[String]
       .map { case ext(t) => t }
    )


  def seq[T](
    implicit ext: Extractor[String,T]
  ): Extractor[Seq[String],Seq[T]] =
    Extractor(
      _.map { case ext(t) => t }
    )

}
