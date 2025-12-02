java -classpath src/test/resources:`find target -name \*.jar | tr -t '\n' ':'` JsonT src/test/resources/ALL_FINES.json template.json all_fines_nsw.csv UTF-8
ls -la all_fines_nsw.csv
sed -i s/\"//g all_fines_nsw.csv
head all_fines_nsw.csv

