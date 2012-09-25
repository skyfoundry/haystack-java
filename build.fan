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
    jar = scriptDir.uri + `haystack.jar`
    cp  = [scriptDir.uri + `lib/servlet.jar`]
    mainClass = "haystack.test.Test"
    packages = ["haystack",
                "haystack.io",
                "haystack.server",
                "haystack.test"]
  }

  @Target { help = "clean up files we don't want to zip" }
  Void cleanup()
  {
    Delete(this, scriptDir+`haystack.jar`).run
    Delete(this, scriptDir+`temp/`).run
  }

  @Target { help = "compile index.fandoc into index.html" }
  Void doc()
  {
    inFile  := scriptDir + `index.fandoc`
    outFile := scriptDir + `index.html`
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
    cmd := ["javadoc", "-d", "doc"]
    docPackages := packages.dup { remove("haystack.test") }
    cmd.addAll(docPackages)
    Exec(this, cmd, scriptDir).run
  }

}