#RFC7369 JSON Merge Patch
#http://tools.ietf.org/html/rfc7396
if [ -t 0 ]; then
    IN="src/test/resources/operations/merge/merge_orig.json"
    STDIN=0
else
    IN="-"
    STDIN=1
fi
#set -x
./JsonT.java diff $IN src/test/resources/operations/merge/merge_result.json - | tee target/merge_patch.json
if [ $STDIN -eq 1 ]; then
cat src/test/resources/operations/merge/merge_patch.json
fi
