#RFC7369 JSON Merge Patch http://tools.ietf.org/html/rfc7396
if [ -t 0 ]; then
    IN="src/test/resources/operations/merge/merge_patch.json"
    STDIN=0
else
    IN="-"
    STDIN=1
fi
#set -x
./JsonT.java merge $IN src/test/resources/operations/merge/merge_orig.json - | tee target/merge_result.json
if [ $STDIN -eq 1 ]; then
cat src/test/resources/operations/merge/merge_result.json
fi
