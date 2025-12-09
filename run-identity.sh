java -classpath src/main/resources:src/test/resources:`find target -name \*.jar | tr -t '\n' ':'` JsonT src/test/resources/ALL_FINES.json 

