package de.dnpm.dip.rest.util.sapphyre


trait HypermediaBase
{

  protected val BASE_URL: String =
    System.getProperty("dnpm.dip.rest.api.baseurl","")


  protected val BASE_URI: String

}
