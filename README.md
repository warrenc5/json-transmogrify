This is a command line tool based on graalson-trax, a Java based JSON transformer that uses Javascript templates to create JSON output.

https://github.com/warrenc5/graalson-trax

## Usage

### Transform Mode

```
JsonT templateFile [inputJson|-] [outputFile|-] [charset|UTF-8] [spaces|4]
```

- **templateFile** — a JavaScript location (resourcePath passed to Source constructor)
- **inputJson** — a resourcePath or filePath, or `-` for STDIN (default)
- **outputFile** — a filePath or `-` for STDOUT (default)
- **charset** — defaults to UTF-8 for input and output
- **spaces** — JSON output indentation (default: 4)

> **Note:** Arguments are positional. If you need to specify `charset` or `spaces`, you must also specify `outputFile` explicitly. Use `-` for STDOUT, e.g.:
> `./JsonT.java template.js input.json - UTF-8 2`

### Operation Mode

```
JsonT <operation> <operand1|-> <operand2> [outputFile|-]
```

JsonT supports four structured JSON operations. Either operand can be `-` to read from STDIN. The optional fourth argument is an output file path, or `-` for STDOUT (the default). Since arguments are positional, use `-` explicitly for STDOUT if subsequent arguments are needed.

| Operation | RFC                                            | Description                                                        |
| --------- | ---------------------------------------------- | ------------------------------------------------------------------ |
| `diff`    | [RFC 7396](http://tools.ietf.org/html/rfc7396) | Compute an RFC 7396 merge patch from two documents                 |
| `merge`   | [RFC 7396](http://tools.ietf.org/html/rfc7396) | Apply an RFC 7396 merge patch to a document                        |
| `patch`   | [RFC 6902](http://tools.ietf.org/html/rfc6902) | Compute an RFC 6902 stepwise patch (JSON Patch) from two documents |
| `apply`   | [RFC 6902](http://tools.ietf.org/html/rfc6902) | Apply an RFC 6902 stepwise patch to a document                     |

**`diff`** and **`merge`** are complementary — `diff` computes a merge patch, `merge` applies it.
**`patch`** and **`apply`** are complementary — `patch` computes a stepwise diff, `apply` applies it.

#### diff — compute RFC 7396 merge patch

```bash
# Compare original to result, write merge patch to file
./JsonT.java diff merge_orig.json merge_result.json merge_patch.json

# To stdout (default)
./JsonT.java diff merge_orig.json merge_result.json

# From stdin
cat merge_orig.json | ./JsonT.java diff - merge_result.json
```

#### merge — apply RFC 7396 merge patch

```bash
# Apply a merge patch, write result to file
./JsonT.java merge merge_patch.json merge_orig.json merge_result.json

# To stdout (default)
./JsonT.java merge merge_patch.json merge_orig.json

# From stdin
cat merge_patch.json | ./JsonT.java merge - merge_orig.json
```

#### patch — compute RFC 6902 stepwise patch

```bash
# Compute a JSON Patch (array of operations), write to file
./JsonT.java patch default.json default_1.json patch_diff.json

# To stdout (default)
./JsonT.java patch default.json default_1.json

# From stdin
cat default.json | ./JsonT.java patch - default_1.json
```

#### apply — apply RFC 6902 stepwise patch

```bash
# Apply a JSON Patch, write result to file
./JsonT.java apply patch_diff.json default.json default_1.json

# To stdout (default)
./JsonT.java apply patch_diff.json default.json

# From stdin
cat patch_diff.json | ./JsonT.java apply - default.json
```

#### Chaining operations

When output goes to STDOUT (the default), operations can be piped together. For example, compute a diff and immediately apply it:

```bash
./JsonT.java patch original.json modified.json | ./JsonT.java apply - original.json
```

Or round-trip a merge diff:

```bash
./JsonT.java diff original.json modified.json | ./JsonT.java merge - original.json
```

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
