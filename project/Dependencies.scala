import sbt._

object Dependencies {
  val minorVersion = SettingKey[Int]("minor scala version")

  object Versions {
    val scalaCheck    = "1.14.3"
    val scalaTest     = "3.1.1"
    val scalaTestPlus = "3.1.1.1"
    val cats          = "2.1.1"

    val bm4           = "0.3.1"
    val kindProjector = "0.11.0"
    val silencer      = "1.6.0"
    val simulacrum    = "1.0.0"
    val macroParadise = "2.1.1"
  }

  object Libs {
    val cats = "org.typelevel" %% "cats-core" % Versions.cats
  }

  object TestLibraries {
    val scalaCheck    = "org.scalacheck"    %% "scalacheck"      % Versions.scalaCheck
    val scalaTest     = "org.scalatest"     %% "scalatest"       % Versions.scalaTest
    val scalaTestPlus = "org.scalatestplus" %% "scalacheck-1-14" % Versions.scalaTestPlus

    val testLibraries = Seq(scalaCheck, scalaTest, scalaTestPlus)
  }

  object Plugins {

    val macroParadise       = "org.scalamacros" % "paradise"            % Versions.macroParadise cross CrossVersion.patch
    val simulacrum          = "org.typelevel"   %% "simulacrum"         % Versions.simulacrum
    val bm4Plugin           = "com.olegpy"      %% "better-monadic-for" % Versions.bm4
    val kindProjectorPlugin = "org.typelevel"   % "kind-projector"      % Versions.kindProjector cross CrossVersion.patch
    val silencerPlugin      = "com.github.ghik" %% "silencer-plugin"    % Versions.silencer cross CrossVersion.full
    val silencerLib         = "com.github.ghik" %% "silencer-lib"       % Versions.silencer % Provided cross CrossVersion.full

    val plugins = Seq(bm4Plugin, kindProjectorPlugin, silencerPlugin).map(compilerPlugin) :+ silencerLib
  }
}
