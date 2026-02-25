if [ ! -x target/jsont ] ; then
    . .env
    native.sh
fi

target/jsont src/test/resources/template.js src/test/resources/ALL_FINES.json  | head
