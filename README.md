This is a tool based on graalson-trax, a Java based JSON transformer that uses Javascript templates to select output.



## Source data

```
https://www.nsw.gov.au/api/v1/elasticsearch/prod_demerit_points/_search?source_content_type=application%2Fjson&source=%7B%22from%22%3A0%2C%22size%22%3A700%2C%22query%22%3A%7B%22match_all%22%3A%7B%7D%7D%2C%22sort%22%3A%5B%7B%22_script%22%3A%7B%22type%22%3A%22number%22%2C%22script%22%3A%7B%22lang%22%3A%22painless%22%2C%22source%22%3A%22if+%28doc%5B%27Fine.keyword%27%5D.size%28%29+%3D%3D+0%29+%7B+return+0%3B+%7D+String+fine+%3D+doc%5B%27Fine.keyword%27%5D.value%3B+if+%28fine+%3D%3D+null+%7C%7C+fine+%3D%3D+%27N%2FA%27%29+%7B+return+0%3B+%7D+String+clean+%3D+fine.replace%28%27%24%27%2C+%27%27%29.replace%28%27%2C%27%2C+%27%27%29%3B+return+Double.parseDouble%28clean%29%3B%22%7D%2C%22order%22%3A%22asc%22%7D%7D%5D%2C%22fields%22%3A%5B%22recordid%22%2C%22offence%22%2C%22type%22%2C%22Class%22%2C%22Description%22%2C%22Law%22%2C%22Section%22%2C%22Fine%22%2C%22Fine+Level%22%2C%22Points%22%2C%22Double+demerits%22%2C%22%40delta%22%5D%7D
```

## Template 

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
