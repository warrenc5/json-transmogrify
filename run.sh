rm all_fines_nsw.csv
java -classpath src/test/resources:`find target -name \*.jar | tr -t '\n' ':'` JsonT template.js src/test/resources/ALL_FINES.json all_fines_nsw.csv UTF-8
ls -la all_fines_nsw.csv
sed -i s/\"//g all_fines_nsw.csv
head all_fines_nsw.csv

