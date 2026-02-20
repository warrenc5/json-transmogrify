// merge-objects.js
// Merges object-based GraalVM native-image config files (e.g. resource-config.json)
// Input (_): an array of objects, e.g. [ {resources:{includes:[...]}, bundles:[...]}, ... ]
// Output ($): a single object with all inner arrays concatenated and nested objects merged
//
// Used for: resource-config.json, serialization-config.json (object form)

function deepMerge(target, source) {
  for (const key of Object.keys(source)) {
    const val = source[key];
    if (Array.isArray(val)) {
      if (!Array.isArray(target[key])) {
        target[key] = [];
      }
      for (const item of val) {
        target[key].push(item);
      }
    } else if (typeof val === "object" && val !== null) {
      if (
        typeof target[key] !== "object" ||
        target[key] === null ||
        Array.isArray(target[key])
      ) {
        target[key] = {};
      }
      deepMerge(target[key], val);
    } else if (!(key in target)) {
      target[key] = val;
    }
  }
  return target;
}

const merged = {};
for (const obj of _) {
  if (obj !== null && typeof obj === "object" && !Array.isArray(obj)) {
    deepMerge(merged, obj);
  }
}
$ = merged;
