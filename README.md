This is a command line tool based on graalson-trax, a Java based JSON transformer that uses Javascript templates to create JSON output.

https://github.com/warrenc5/graalson-trax

## Usage

JsonT templateFile [inputJson|-] [outputFile|-] [charset|UTF-8]

- templateFile a javascript location - resourcePath passed to Source constructor
- inputJson a resourcePath or a filePath or - to indicate STDIN (default)
- outputFile a filePath or - to indicate STDOUT (default)
- charset defaults to UTF-8 for input and output.

### Running with JBang (Recommended)

The easiest way to run JsonT is using [JBang](https://www.jbang.dev/):

```bash
# Direct execution (file is marked executable)
./src/main/java/JsonT.java template.js src/test/resources/ALL_FINES.json all_fines_nsw.json UTF-8

# Or explicitly with jbang
jbang src/main/java/JsonT.java template.js src/test/resources/ALL_FINES.json all_fines_nsw.json UTF-8

# With stdin
cat src/test/resources/ALL_FINES.json | ./src/main/java/JsonT.java template.js > all_fines_nsw.json
```

### Running with Java and Maven

Alternatively, you can run with a traditional Java classpath:

```bash
java -classpath src/test/resources:`find target -name \*.jar | tr -t '\n' ':'` JsonT template.js src/test/resources/ALL_FINES.json all_fines_nsw.json UTF-8

cat src/test/resources/ALL_FINES.json | java -classpath src/test/resources:`find target -name \*.jar | tr -t '\n' ':'` JsonT template.js > all_fines_nsw.json
```

## Template

Templates are JavaScript files evaluated by GraalJS. The input JSON is bound to `_` and you assign your output to `$`.

- **`_`** — the parsed input JSON (read-only)
- **`$`** — assign your result object/array to this variable to produce output

> **Reserved variable names:** The name **`result`** is reserved for internal use by the
> GraalJS/graalson runtime. Using `const result = ...` or `let result = ...` in a template
> will fail with `TypeError: Assignment to constant "result"`. Use a different name
> such as `merged`, `output`, `data`, etc.

```
const hits = _.hits.hits
const l = hits.length
function expand(o) {
return {t:o.offence}
}

$ = {
results:  hits.map(h=> [
"'" + (h.fields === undefined ? "<none>": h.fields.Description[0]) + "'",
h._source.Points,
h._source.Fine,
h._source.type,
"'"+h._source.Section+"'",
].join(',')).sort()
}
```

## Output

```
-rw-rw-r-- 1 wozza wozza 71977 Dec  3 08:44 all_fines_nsw.csv
{
    results: [
        'AFM hours - critical',4,$20,590,DRIVER FATIGUE,'Sec 258 (1)',
        'AFM hours - severe',3,$13,730,DRIVER FATIGUE,'Sec 258 (1)',
        'Approach children�s crossing too quickly to stop safely (school zone)',4,$704,PEDESTRIAN CROSSINGS,'Rule 80 (1)',
        'Approach children�s crossing too quickly to stop safely',3,$562,PEDESTRIAN CROSSINGS,'Rule 80 (1)',
```

## Native Compilation

Building a native executable produces a standalone binary (`jsont`) that starts instantly and requires no JVM at runtime.

### Prerequisites

- [GraalVM JDK 21+](https://www.graalvm.org/) (install via [SDKMAN](https://sdkman.io/): `sdk install java 21-graal`)
- Native Image component (included in GraalVM JDK 21+)
- C toolchain (`gcc`, `glibc-devel`, `zlib-devel` on Linux)

### Build

```bash
mvn -Pnative install
```

This runs the full build lifecycle including:

1. **test** — generates resource configuration
2. **pre-integration-test** — runs the Java agent to collect native-image reachability metadata
3. **post-integration-test** — compiles the native image using `native-maven-plugin`

The resulting binary is written to `target/jsont`.

### Running the Native Binary

A `run-native.sh` script is provided for quick testing:

```bash
# run-native.sh
target/jsont src/test/resources/template.js src/test/resources/ALL_FINES.json  | head
```

You can also run the binary directly:

```bash
# With file arguments
target/jsont template.js input.json output.json UTF-8

# With stdin/stdout
cat input.json | target/jsont template.js > output.json
```

### Installing System-Wide

Copy the native binary to a directory on your `PATH` so you can invoke it from anywhere:

```bash
sudo cp target/jsont /usr/local/bin/jsont
```

Then use it like any other command:

```bash
jsont template.js input.json output.json
cat data.json | jsont template.js > result.json
```

## Advanced Usage — Merging GraalVM Native Image Metadata

When building a native image, GraalVM collects reachability metadata (`reflect-config.json`,
`jni-config.json`, `proxy-config.json`, `resource-config.json`, `serialization-config.json`)
from many libraries scattered across `target/graalvm-reachability-metadata/`. The included
`joiner.sh` script uses `jsont` itself to merge all files of each config type into a single
consolidated file.

### Merge Templates

Three JS templates handle the different config structures:

| Template                 | Config Types                                                  | Strategy                                                                               |
| ------------------------ | ------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| `merge-arrays.js`        | `reflect-config.json`, `jni-config.json`, `proxy-config.json` | Flat concatenation of top-level JSON arrays                                            |
| `merge-objects.js`       | `resource-config.json`                                        | Recursive deep-merge — concatenates inner arrays (`includes`, `excludes`, `bundles`)   |
| `merge-serialization.js` | `serialization-config.json`                                   | Normalises mixed array/object forms, merges `types`, `lambdaCapturingTypes`, `proxies` |

### Running joiner.sh

```bash
# Default: search target/, output to target/merged-native-config/
./joiner.sh

# Specify search and output directories
./joiner.sh target/graalvm-reachability-metadata merged-output

# Use jbang instead of the native binary
JSONT="./src/main/java/JsonT.java" ./joiner.sh
```

The script:

1. Discovers every unique `*-config.json` filename under the search directory
2. Wraps all files of each type into a single JSON array (`[ <file1>, <file2>, ... ]`)
3. Pipes that array into `jsont` with the matching merge template
4. Writes the merged result to the output directory

Example output:

```
──────────────────────────────────────────────
  joiner.sh — GraalVM native-image config merger
  search : target/graalvm-reachability-metadata
  output : target/merged-native-config
  jsont  : target/jsont
──────────────────────────────────────────────

jni-config.json                  10 file(s)  ← merge-arrays.js
                                wrote 66125 bytes → target/merged-native-config/jni-config.json
proxy-config.json                15 file(s)  ← merge-arrays.js
                                wrote 28917 bytes → target/merged-native-config/proxy-config.json
reflect-config.json             117 file(s)  ← merge-arrays.js
                                wrote 4163181 bytes → target/merged-native-config/reflect-config.json
resource-config.json             83 file(s)  ← merge-objects.js
                                wrote 156007 bytes → target/merged-native-config/resource-config.json
serialization-config.json         5 file(s)  ← merge-serialization.js
                                wrote 4990 bytes → target/merged-native-config/serialization-config.json
```

### Installing the Merged Metadata

Copy the merged configs into your project's native-image metadata directory so they
are picked up automatically on the next native build:

```bash
cp target/merged-native-config/*.json \
    src/main/resources/META-INF/native-image/au.com.devnull/json-transformer/
```

### How the Templates Work

Each template receives `_` (an array of parsed config file contents) and assigns a
merged result to `$`.

**`merge-arrays.js`** — concatenates arrays:

```
const merged = []
for (const arr of _) {
    if (Array.isArray(arr)) {
        for (const item of arr) {
            merged.push(item)
        }
    }
}
$ = merged
```

**`merge-objects.js`** — recursively merges objects, concatenating any inner arrays:

```
function deepMerge(target, source) {
  for (const key of Object.keys(source)) {
    const val = source[key]
    if (Array.isArray(val)) {
      if (!Array.isArray(target[key])) target[key] = []
      for (const item of val) target[key].push(item)
    } else if (typeof val === 'object' && val !== null) {
      if (typeof target[key] !== 'object' || target[key] === null) target[key] = {}
      deepMerge(target[key], val)
    } else if (!(key in target)) {
      target[key] = val
    }
  }
  return target
}

const merged = {}
for (const obj of _) {
  if (obj !== null && typeof obj === 'object' && !Array.isArray(obj))
    deepMerge(merged, obj)
}
$ = merged
```

**`merge-serialization.js`** — handles `serialization-config.json` files that may be
either a plain array `[...]` or an object `{ types: [...], ... }`, normalising both
forms before merging.

## Dependencies

Library is uses Graalson & Graalson-Trax

https://www.github.com/warrenc5/graalson-trax

```
        <dependency>
            <groupId>mobi.mofokom</groupId>
            <artifactId>graalson-trax</artifactId>
            <version>1.0.3</version>
        </dependency>
```

And

https://www.github.com/warrenc5/graalson

```
        <dependency>
            <groupId>mobi.mofokom</groupId>
            <artifactId>graalson</artifactId>
            <version>1.0.5</version>
        </dependency>
```

## Thanks to NSW driving demerit points for motivating me to write this project. - Drive well out there.

> curl -o ALL_FINES.json https://www.nsw.gov.au/api/v1/elasticsearch/prod_demerit_points/_search?source_content_type=application%2Fjson&source=%7B%22from%22%3A0%2C%22size%22%3A700%2C%22query%22%3A%7B%22match_all%22%3A%7B%7D%7D%2C%22sort%22%3A%5B%7B%22_script%22%3A%7B%22type%22%3A%22number%22%2C%22script%22%3A%7B%22lang%22%3A%22painless%22%2C%22source%22%3A%22if+%28doc%5B%27Fine.keyword%27%5D.size%28%29+%3D%3D+0%29+%7B+return+0%3B+%7D+String+fine+%3D+doc%5B%27Fine.keyword%27%5D.value%3B+if+%28fine+%3D%3D+null+%7C%7C+fine+%3D%3D+%27N%2FA%27%29+%7B+return+0%3B+%7D+String+clean+%3D+fine.replace%28%27%24%27%2C+%27%27%29.replace%28%27%2C%27%2C+%27%27%29%3B+return+Double.parseDouble%28clean%29%3B%22%7D%2C%22order%22%3A%22asc%22%7D%7D%5D%2C%22fields%22%3A%5B%22recordid%22%2C%22offence%22%2C%22type%22%2C%22Class%22%2C%22Description%22%2C%22Law%22%2C%22Section%22%2C%22Fine%22%2C%22Fine+Level%22%2C%22Points%22%2C%22Double+demerits%22%2C%22%40delta%22%5D%7D
