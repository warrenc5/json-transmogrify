#JSON Patch as defined by RFC 6902 http://tools.ietf.org/html/rfc6902
if [ -t 0 ]; then
    IN="src/test/resources/operations/patch/patch_diff.json"
    STDIN=0
else
    IN="-"
    STDIN=1
fi
#set -x
./JsonT.java apply $IN src/test/resources/operations/patch/default.json - | tee target/default_1.json
if [ $STDIN -eq 1 ]; then
cat src/test/resources/operations/patch/default_1.json
fi
