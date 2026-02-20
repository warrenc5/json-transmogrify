// merge-arrays.js
// Merges array-based GraalVM native-image config files
// Input (_): an array of arrays, e.g. [ [...file1...], [...file2...], ... ]
// Output ($): a single flat array with all elements concatenated
//
// Used for: reflect-config.json, jni-config.json, proxy-config.json

const merged = []
for (const arr of _) {
    if (Array.isArray(arr)) {
        for (const item of arr) {
            merged.push(item)
        }
    }
}
$ = merged
