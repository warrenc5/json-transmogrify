const File = Java.type("java.io.File");
const Files = Java.type("java.nio.file.Files");
const Paths = Java.type("java.nio.file.Paths");

function loadAndEval(relativePath) {
  const file = new File(relativePath);
  const absolutePath = file.getAbsolutePath();
  const scriptContent = new java.lang.String(Files.readAllBytes(Paths.get(absolutePath)));
  return eval(scriptContent);
}

loadAndEval("src/test/resources/template.js");

//$ = { results: $.results, total: _.hits.hits.length };
//$ = { ...$, total: _.hits.hits.length };
$ = { ...$.results, total: _.hits.hits.length };
