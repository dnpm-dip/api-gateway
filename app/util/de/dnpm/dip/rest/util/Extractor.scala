package de.dnpm.dip.rest.util


import de.dnpm.dip.coding.{
  Coding,
  CodeSystem
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
  

  def AsCodings[T: CodeSystem]: Extractor[Seq[String],Set[Coding[T]]] =
    Extractor[Seq[String],Set[Coding[T]]](
      _.toSet.flatMap(CodeSystem[T].codingWithCode(_))
    )


  def AsCodingsOf[T: Coding.System]: Extractor[Seq[String],Set[Coding[T]]] =
    Extractor[Seq[String],Set[Coding[T]]](
      _.toSet[String].map(Coding[T](_))
    )

}
