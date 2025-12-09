which mdcat || cargo install mdcat
java -classpath src/test/resources:`find target -name \*.jar | tr -t '\n' ':'` JsonT 2>&1 | mdcat -
