cat src/test/resources/ALL_FINES.json | java -classpath src/test/resources:`find target -name \*.jar | tr -t '\n' ':'` JsonT template.js | head 

