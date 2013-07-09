#! /usr/bin/env fansubstitute

using build
using fandoc

**
** Fantom build script for "haystack.jar"
** See `http://fantom.org/`
**
class Build : BuildJava
{

  new make()
  {
    baseDir = scriptDir.parent
    jar = baseDir.uri + `haystack.jar`
    cp  = [baseDir.uri + `lib/servlet.jar`]
    mainClass = "org.projecthaystack.test.Test"
    packages = ["org.projecthaystack",
                "org.projecthaystack.io",
                "org.projecthaystack.client",
                "org.projecthaystack.server",
                "org.projecthaystack.test",
                "org.projecthaystack.util"]
  }

  const File baseDir

  @Target { help = "clean up files we don't want to zip" }
  Void cleanup()
  {
    Delete(this, baseDir +`haystack.jar`).run
    Delete(this, scriptDir +`temp/`).run
  }

  @Target { help = "compile index.fandoc into index.html" }
  Void doc()
  {
    inFile  := baseDir + `index.fandoc`
    outFile := baseDir + `index.html`
    doc := FandocParser().parse("index.fandoc", inFile.in)
    w := HtmlDocWriter(outFile.out)

    // header
    w.out.print(
     Str<|<html>
          <head>
          <title>Haystack Java Toolkit</title>
          <style type="text/css">
          <!--
          body {
            background: #ffffff;
          }

          h1 {
            color: #000;
            background: #eee;
            border-bottom: 1px solid black;
            padding-left: 5px;
            font-size: 18pt;
          }

          h1.title {
            color: #144b7a;
            background: none;
            font-size: 24pt;
            border: none;
          }

          h2 {
            font-size: 16pt;
            padding-left: 0.5em;
            width: 40%;
          }

          h3, h4, h5, h6
          {
            font-size: 14pt;
            padding-left: 0.5em;
          }

          p {
            padding-left: 10px;
            padding-right: 10px;
          }

          pre {
            font-family: monospace;
            padding-left: 4em;
            color: #008000;
          }

          ul { padding-left: 2em; }
          ol {   padding-left: 2em; }
          li {   margin: 0.3em; }
          table { padding-left: 3em; }
          -->
          </style>
          </head>
          <body>

          <!-- Title Block -->
          <h1 class='title'>Haystack Java Toolkit</h1>|>)

    // body
    doc.children.each |elem| { elem.write(w) }

    // footer
    w.out.print("</body></html>")
    w.out.close
  }

  @Target { help = "generate javadoc" }
  Void javadoc()
  {
    exe := configDir("jdkHome") + `bin/javadoc`
    cmd := [Exec.exePath(exe), "-d", "doc", "-classpath", scriptDir.osPath]
    docPackages := packages.dup { remove("org.projecthaystack.test") }
    cmd.addAll(docPackages)
    Exec(this, cmd, baseDir).run
  }

}