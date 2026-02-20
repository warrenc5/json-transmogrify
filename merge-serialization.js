// merge-serialization.js
// Merges serialization-config.json files which may be either:
//   - A JSON array:  [ {name: "...", condition: {...}}, ... ]
//   - A JSON object: { types: [...], lambdaCapturingTypes: [...], proxies: [...] }
// Input (_): an array of these mixed items
// Output ($): a single object with all inner arrays concatenated

const types = []
const lambdaCapturingTypes = []
const proxies = []

for (const item of _) {
    if (Array.isArray(item)) {
        // Array form — entries are type registrations
        for (const entry of item) {
            types.push(entry)
        }
    } else if (item !== null && typeof item === 'object') {
        // Object form — merge each known array property
        if (Array.isArray(item.types)) {
            for (const t of item.types) {
                types.push(t)
            }
        }
        if (Array.isArray(item.lambdaCapturingTypes)) {
            for (const l of item.lambdaCapturingTypes) {
                lambdaCapturingTypes.push(l)
            }
        }
        if (Array.isArray(item.proxies)) {
            for (const p of item.proxies) {
                proxies.push(p)
            }
        }
    }
}

$ = {
    types: types,
    lambdaCapturingTypes: lambdaCapturingTypes,
    proxies: proxies
}
