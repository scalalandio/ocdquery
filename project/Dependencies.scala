import sbt._

import Dependencies._

object Dependencies {

  // scala version
  val scalaOrganization = "org.scala-lang"
  val scalaVersion      = "2.12.8"

  // build tools version
  val scalaFmtVersion = "1.5.1"

  // libraries versions
  val doobieVersion   = "0.7.0"
  val specs2Version   = "4.5.1"

  // resolvers
  val resolvers = Seq(
    Resolver sonatypeRepo "public",
    Resolver typesafeRepo "releases"
  )

  // functional libraries
  val doobie             = "org.tpolecat"                 %% "doobie-core"               % doobieVersion
  val doobieH2           = "org.tpolecat"                 %% "doobie-h2"                 % doobieVersion
  val doobieSpecs2       = "org.tpolecat"                 %% "doobie-specs2"             % doobieVersion
  val magnolia           = "com.propensive"               %% "magnolia"                  % "0.11.0"
  val shapeless          = "com.chuusai"                  %% "shapeless"                 % "2.3.3"
  val quicklens          = "com.softwaremill.quicklens"   %% "quicklens"                 % "1.4.12"
  // testing
  val spec2Core          = "org.specs2"                   %% "specs2-core"               % specs2Version
  val spec2Mock          = "org.specs2"                   %% "specs2-mock"               % specs2Version
  val spec2Scalacheck    = "org.specs2"                   %% "specs2-scalacheck"         % specs2Version
}

trait Dependencies {

  val scalaOrganizationUsed = scalaOrganization
  val scalaVersionUsed = scalaVersion

  val scalaFmtVersionUsed = scalaFmtVersion

  // resolvers
  val commonResolvers = resolvers

  val mainDeps = Seq(doobie, magnolia, shapeless)

  val testDeps = Seq(doobieH2, doobieSpecs2, spec2Core, spec2Mock, spec2Scalacheck, quicklens)

  implicit final class ProjectRoot(project: Project) {

    def root: Project = project in file(".")
  }

  implicit final class ProjectFrom(project: Project) {

    private val commonDir = "modules"

    def from(dir: String): Project = project in file(s"$commonDir/$dir")
  }

  implicit final class DependsOnProject(project: Project) {

    private val testConfigurations = Set("test", "fun", "it")
    private def findCompileAndTestConfigs(p: Project) =
      (p.configurations.map(_.name).toSet intersect testConfigurations) + "compile"

    private val thisProjectsConfigs = findCompileAndTestConfigs(project)
    private def generateDepsForProject(p: Project) =
      p % (thisProjectsConfigs intersect findCompileAndTestConfigs(p) map (c => s"$c->$c") mkString ";")

    def compileAndTestDependsOn(projects: Project*): Project =
      project dependsOn (projects.map(generateDepsForProject): _*)
  }
}
